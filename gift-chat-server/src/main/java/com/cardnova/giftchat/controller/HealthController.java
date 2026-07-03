package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
            "status", "ok",
            "time", Instant.now().toString()
        ));
    }
}
