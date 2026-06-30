package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.SendSupportMessageRequest;
import com.cardnova.giftchat.dto.UpdateSupportConversationNoteRequest;
import com.cardnova.giftchat.model.ChatMessage;
import com.cardnova.giftchat.model.ChatMessageSync;
import com.cardnova.giftchat.model.SupportConversation;
import com.cardnova.giftchat.model.SupportCustomerProfile;
import com.cardnova.giftchat.model.SupportLedgerReport;
import com.cardnova.giftchat.service.PersistentSupportService;
import com.cardnova.giftchat.service.SupportCustomerProfileService;
import com.cardnova.giftchat.service.SupportLedgerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final PersistentSupportService persistentSupportService;
    private final SupportCustomerProfileService supportCustomerProfileService;
    private final SupportLedgerService supportLedgerService;

    public SupportController(
        PersistentSupportService persistentSupportService,
        SupportCustomerProfileService supportCustomerProfileService,
        SupportLedgerService supportLedgerService
    ) {
        this.persistentSupportService = persistentSupportService;
        this.supportCustomerProfileService = supportCustomerProfileService;
        this.supportLedgerService = supportLedgerService;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<SupportConversation>> conversations() {
        return ApiResponse.success(persistentSupportService.getConversations());
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ApiResponse<SupportConversation> markRead(@PathVariable String conversationId) {
        return ApiResponse.success("support_conversation_read", persistentSupportService.markConversationRead(conversationId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<ChatMessage>> messagesAfter(
        @PathVariable String conversationId,
        @RequestParam(required = false, defaultValue = "") String afterId
    ) {
        return ApiResponse.success(persistentSupportService.getMessagesAfter(conversationId, afterId));
    }

    @GetMapping("/conversations/{conversationId}/messages/sync")
    public ApiResponse<ChatMessageSync> syncMessages(
        @PathVariable String conversationId,
        @RequestParam(required = false, defaultValue = "0") long sinceSeq
    ) {
        return ApiResponse.success(persistentSupportService.syncMessages(conversationId, sinceSeq));
    }

    @PostMapping("/conversations/{conversationId}/note")
    public ApiResponse<SupportConversation> updateNote(
        @PathVariable String conversationId,
        @RequestBody UpdateSupportConversationNoteRequest request
    ) {
        return ApiResponse.success("support_conversation_note_updated", persistentSupportService.updateConversationNote(conversationId, request.note()));
    }

    @GetMapping("/conversations/{conversationId}/customer-profile")
    public ApiResponse<SupportCustomerProfile> customerProfile(@PathVariable String conversationId) {
        return ApiResponse.success(supportCustomerProfileService.getProfile(conversationId));
    }

    @GetMapping("/ledger")
    public ApiResponse<SupportLedgerReport> ledger() {
        return ApiResponse.success(supportLedgerService.report());
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResponse<ChatMessage> sendMessage(
        @PathVariable String conversationId,
        @Valid @RequestBody SendSupportMessageRequest request
    ) {
        return ApiResponse.success("support_message_sent", persistentSupportService.sendMessage(conversationId, request));
    }
}
