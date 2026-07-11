package com.cardnova.giftchat.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class GiftCardCatalog {

    public record CardDefinition(String code, String name, List<String> aliases) {
    }

    private static final List<CardDefinition> CARDS = List.of(
        card("APPLE_ITUNES", "Apple / iTunes", "Apple", "iTunes", "Apple(itunes)", "Apple iTunes"),
        card("STEAM", "Steam", "Steam Card", "Steam Gift Card"),
        card("RAZER_GOLD", "Razer Gold", "Razer", "RazerGold"),
        card("XBOX", "Xbox", "Xbox Card", "Xbox Gift Card"),
        card("EBAY", "eBay", "Ebay", "eBay Card"),
        card("SEPHORA", "Sephora", "Sephora Card"),
        card("GOOGLE_PLAY", "Google Play", "Google", "Google Card", "Google Play Card"),
        card("VANILLA", "Vanilla", "Vanilla Card", "Vanilla Gift Card"),
        card("AMERICAN_EXPRESS", "American Express", "Amex", "American Express Card"),
        card("ZELLE", "Zelle", "Zelle Card"),
        card("CHIME", "Chime", "Chime Card")
    );
    private static final Map<String, CardDefinition> BY_CODE = indexByCode();
    private static final Map<String, CardDefinition> BY_ALIAS = indexByAlias();

    private GiftCardCatalog() {
    }

    public static List<CardDefinition> cards() {
        return CARDS;
    }

    public static Optional<CardDefinition> findByCode(String value) {
        return Optional.ofNullable(BY_CODE.get(normalizeCode(value)));
    }

    public static Optional<CardDefinition> findByName(String value) {
        return Optional.ofNullable(BY_ALIAS.get(normalizeAlias(value)));
    }

    private static CardDefinition card(String code, String name, String... aliases) {
        return new CardDefinition(code, name, List.of(aliases));
    }

    private static Map<String, CardDefinition> indexByCode() {
        Map<String, CardDefinition> result = new LinkedHashMap<>();
        CARDS.forEach(card -> result.put(card.code(), card));
        return Map.copyOf(result);
    }

    private static Map<String, CardDefinition> indexByAlias() {
        Map<String, CardDefinition> result = new LinkedHashMap<>();
        for (CardDefinition card : CARDS) {
            putAlias(result, card.name(), card);
            putAlias(result, card.code(), card);
            card.aliases().forEach(alias -> putAlias(result, alias, card));
        }
        return Map.copyOf(result);
    }

    private static void putAlias(Map<String, CardDefinition> result, String alias, CardDefinition card) {
        CardDefinition existing = result.putIfAbsent(normalizeAlias(alias), card);
        if (existing != null && !existing.code().equals(card.code())) {
            throw new IllegalStateException("Duplicate gift card alias: " + alias);
        }
    }

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeAlias(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
