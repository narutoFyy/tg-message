package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.AssignSupportConversationRequest;
import com.cardnova.giftchat.dto.CreateAgentRequest;
import com.cardnova.giftchat.dto.ResetLotteryEligibilityRequest;
import com.cardnova.giftchat.dto.UpdateLotteryRecordStatusRequest;
import com.cardnova.giftchat.dto.UpdateAgentWelcomeMessageRequest;
import com.cardnova.giftchat.dto.UpdateRegistrationBonusConfigRequest;
import com.cardnova.giftchat.dto.UpdateReferralRewardConfigRequest;
import com.cardnova.giftchat.dto.UpdateUserStatusRequest;
import com.cardnova.giftchat.model.AdminDirectConversation;
import com.cardnova.giftchat.model.AdminUserItem;
import com.cardnova.giftchat.model.AgentItem;
import com.cardnova.giftchat.model.BankAccountRiskMatch;
import com.cardnova.giftchat.model.LotteryRecordItem;
import com.cardnova.giftchat.model.ReferralRewardConfigItem;
import com.cardnova.giftchat.model.ReferralRewardItem;
import com.cardnova.giftchat.model.RegistrationBonusConfigItem;
import com.cardnova.giftchat.model.RegistrationBonusRecordItem;
import com.cardnova.giftchat.model.SupportConversation;
import com.cardnova.giftchat.service.AdminService;
import com.cardnova.giftchat.service.BankAccountRiskService;
import com.cardnova.giftchat.service.CurrentUserService;
import com.cardnova.giftchat.service.LotteryService;
import com.cardnova.giftchat.service.ReferralRewardService;
import com.cardnova.giftchat.service.RegistrationBonusService;
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
    private final ReferralRewardService referralRewardService;
    private final RegistrationBonusService registrationBonusService;
    private final BankAccountRiskService bankAccountRiskService;
    private final CurrentUserService currentUserService;
    private final LotteryService lotteryService;

    public AdminController(
        AdminService adminService,
        ReferralRewardService referralRewardService,
        RegistrationBonusService registrationBonusService,
        BankAccountRiskService bankAccountRiskService,
        CurrentUserService currentUserService,
        LotteryService lotteryService
    ) {
        this.adminService = adminService;
        this.referralRewardService = referralRewardService;
        this.registrationBonusService = registrationBonusService;
        this.bankAccountRiskService = bankAccountRiskService;
        this.currentUserService = currentUserService;
        this.lotteryService = lotteryService;
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

    @PostMapping("/agents/{agentId}/welcome-message")
    public ApiResponse<AgentItem> updateAgentWelcomeMessage(
        @PathVariable String agentId,
        @Valid @RequestBody UpdateAgentWelcomeMessageRequest request
    ) {
        return ApiResponse.success("agent_welcome_message_updated", adminService.updateAgentWelcomeMessage(agentId, request));
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

    @GetMapping("/referral-rewards/config")
    public ApiResponse<ReferralRewardConfigItem> referralRewardConfig() {
        return ApiResponse.success(referralRewardService.config());
    }

    @PostMapping("/referral-rewards/config")
    public ApiResponse<ReferralRewardConfigItem> updateReferralRewardConfig(
        @Valid @RequestBody UpdateReferralRewardConfigRequest request
    ) {
        return ApiResponse.success("referral_reward_config_updated", referralRewardService.updateConfig(request));
    }

    @GetMapping("/referral-rewards")
    public ApiResponse<List<ReferralRewardItem>> referralRewards() {
        return ApiResponse.success(referralRewardService.rewards());
    }

    @GetMapping("/registration-bonuses/config")
    public ApiResponse<List<RegistrationBonusConfigItem>> registrationBonusConfigs() {
        return ApiResponse.success(registrationBonusService.configs());
    }

    @PostMapping("/registration-bonuses/config")
    public ApiResponse<RegistrationBonusConfigItem> updateRegistrationBonusConfig(
        @Valid @RequestBody UpdateRegistrationBonusConfigRequest request
    ) {
        return ApiResponse.success("registration_bonus_config_updated", registrationBonusService.updateConfig(request));
    }

    @GetMapping("/registration-bonuses")
    public ApiResponse<List<RegistrationBonusRecordItem>> registrationBonusRecords() {
        return ApiResponse.success(registrationBonusService.records());
    }

    @GetMapping("/bank-account-risks")
    public ApiResponse<List<BankAccountRiskMatch>> bankAccountRisks() {
        var currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);
        return ApiResponse.success(bankAccountRiskService.allPlatformMatches(currentUser));
    }

    @GetMapping("/lottery/records")
    public ApiResponse<List<LotteryRecordItem>> lotteryRecords() {
        return ApiResponse.success(lotteryService.adminRecords());
    }

    @PostMapping("/lottery/records/{recordId}/status")
    public ApiResponse<LotteryRecordItem> updateLotteryRecordStatus(
        @PathVariable String recordId,
        @Valid @RequestBody UpdateLotteryRecordStatusRequest request
    ) {
        return ApiResponse.success("lottery_record_status_updated", lotteryService.updateRecordStatus(recordId, request.status()));
    }

    @PostMapping("/lottery/users/{userId}/reset")
    public ApiResponse<Boolean> resetLotteryEligibility(
        @PathVariable String userId,
        @Valid @RequestBody ResetLotteryEligibilityRequest request
    ) {
        return ApiResponse.success("lottery_eligibility_reset", lotteryService.resetUserEligibility(userId, request.reason()));
    }
}
