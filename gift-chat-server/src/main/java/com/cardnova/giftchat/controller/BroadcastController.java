package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.CreateBroadcastRequest;
import com.cardnova.giftchat.model.BroadcastItem;
import com.cardnova.giftchat.service.BroadcastService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/broadcasts")
public class BroadcastController {

    private final BroadcastService broadcastService;

    public BroadcastController(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @GetMapping
    public ApiResponse<List<BroadcastItem>> broadcasts() {
        return ApiResponse.success(broadcastService.getBroadcasts());
    }

    @PostMapping
    public ApiResponse<BroadcastItem> create(@Valid @RequestBody CreateBroadcastRequest request) {
        return ApiResponse.success("broadcast_sent", broadcastService.create(request));
    }
}
