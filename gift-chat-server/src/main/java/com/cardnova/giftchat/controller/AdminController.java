package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.AssignSupportConversationRequest;
import com.cardnova.giftchat.dto.CreateAgentRequest;
import com.cardnova.giftchat.dto.UpdateUserStatusRequest;
import com.cardnova.giftchat.model.AdminDirectConversation;
import com.cardnova.giftchat.model.AdminUserItem;
import com.cardnova.giftchat.model.AgentItem;
import com.cardnova.giftchat.model.SupportConversation;
import com.cardnova.giftchat.service.AdminService;
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
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserItem>> users() {
        return ApiResponse.success(adminService.users());
    }

    @GetMapping("/agents")
    public ApiResponse<List<AgentItem>> agents() {
        return ApiResponse.success(adminService.agents());
    }

    @PostMapping("/agents")
    public ApiResponse<AgentItem> createAgent(@Valid @RequestBody CreateAgentRequest request) {
        return ApiResponse.success("agent_created", adminService.createAgent(request));
    }

    @PostMapping("/agents/{agentId}/status")
    public ApiResponse<AgentItem> updateAgentStatus(
        @PathVariable String agentId,
        @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ApiResponse.success("agent_status_updated", adminService.updateAgentStatus(agentId, request.status()));
    }

    @GetMapping("/support/conversations")
    public ApiResponse<List<SupportConversation>> supportConversations() {
        return ApiResponse.success(adminService.supportConversations());
    }

    @GetMapping("/direct/conversations")
    public ApiResponse<List<AdminDirectConversation>> directConversations(@RequestParam(required = false) String username) {
        return ApiResponse.success(adminService.directConversations(username));
    }

    @PostMapping("/support/conversations/{conversationId}/assign")
    public ApiResponse<SupportConversation> assignConversation(
        @PathVariable String conversationId,
        @Valid @RequestBody AssignSupportConversationRequest request
    ) {
        return ApiResponse.success("support_conversation_assigned", adminService.assignConversation(conversationId, request.agentUsername()));
    }
}
