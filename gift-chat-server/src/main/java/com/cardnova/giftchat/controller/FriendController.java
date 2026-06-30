package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.CreateFriendRequest;
import com.cardnova.giftchat.dto.SearchFriendResponse;
import com.cardnova.giftchat.dto.SendDirectMessageRequest;
import com.cardnova.giftchat.model.ChatMessage;
import com.cardnova.giftchat.model.ChatMessageSync;
import com.cardnova.giftchat.model.FriendProfile;
import com.cardnova.giftchat.model.FriendRequestItem;
import com.cardnova.giftchat.service.PersistentFriendService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final PersistentFriendService persistentFriendService;

    public FriendController(PersistentFriendService persistentFriendService) {
        this.persistentFriendService = persistentFriendService;
    }

    @GetMapping
    public ApiResponse<List<FriendProfile>> getFriends() {
        return ApiResponse.success(persistentFriendService.getFriends());
    }

    @GetMapping("/search")
    public ApiResponse<List<SearchFriendResponse>> search(@RequestParam String username) {
        return ApiResponse.success(persistentFriendService.searchUsers(username));
    }

    @GetMapping("/requests")
    public ApiResponse<List<FriendRequestItem>> requests() {
        return ApiResponse.success(persistentFriendService.getFriendRequests());
    }

    @PostMapping("/requests")
    public ApiResponse<FriendRequestItem> createRequest(@Valid @RequestBody CreateFriendRequest request) {
        return ApiResponse.success("friend_request_created", persistentFriendService.createFriendRequest(request));
    }

    @PostMapping("/requests/{friendshipId}/accept")
    public ApiResponse<FriendRequestItem> acceptRequest(@PathVariable String friendshipId) {
        return ApiResponse.success("friend_request_accepted", persistentFriendService.acceptRequest(friendshipId));
    }

    @PostMapping("/requests/{friendshipId}/reject")
    public ApiResponse<FriendRequestItem> rejectRequest(@PathVariable String friendshipId) {
        return ApiResponse.success("friend_request_rejected", persistentFriendService.rejectRequest(friendshipId));
    }

    @DeleteMapping("/{friendshipId}")
    public ApiResponse<FriendProfile> removeFriend(@PathVariable String friendshipId) {
        return ApiResponse.success("friend_removed", persistentFriendService.removeFriend(friendshipId));
    }

    @PostMapping("/{friendshipId}/read")
    public ApiResponse<FriendProfile> markRead(@PathVariable String friendshipId) {
        return ApiResponse.success("friend_read", persistentFriendService.markConversationRead(friendshipId));
    }

    @GetMapping("/{friendshipId}/messages")
    public ApiResponse<List<ChatMessage>> messagesAfter(
        @PathVariable String friendshipId,
        @RequestParam(required = false, defaultValue = "") String afterId
    ) {
        return ApiResponse.success(persistentFriendService.getMessagesAfter(friendshipId, afterId));
    }

    @GetMapping("/{friendshipId}/messages/sync")
    public ApiResponse<ChatMessageSync> syncMessages(
        @PathVariable String friendshipId,
        @RequestParam(required = false, defaultValue = "0") long sinceSeq
    ) {
        return ApiResponse.success(persistentFriendService.syncMessages(friendshipId, sinceSeq));
    }

    @PostMapping("/{friendshipId}/messages")
    public ApiResponse<ChatMessage> sendMessage(
        @PathVariable String friendshipId,
        @Valid @RequestBody SendDirectMessageRequest request
    ) {
        return ApiResponse.success("direct_message_sent", persistentFriendService.sendMessage(friendshipId, request));
    }
}
