package com.cardnova.giftchat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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
public class TrtcUserSigService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long DEFAULT_EXPIRE_SECONDS = 7L * 24L * 60L * 60L;

    private final ObjectMapper objectMapper;
    private final int sdkAppId;
    private final String secretKey;
    private final long expireSeconds;

    public TrtcUserSigService(
        ObjectMapper objectMapper,
        @Value("${app.video.trtc-sdk-app-id:0}") int sdkAppId,
        @Value("${app.video.trtc-secret-key:}") String secretKey,
        @Value("${app.video.trtc-user-sig-expire-seconds:604800}") long expireSeconds
    ) {
        this.objectMapper = objectMapper;
        this.sdkAppId = sdkAppId;
        this.secretKey = secretKey;
        this.expireSeconds = expireSeconds > 0 ? expireSeconds : DEFAULT_EXPIRE_SECONDS;
    }

    public boolean isConfigured() {
        return sdkAppId > 0 && !secretKey.isBlank();
    }

    public String generate(String userId) {
        if (!isConfigured()) {
            return "";
        }

        try {
            long currentTime = Instant.now().getEpochSecond();
            String signature = hmacSha256(userId, currentTime);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("TLS.ver", "2.0");
            payload.put("TLS.identifier", userId);
            payload.put("TLS.sdkappid", sdkAppId);
            payload.put("TLS.expire", expireSeconds);
            payload.put("TLS.time", currentTime);
            payload.put("TLS.sig", signature);

            return base64UrlEncode(deflate(objectMapper.writeValueAsBytes(payload)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("TRTC UserSig payload serialization failed", exception);
        }
    }

    private String hmacSha256(String userId, long currentTime) {
        try {
            String contentToBeSigned = "TLS.identifier:" + userId + "\n"
                + "TLS.sdkappid:" + sdkAppId + "\n"
                + "TLS.time:" + currentTime + "\n"
                + "TLS.expire:" + expireSeconds + "\n";
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(contentToBeSigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("TRTC UserSig signing failed", exception);
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
