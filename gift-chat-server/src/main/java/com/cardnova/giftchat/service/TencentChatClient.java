package com.cardnova.giftchat.service;

import com.cardnova.giftchat.config.TencentChatProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TencentChatClient {

    private final TencentChatProperties properties;
    private final TencentUserSigService userSigService;
    private final RestClient restClient;

    public TencentChatClient(
        TencentChatProperties properties,
        TencentUserSigService userSigService,
        RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.userSigService = userSigService;
        this.restClient = restClientBuilder.baseUrl(properties.restBaseUrl()).build();
    }

    public boolean configured() {
        return properties.configured();
    }

    public TencentChatResult importAccount(String tencentUserId, String nickname) {
        if (!configured()) {
            return TencentChatResult.skipped("Tencent Chat is disabled or not configured");
        }
        Map<String, Object> body = Map.of(
            "UserID", tencentUserId,
            "Nick", nickname == null || nickname.isBlank() ? tencentUserId : nickname
        );
        return post("/v4/im_open_login_svc/account_import", body);
    }

    public TencentChatResult sendTextMessage(String fromUserId, String toUserId, String content) {
        if (!configured()) {
            return TencentChatResult.skipped("Tencent Chat is disabled or not configured");
        }
        Map<String, Object> body = Map.of(
            "SyncOtherMachine", 2,
            "From_Account", fromUserId,
            "To_Account", toUserId,
            "MsgRandom", ThreadLocalRandom.current().nextInt(100000, Integer.MAX_VALUE),
            "MsgBody", List.of(Map.of(
                "MsgType", "TIMTextElem",
                "MsgContent", Map.of("Text", content)
            ))
        );
        return post("/v4/openim/sendmsg", body);
    }

    private TencentChatResult post(String path, Map<String, Object> body) {
        try {
            Map<?, ?> response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path(path)
                    .queryParam("sdkappid", properties.sdkAppId())
                    .queryParam("identifier", properties.adminIdentifier())
                    .queryParam("usersig", userSigService.adminUserSig())
                    .queryParam("random", ThreadLocalRandom.current().nextInt(100000, Integer.MAX_VALUE))
                    .queryParam("contenttype", "json")
                    .build())
                .body(body)
                .retrieve()
                .body(Map.class);
            int errorCode = response == null || response.get("ErrorCode") == null ? -1 : ((Number) response.get("ErrorCode")).intValue();
            String errorInfo = response == null || response.get("ErrorInfo") == null ? "" : String.valueOf(response.get("ErrorInfo"));
            String messageKey = response == null || response.get("MsgKey") == null ? "" : String.valueOf(response.get("MsgKey"));
            return errorCode == 0
                ? TencentChatResult.success(messageKey)
                : TencentChatResult.failed("Tencent ErrorCode " + errorCode + ": " + errorInfo);
        } catch (RuntimeException exception) {
            return TencentChatResult.failed(exception.getMessage());
        }
    }

    public record TencentChatResult(boolean attempted, boolean success, String messageKey, String error) {
        public static TencentChatResult skipped(String reason) {
            return new TencentChatResult(false, false, "", reason);
        }

        public static TencentChatResult success(String messageKey) {
            return new TencentChatResult(true, true, messageKey == null ? "" : messageKey, "");
        }

        public static TencentChatResult failed(String error) {
            return new TencentChatResult(true, false, "", error == null ? "Tencent Chat request failed" : error);
        }
    }
}
