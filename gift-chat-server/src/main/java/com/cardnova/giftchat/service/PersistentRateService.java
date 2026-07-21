package com.cardnova.giftchat.service;

import com.cardnova.giftchat.api.ConflictException;
import com.cardnova.giftchat.dto.CreateRateRequest;
import com.cardnova.giftchat.entity.GiftCardRateEntity;
import com.cardnova.giftchat.entity.GiftCardRateQuoteEntity;
import com.cardnova.giftchat.entity.GiftCardArtworkEntity;
import com.cardnova.giftchat.entity.UploadAssetEntity;
import com.cardnova.giftchat.model.RateItem;
import com.cardnova.giftchat.repository.GiftCardArtworkRepository;
import com.cardnova.giftchat.repository.GiftCardRateRepository;
import com.cardnova.giftchat.repository.GiftCardRateQuoteRepository;
import com.cardnova.giftchat.repository.UploadAssetRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PersistentRateService {

    private static final DateTimeFormatter RATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ALLOWED_STATUSES = Set.of("active", "paused");
    private static final List<String> SUPPORTED_FACE_CURRENCIES = List.of("USD", "EUR", "GBP", "AUD");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Map<String, String> REGION_ALIASES = Map.ofEntries(
        Map.entry("NG", "NG"),
        Map.entry("NIGERIA", "NG"),
        Map.entry("234", "NG"),
        Map.entry("\u5c3c\u65e5\u5229\u4e9a", "NG"),
        Map.entry("IN", "IN"),
        Map.entry("INDIA", "IN"),
        Map.entry("91", "IN"),
        Map.entry("\u5370\u5ea6", "IN"),
        Map.entry("CM", "CM"),
        Map.entry("CAMEROON", "CM"),
        Map.entry("237", "CM"),
        Map.entry("\u5580\u9ea6\u9686", "CM"),
        Map.entry("GH", "GH"),
        Map.entry("GHANA", "GH"),
        Map.entry("233", "GH"),
        Map.entry("\u52a0\u7eb3", "GH"),
        Map.entry("KE", "KE"),
        Map.entry("KENYA", "KE"),
        Map.entry("254", "KE"),
        Map.entry("US", "US"),
        Map.entry("USA", "US"),
        Map.entry("UNITEDSTATES", "US"),
        Map.entry("1", "US")
    );

    private final GiftCardRateRepository giftCardRateRepository;
    private final GiftCardRateQuoteRepository quoteRepository;
    private final GiftCardArtworkRepository artworkRepository;
    private final UploadAssetRepository uploadAssetRepository;
    private final CurrentUserService currentUserService;
    private final CountryCodeService countryCodeService;

    public PersistentRateService(
        GiftCardRateRepository giftCardRateRepository,
        GiftCardRateQuoteRepository quoteRepository,
        GiftCardArtworkRepository artworkRepository,
        UploadAssetRepository uploadAssetRepository,
        CurrentUserService currentUserService,
        CountryCodeService countryCodeService
    ) {
        this.giftCardRateRepository = giftCardRateRepository;
        this.quoteRepository = quoteRepository;
        this.artworkRepository = artworkRepository;
        this.uploadAssetRepository = uploadAssetRepository;
        this.currentUserService = currentUserService;
        this.countryCodeService = countryCodeService;
    }

    public List<RateItem> findAll() {
        var currentUser = currentUserService.getCurrentUser();
        return giftCardRateRepository.findAllByOrderByUpdatedAtDesc().stream()
            .filter(entity -> !"USER".equalsIgnoreCase(currentUser.getRoleCode())
                || currentUser.getCountryCode() == null
                || currentUser.getCountryCode().equalsIgnoreCase(entity.getRegionCode()))
            .map(this::toRateItem)
            .toList();
    }

    public GiftCardRateEntity requireActiveRate(String cardName, String countryCode) {
        String normalizedCardName = GiftCardCatalog.findByName(cardName)
            .map(GiftCardCatalog.CardDefinition::name)
            .orElseGet(() -> value(cardName).trim());
        GiftCardRateEntity entity = giftCardRateRepository
            .findFirstByCardNameIgnoreCaseAndRegionCodeIgnoreCaseAndStatusCodeIgnoreCase(
                normalizedCardName,
                countryCodeService.requireCountry(countryCode).code(),
                "ACTIVE"
            )
            .orElseThrow(() -> new IllegalArgumentException("Active gift card payout rate is not configured"));
        if (entity.getLocalPayoutPerUsd() == null) {
            entity.setLocalPayoutPerUsd(parseLegacyRate(entity.getRateValue()).setScale(6, RoundingMode.HALF_UP));
        }
        return entity;
    }

    public RateQuote requireActiveQuote(String cardName, String countryCode, String faceCurrencyCode) {
        GiftCardRateEntity rate = requireActiveRate(cardName, countryCode);
        String currency = normalizeFaceCurrency(faceCurrencyCode);
        GiftCardRateQuoteEntity quote = quoteRepository
            .findByRate_IdAndFaceCurrencyCodeIgnoreCase(rate.getId(), currency)
            .orElse(null);
        if (quote != null) {
            return new RateQuote(rate, currency, quote.getLocalPayoutPerUnit());
        }
        if ("USD".equals(currency)) {
            BigDecimal legacy = rate.getLocalPayoutPerUsd() == null ? parseLegacyRate(rate.getRateValue()) : rate.getLocalPayoutPerUsd();
            return new RateQuote(rate, currency, legacy.setScale(6, RoundingMode.HALF_UP));
        }
        throw new IllegalArgumentException(currency + " payout rate is not configured for this gift card");
    }

    @Transactional
    public RateItem create(CreateRateRequest request) {
        var currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        GiftCardRateEntity entity = new GiftCardRateEntity();
        entity.setId(UUID.randomUUID().toString());
        applyCardIdentity(entity, request);
        applyPricing(entity, request);
        giftCardRateRepository.findByRegionCodeIgnoreCaseAndIdentityKey(entity.getRegionCode(), entity.getIdentityKey())
            .ifPresent(existing -> {
                throw new ConflictException("A rate already exists for this card and country");
            });
        entity.setStatusCode("ACTIVE");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUser);
        GiftCardRateEntity saved = saveWithUniqueIdentityGuard(entity);
        replaceQuotes(saved, normalizeQuotes(request));
        updateArtwork(saved.getIdentityKey(), request.imageUrl(), currentUser);
        return toRateItem(saved);
    }

    @Transactional
    public RateItem updateStatus(String rateId, String status) {
        var currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        String normalizedStatus = status == null ? "" : status.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported rate status");
        }

        GiftCardRateEntity entity = giftCardRateRepository.findById(rateId)
            .orElseThrow(() -> new IllegalArgumentException("Rate not found"));
        entity.setStatusCode(normalizedStatus.toUpperCase());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUser);
        return toRateItem(giftCardRateRepository.save(entity));
    }

    @Transactional
    public RateItem update(String rateId, CreateRateRequest request) {
        var currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        GiftCardRateEntity entity = giftCardRateRepository.findById(rateId)
            .orElseThrow(() -> new IllegalArgumentException("Rate not found"));
        GiftCardRateEntity proposed = new GiftCardRateEntity();
        applyCardIdentity(proposed, request);
        applyPricing(proposed, request);
        giftCardRateRepository.findByRegionCodeIgnoreCaseAndIdentityKey(proposed.getRegionCode(), proposed.getIdentityKey())
            .filter(existing -> !existing.getId().equals(entity.getId()))
            .ifPresent(existing -> {
                throw new ConflictException("A rate already exists for this card and country");
            });
        copyRateValues(proposed, entity);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUser);
        GiftCardRateEntity saved = saveWithUniqueIdentityGuard(entity);
        replaceQuotes(saved, normalizeQuotes(request));
        updateArtwork(saved.getIdentityKey(), request.imageUrl(), currentUser);
        return toRateItem(saved);
    }

    @Transactional
    public RateItem delete(String rateId) {
        var currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        GiftCardRateEntity entity = giftCardRateRepository.findById(rateId)
            .orElseThrow(() -> new IllegalArgumentException("Rate not found"));
        RateItem deleted = toRateItem(entity);
        quoteRepository.deleteByRate_Id(entity.getId());
        giftCardRateRepository.delete(entity);
        return deleted;
    }

    private RateItem toRateItem(GiftCardRateEntity entity) {
        var country = countryCodeService.requireCountry(entity.getRegionCode());
        Map<String, String> quotes = quoteMap(entity);
        BigDecimal amount = new BigDecimal(quotes.get("USD"));
        String displayRate = displayRate(country.currencySymbol(), amount);
        return new RateItem(
            entity.getId(),
            entity.getCardName(),
            entity.getCardCode(),
            entity.getRegionCode(),
            country.currencyCode(),
            decimal(amount),
            quotes,
            artworkRepository.findById(entity.getIdentityKey()).map(GiftCardArtworkEntity::getImageUrl).orElse(""),
            displayRate,
            entity.getStatusCode().equalsIgnoreCase("ACTIVE") ? "active" : "paused",
            RATE_TIME_FORMATTER.format(entity.getUpdatedAt())
        );
    }

    private GiftCardRateEntity saveWithUniqueIdentityGuard(GiftCardRateEntity entity) {
        try {
            return giftCardRateRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A rate already exists for this card and country");
        }
    }

    private void copyRateValues(GiftCardRateEntity source, GiftCardRateEntity target) {
        target.setCardName(source.getCardName());
        target.setCardCode(source.getCardCode());
        target.setIdentityKey(source.getIdentityKey());
        target.setRegionCode(source.getRegionCode());
        target.setCurrencyCode(source.getCurrencyCode());
        target.setLocalPayoutPerUsd(source.getLocalPayoutPerUsd());
        target.setRateValue(source.getRateValue());
    }

    private void applyPricing(GiftCardRateEntity entity, CreateRateRequest request) {
        String countryCode = normalizeRegionCode(request.region());
        var country = countryCodeService.requireCountry(countryCode);
        BigDecimal amount = normalizeQuotes(request).get("USD");
        entity.setRegionCode(country.code());
        entity.setCurrencyCode(country.currencyCode());
        entity.setLocalPayoutPerUsd(amount);
        entity.setRateValue(displayRate(country.currencySymbol(), amount));
    }

    private Map<String, BigDecimal> normalizeQuotes(CreateRateRequest request) {
        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        if (request.quotes() != null && !request.quotes().isEmpty()) {
            request.quotes().forEach((currency, amount) -> {
                String code = normalizeFaceCurrency(currency);
                if (amount == null || amount.signum() <= 0) {
                    throw new IllegalArgumentException(code + " payout rate must be greater than zero");
                }
                normalized.put(code, amount.setScale(6, RoundingMode.HALF_UP));
            });
        } else {
            BigDecimal usd = request.localPayoutPerUsd() != null
                ? request.localPayoutPerUsd()
                : parseLegacyRate(request.rate());
            normalized.put("USD", usd.setScale(6, RoundingMode.HALF_UP));
        }
        if (!normalized.containsKey("USD")) {
            throw new IllegalArgumentException("USD payout rate is required for the home page");
        }
        return normalized;
    }

    private void replaceQuotes(GiftCardRateEntity rate, Map<String, BigDecimal> quotes) {
        Map<String, GiftCardRateQuoteEntity> existing = quoteRepository.findByRate_IdOrderByFaceCurrencyCodeAsc(rate.getId()).stream()
            .collect(java.util.stream.Collectors.toMap(GiftCardRateQuoteEntity::getFaceCurrencyCode, item -> item));
        LocalDateTime now = LocalDateTime.now();
        List<GiftCardRateQuoteEntity> saved = new ArrayList<>();
        quotes.forEach((currency, amount) -> {
            GiftCardRateQuoteEntity quote = existing.remove(currency);
            if (quote == null) {
                quote = new GiftCardRateQuoteEntity();
                quote.setId(UUID.randomUUID().toString());
                quote.setRate(rate);
                quote.setFaceCurrencyCode(currency);
                quote.setCreatedAt(now);
            }
            quote.setLocalPayoutPerUnit(amount);
            quote.setUpdatedAt(now);
            saved.add(quote);
        });
        if (!existing.isEmpty()) quoteRepository.deleteAll(existing.values());
        quoteRepository.saveAll(saved);
    }

    private Map<String, String> quoteMap(GiftCardRateEntity rate) {
        Map<String, BigDecimal> stored = quoteRepository.findByRate_IdOrderByFaceCurrencyCodeAsc(rate.getId()).stream()
            .collect(java.util.stream.Collectors.toMap(GiftCardRateQuoteEntity::getFaceCurrencyCode, GiftCardRateQuoteEntity::getLocalPayoutPerUnit));
        if (!stored.containsKey("USD")) {
            BigDecimal legacy = rate.getLocalPayoutPerUsd() == null ? parseLegacyRate(rate.getRateValue()) : rate.getLocalPayoutPerUsd();
            stored.put("USD", legacy);
        }
        Map<String, String> ordered = new LinkedHashMap<>();
        SUPPORTED_FACE_CURRENCIES.forEach(currency -> {
            BigDecimal amount = stored.get(currency);
            if (amount != null && amount.signum() > 0) ordered.put(currency, decimal(amount));
        });
        return ordered;
    }

    private void updateArtwork(String identityKey, String requestedUrl, com.cardnova.giftchat.entity.UserEntity admin) {
        if (requestedUrl == null) return;
        String imageUrl = requestedUrl.trim();
        if (imageUrl.isEmpty()) {
            artworkRepository.deleteById(identityKey);
            return;
        }
        UploadAssetEntity asset = uploadAssetRepository.findByPublicUrl(imageUrl)
            .orElseThrow(() -> new IllegalArgumentException("Card image upload was not found"));
        if (!asset.getOwnerUser().getId().equals(admin.getId()) || asset.getMimeType() == null || !asset.getMimeType().startsWith("image/")) {
            throw new IllegalArgumentException("Card image must be an image uploaded by the current administrator");
        }
        LocalDateTime now = LocalDateTime.now();
        GiftCardArtworkEntity artwork = artworkRepository.findById(identityKey).orElseGet(() -> {
            GiftCardArtworkEntity created = new GiftCardArtworkEntity();
            created.setIdentityKey(identityKey);
            created.setCreatedAt(now);
            return created;
        });
        artwork.setImageUrl(asset.getPublicUrl());
        artwork.setUpdatedBy(admin);
        artwork.setUpdatedAt(now);
        artworkRepository.save(artwork);
    }

    private String normalizeFaceCurrency(String value) {
        String currency = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
        if (!SUPPORTED_FACE_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException("Unsupported gift card face currency");
        }
        return currency;
    }

    private BigDecimal parseLegacyRate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Gift card payout rate is required");
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value.replace(",", ""));
        List<BigDecimal> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(new BigDecimal(matcher.group(1)));
        }
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Gift card payout rate is invalid");
        }
        BigDecimal amount = numbers.get(numbers.size() - 1);
        if (numbers.size() > 1 && amount.compareTo(BigDecimal.ONE) == 0) {
            amount = numbers.get(numbers.size() - 2);
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Gift card payout rate is invalid");
        }
        return amount;
    }

    private String displayRate(String symbol, BigDecimal amount) {
        String separator = symbol.length() > 1 ? " " : "";
        return "$1 ≈ " + symbol + separator + decimal(amount);
    }

    private String decimal(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private void applyCardIdentity(GiftCardRateEntity entity, CreateRateRequest request) {
        String requestedCode = value(request.cardCode()).trim();
        if (!requestedCode.isEmpty()) {
            GiftCardCatalog.CardDefinition card = GiftCardCatalog.findByCode(requestedCode)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported gift card code"));
            entity.setCardCode(card.code());
            entity.setCardName(card.name());
            entity.setIdentityKey("CODE:" + card.code());
            return;
        }

        String requestedName = value(request.cardName()).trim();
        validateCustomName(requestedName);
        GiftCardCatalog.findByName(requestedName).ifPresentOrElse(card -> {
            entity.setCardCode(card.code());
            entity.setCardName(card.name());
            entity.setIdentityKey("CODE:" + card.code());
        }, () -> {
            entity.setCardCode(null);
            entity.setCardName(requestedName);
            entity.setIdentityKey("NAME:" + requestedName.toLowerCase(Locale.ROOT));
        });
    }

    private void validateCustomName(String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Card name is required");
        }
        if (name.length() > 128 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Card name is invalid");
        }
    }

    private String normalizeRegionCode(String region) {
        String trimmed = region == null ? "" : region.trim();
        String normalized = trimmed
            .replace("+", "")
            .replace("\uff0b", "")
            .replaceAll("[\\s_-]+", "")
            .toUpperCase(Locale.ROOT);
        return REGION_ALIASES.getOrDefault(normalized, trimmed.toUpperCase(Locale.ROOT));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    public record RateQuote(GiftCardRateEntity rate, String faceCurrencyCode, BigDecimal localPayoutPerUnit) {}
}
