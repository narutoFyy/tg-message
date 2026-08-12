package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.DeleteWebPushSubscriptionRequest;
import com.cardnova.giftchat.dto.RegisterWebPushSubscriptionRequest;
import com.cardnova.giftchat.model.WebPushConfiguration;
import com.cardnova.giftchat.model.WebPushSubscriptionItem;
import com.cardnova.giftchat.service.WebPushSubscriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/push/web")
public class WebPushController {

    private final WebPushSubscriptionService service;

    public WebPushController(WebPushSubscriptionService service) {
        this.service = service;
    }

    @GetMapping("/config")
    public ApiResponse<WebPushConfiguration> configuration() {
        return ApiResponse.success(service.configuration());
    }

    @PostMapping("/subscriptions")
    public ApiResponse<WebPushSubscriptionItem> register(
        @Valid @RequestBody RegisterWebPushSubscriptionRequest request
    ) {
        return ApiResponse.success("web_push_subscription_registered", service.register(request));
    }

    @DeleteMapping("/subscriptions")
    public ApiResponse<Map<String, Boolean>> disable(
        @Valid @RequestBody DeleteWebPushSubscriptionRequest request
    ) {
        return ApiResponse.success(Map.of("disabled", service.disable(request.endpoint())));
    }
}
