package com.cardnova.giftchat.service;

import com.cardnova.giftchat.config.TencentChatProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Deflater;

@Service
public class TencentUserSigService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final TencentChatProperties properties;

    public TencentUserSigService(ObjectMapper objectMapper, TencentChatProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String adminUserSig() {
        if (properties.adminUserSig() != null && !properties.adminUserSig().isBlank()) {
            return properties.adminUserSig().trim();
        }
        return generate(properties.adminIdentifier());
    }

    public String generate(String userId) {
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            return "";
        }

        try {
            long currentTime = Instant.now().getEpochSecond();
            String signature = hmacSha256(userId, currentTime);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("TLS.ver", "2.0");
            payload.put("TLS.identifier", userId);
            payload.put("TLS.sdkappid", Long.parseLong(properties.sdkAppId()));
            payload.put("TLS.expire", properties.userSigExpireSeconds());
            payload.put("TLS.time", currentTime);
            payload.put("TLS.sig", signature);
            return base64UrlEncode(deflate(objectMapper.writeValueAsBytes(payload)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Tencent UserSig payload serialization failed", exception);
        }
    }

    private String hmacSha256(String userId, long currentTime) {
        try {
            String contentToBeSigned = "TLS.identifier:" + userId + "\n"
                + "TLS.sdkappid:" + properties.sdkAppId() + "\n"
                + "TLS.time:" + currentTime + "\n"
                + "TLS.expire:" + properties.userSigExpireSeconds() + "\n";
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.secretKey().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(contentToBeSigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Tencent UserSig signing failed", exception);
        }
    }

    private byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        byte[] buffer = new byte[512];
        byte[] output = new byte[0];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            byte[] next = new byte[output.length + count];
            System.arraycopy(output, 0, next, 0, output.length);
            System.arraycopy(buffer, 0, next, output.length, count);
            output = next;
        }
        deflater.end();
        return output;
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getEncoder().encodeToString(data)
            .replace('+', '*')
            .replace('/', '-')
            .replace('=', '_');
    }
}
