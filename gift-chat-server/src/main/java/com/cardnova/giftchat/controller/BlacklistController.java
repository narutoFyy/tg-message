package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.CreateBlacklistRequest;
import com.cardnova.giftchat.model.FriendProfile;
import com.cardnova.giftchat.service.PersistentBlacklistService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/blacklist")
public class BlacklistController {

    private final PersistentBlacklistService persistentBlacklistService;

    public BlacklistController(PersistentBlacklistService persistentBlacklistService) {
        this.persistentBlacklistService = persistentBlacklistService;
    }

    @GetMapping
    public ApiResponse<List<FriendProfile>> getBlacklist() {
        return ApiResponse.success(persistentBlacklistService.getBlacklist());
    }

    @PostMapping
    public ApiResponse<FriendProfile> addBlacklist(@Valid @RequestBody CreateBlacklistRequest request) {
        return ApiResponse.success("blacklist_entry_created", persistentBlacklistService.addToBlacklist(request));
    }

    @DeleteMapping("/{blacklistId}")
    public ApiResponse<FriendProfile> removeBlacklist(@PathVariable String blacklistId) {
        return ApiResponse.success("blacklist_entry_removed", persistentBlacklistService.removeFromBlacklist(blacklistId));
    }
}
