package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.TranslationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TranslationService {

    private static final int MAX_TEXT_LENGTH = 600;

    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TranslationService(CurrentUserService currentUserService, ObjectMapper objectMapper) {
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    }

    public TranslationResult translateToChinese(String text) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(currentUser);

        String normalized = normalizeText(text);
        if (looksChinese(normalized)) {
            return new TranslationResult(normalized, normalized, "original");
        }

        String online = translateOnline(normalized);
        if (StringUtils.hasText(online)) {
            return new TranslationResult(normalized, online, "mymemory");
        }

        return new TranslationResult(normalized, translateOffline(normalized), "offline");
    }

    private String normalizeText(String text) {
        String normalized = text == null ? "" : text.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Text is required");
        }
        return normalized.length() > MAX_TEXT_LENGTH ? normalized.substring(0, MAX_TEXT_LENGTH) : normalized;
    }

    private boolean looksChinese(String value) {
        return value.codePoints().anyMatch(codePoint -> codePoint >= 0x4E00 && codePoint <= 0x9FFF);
    }

    private String translateOnline(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            URI uri = URI.create("https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=en%7Czh-CN");
            HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                return "";
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("responseStatus").asInt(200) >= 400) {
                return "";
            }

            String translated = repairMojibake(root.path("responseData").path("translatedText").asText("").trim());
            String upper = translated.toUpperCase();
            if (upper.contains("INVALID") || upper.contains("LANGPAIR") || looksMojibake(translated) || !looksChinese(translated)) {
                return "";
            }
            return translated;
        } catch (IOException | InterruptedException | RuntimeException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "";
        }
    }

    private String repairMojibake(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (!looksMojibake(value)) {
            return value;
        }
        try {
            String repaired = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            return StringUtils.hasText(repaired) ? repaired : value;
        } catch (RuntimeException exception) {
            return value;
        }
    }

    private boolean looksMojibake(String value) {
        return value.codePoints().anyMatch(codePoint ->
            codePoint == 0x00C3
                || codePoint == 0x00E4
                || codePoint == 0x00E5
                || codePoint == 0x00E7
                || codePoint == 0x00EF
                || (codePoint >= 0x0080 && codePoint <= 0x009F)
        );
    }

    private String translateOffline(String text) {
        String lower = text.toLowerCase();
        Map<String, String> phrases = new LinkedHashMap<>();
        phrases.put("hello", "你好");
        phrases.put("hi", "你好");
        phrases.put("bro", "兄弟");
        phrases.put("rate", "汇率");
        phrases.put("current rate", "当前汇率");
        phrases.put("question", "问题");
        phrases.put("received", "收到");
        phrases.put("money", "钱");
        phrases.put("payout", "付款");
        phrases.put("gift card", "礼品卡");
        phrases.put("razer gold", "Razer Gold");
        phrases.put("apple", "Apple");
        phrases.put("steam", "Steam");
        phrases.put("xbox", "Xbox");
        phrases.put("please", "请");
        phrases.put("thanks", "谢谢");
        phrases.put("thank you", "谢谢");
        phrases.put("yes", "是的");
        phrases.put("no", "不是");
        phrases.put("sure", "好的");

        String translated = lower;
        for (Map.Entry<String, String> entry : phrases.entrySet()) {
            translated = translated.replace(entry.getKey(), entry.getValue());
        }
        return translated.equals(lower) ? "暂无法离线翻译：" + text : translated;
    }
}
