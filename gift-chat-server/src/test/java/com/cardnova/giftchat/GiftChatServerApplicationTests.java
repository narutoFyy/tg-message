package com.cardnova.giftchat;

import com.cardnova.giftchat.api.ForbiddenException;
import com.cardnova.giftchat.config.AuthSafetyConfig;
import com.cardnova.giftchat.entity.LotteryDrawRecordEntity;
import com.cardnova.giftchat.entity.LotteryPrizeEntity;
import com.cardnova.giftchat.entity.GiftCardRateEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.LotteryDrawRecordRepository;
import com.cardnova.giftchat.repository.LotteryPrizeRepository;
import com.cardnova.giftchat.repository.GiftCardRateRepository;
import com.cardnova.giftchat.repository.RegistrationBonusRecordRepository;
import com.cardnova.giftchat.repository.ReferralRewardRepository;
import com.cardnova.giftchat.repository.UserRepository;
import com.cardnova.giftchat.service.ReferralRewardService;
import com.cardnova.giftchat.service.RegistrationBonusService;
import com.cardnova.giftchat.service.WebSocketChannelAuthorizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class GiftChatServerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebSocketChannelAuthorizationService webSocketChannelAuthorizationService;

    @Autowired
    private ReferralRewardService referralRewardService;

    @Autowired
    private RegistrationBonusService registrationBonusService;

    @Autowired
    private ReferralRewardRepository referralRewardRepository;

    @Autowired
    private RegistrationBonusRecordRepository registrationBonusRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LotteryPrizeRepository lotteryPrizeRepository;

    @Autowired
    private LotteryDrawRecordRepository lotteryDrawRecordRepository;

    @Autowired
    private GiftCardRateRepository giftCardRateRepository;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ok"))
            .andExpect(jsonPath("$.data.time").isString());
    }

    @Test
    void productionSafetyRejectsPlaceholderJwtSecret() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            authSafety("prod", "replace-with-a-long-random-secret", "https://cardbrother.example", false)
                .validateProductionAuthSettings());

        assertEquals("APP_AUTH_JWT_SECRET must be a strong non-placeholder value outside dev/test", exception.getMessage());
    }

    @Test
    void productionSafetyRejectsDemoFallbackAndUnsafeCors() {
        assertThrows(IllegalStateException.class, () ->
            authSafety("prod", "a-very-long-random-jwt-secret-for-production-123", "https://cardbrother.example", true)
                .validateProductionAuthSettings());

        assertThrows(IllegalStateException.class, () ->
            authSafety("prod", "a-very-long-random-jwt-secret-for-production-123", "http://localhost:5174", false)
                .validateProductionAuthSettings());
    }

    @Test
    void productionSafetyAcceptsStrongProductionSettings() {
        assertDoesNotThrow(() ->
            authSafety("prod", "a-very-long-random-jwt-secret-for-production-123", "https://cardbrother.example", false)
                .validateProductionAuthSettings());
    }

    @Test
    void loginReturnsJwtForActiveUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "demo@cardnova.app",
                      "password": "demo12345",
                      "countryCode": "NG"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isString())
            .andExpect(jsonPath("$.data.roleCode").value("USER"))
            .andExpect(jsonPath("$.data.username").value("cardnova_user"))
            .andExpect(jsonPath("$.data.nextRoute").value("/pages/support/index"));
    }

    @Test
    void loginFailuresDoNotRevealAccountState() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "missing_%s",
                      "password": "wrong-password"
                    }
                    """.formatted(UUID.randomUUID().toString().replace("-", ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid account, password, or country"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "cardnova_user",
                      "password": "wrong-password"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid account, password, or country"));
    }

    @Test
    void repeatedLoginFailuresAreRateLimited() throws Exception {
        String identifier = "missing_%s".formatted(UUID.randomUUID().toString().replace("-", ""));

        for (int index = 0; index < 5; index++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "identifier": "%s",
                          "password": "wrong-password"
                        }
                        """.formatted(identifier)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid account, password, or country"));
        }

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "%s",
                      "password": "wrong-password"
                    }
                    """.formatted(identifier)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Too many login attempts, please try again later"));
    }

    @Test
    void adminAndAgentLoginsUseSeparateLandingRoutes() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "support_luna",
                      "password": "demo12345"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleCode").value("AGENT"))
            .andExpect(jsonPath("$.data.nextRoute").value("/pages/support-chat-v2/index"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "admin_mia",
                      "password": "demo12345"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleCode").value("ADMIN"))
            .andExpect(jsonPath("$.data.nextRoute").value("/pages/admin-console/index"));
    }

    @Test
    void ordinaryUserLoginRequiresBoundCountryButStaffBypassIt() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "cardnova_user",
                      "password": "demo12345",
                      "countryCode": "IN"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid account, password, or country"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "support_luna",
                      "password": "demo12345",
                      "countryCode": "US"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleCode").value("AGENT"));
    }

    @Test
    void adminCanConfigureAgentWelcomeMessagesForNewRegistrations() throws Exception {
        String adminToken = loginToken("admin_mia");
        List<String> agentIds = adminAgentIds(adminToken);
        String welcomeMessage = "Welcome test " + UUID.randomUUID();

        for (String agentId : agentIds) {
            mockMvc.perform(post("/api/admin/agents/%s/welcome-message".formatted(agentId))
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "content": "%s",
                          "enabled": true
                        }
                        """.formatted(welcomeMessage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.welcomeMessage").value(welcomeMessage))
                .andExpect(jsonPath("$.data.welcomeMessageEnabled").value(true))
                .andExpect(jsonPath("$.data.welcomeMessageUpdatedBy").value("admin_mia"));
        }

        String enabledUserToken = registerToken("welcome_enabled_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        JsonNode enabledConversations = supportConversations(enabledUserToken);
        assertEquals(1, countSupportMessagesWithContent(enabledConversations, welcomeMessage));

        JsonNode repeatedConversations = supportConversations(enabledUserToken);
        assertEquals(1, countSupportMessagesWithContent(repeatedConversations, welcomeMessage));

        String disabledMessage = "Disabled welcome " + UUID.randomUUID();
        for (String agentId : agentIds) {
            mockMvc.perform(post("/api/admin/agents/%s/welcome-message".formatted(agentId))
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "content": "%s",
                          "enabled": false
                        }
                        """.formatted(disabledMessage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.welcomeMessageEnabled").value(false));
        }

        String disabledUserToken = registerToken("welcome_disabled_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        JsonNode disabledConversations = supportConversations(disabledUserToken);
        assertEquals(0, countSupportMessagesWithContent(disabledConversations, disabledMessage));
    }

    @Test
    void registerWithEmailReturnsSupportRoute() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "email_user_" + suffix;
        String email = username + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "NG",
                      "username": "%s",
                      "email": "%s",
                      "password": "demo12345"
                    }
                    """.formatted(username, email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.roleCode").value("USER"))
            .andExpect(jsonPath("$.data.nextRoute").value("/pages/support/index"));
    }

    @Test
    void registerPhoneMustMatchSelectedCountryLength() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "IN",
                      "username": "india_phone_%s",
                      "phone": "+919876543210",
                      "password": "demo12345"
                    }
                    """.formatted(suffix)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.phone").value("+919876543210"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "IN",
                      "username": "bad_india_%s",
                      "phone": "+91987654321",
                      "password": "demo12345"
                    }
                    """.formatted(suffix)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("India phone number must be 10 digits"));
    }

    @Test
    void loginAndRegistrationCountryOptionsAreAligned() throws Exception {
        mockMvc.perform(get("/api/country-codes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(6)))
            .andExpect(jsonPath("$.data[0].countryCode").value("+234"))
            .andExpect(jsonPath("$.data[1].countryCode").value("+91"))
            .andExpect(jsonPath("$.data[2].countryCode").value("+237"))
            .andExpect(jsonPath("$.data[3].countryCode").value("+233"))
            .andExpect(jsonPath("$.data[4].countryCode").value("+254"))
            .andExpect(jsonPath("$.data[5].code").value("US"))
            .andExpect(jsonPath("$.data[5].countryCode").value("+1"))
            .andExpect(jsonPath("$.data[5].currencyCode").value("USD"))
            .andExpect(content().string(not(containsString("China"))))
            .andExpect(content().string(not(containsString("+86"))));

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "KE",
                      "username": "kenya_phone_%s",
                      "phone": "+254712345678",
                      "password": "demo12345"
                    }
                    """.formatted(suffix)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.phone").value("+254712345678"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "NG",
                      "username": "china_phone_%s",
                      "phone": "+8613800138000",
                      "password": "demo12345"
                    }
                    """.formatted(suffix)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unsupported phone country code"));
    }

    @Test
    void bankAccountBindingIsOnePerUserAndUniqueAcrossUsers() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String firstToken = registerToken("bank_one_" + suffix);
        String secondToken = registerToken("bank_two_" + suffix);
        String accountNumber = "88" + randomDigits(8);

        bindBankAccount(firstToken, "Nigeria", "Ada One", "Demo Bank", accountNumber.substring(0, 3) + " " + accountNumber.substring(3, 6) + "-" + accountNumber.substring(6))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.maskedAccountNumber").value("****" + accountNumber.substring(accountNumber.length() - 4)));

        bindBankAccount(firstToken, "Nigeria", "Ada One", "Second Bank", "99887766")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Each user can bind only one bank account"));

        bindBankAccount(secondToken, "Nigeria", "Other User", "Demo Bank", accountNumber)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("This bank account is already bound"));
    }

    @Test
    void lotteryWinnerCanRequestWithdrawalAfterBindingBankAccount() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "lottery_bank_" + suffix;
        String userToken = registerToken(username);

        bindBankAccount(userToken, "Nigeria", "Lucky User", "Prize Bank", "7000 8888")
            .andExpect(status().isOk());

        JsonNode spin = spinLottery(userToken);
        String recordId = spin.path("recordId").asText();

        MvcResult claimResult = mockMvc.perform(post("/api/lottery/records/%s/withdrawal-request".formatted(recordId))
            .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sourceType").value("lottery_cash"))
            .andExpect(jsonPath("$.data.note").value("Lottery prize withdrawal: " + recordId))
            .andExpect(jsonPath("$.data.bankName").value("Prize Bank"))
            .andReturn();
        String requestNo = objectMapper.readTree(claimResult.getResponse().getContentAsString()).path("data").path("requestNo").asText();

        mockMvc.perform(post("/api/lottery/records/%s/withdrawal-request".formatted(recordId))
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Lottery withdrawal request already exists"));

        mockMvc.perform(get("/api/support/conversations")
            .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Lottery withdrawal request"))))
            .andExpect(content().string(not(containsString(recordId))))
            .andExpect(content().string(containsString("I submitted a lottery cash claim")))
            .andExpect(content().string(containsString(requestNo)));
    }

    @Test
    void lotteryCashClaimBindsOnceStaysOutsideWalletAndAllowsReplacementAfterCompletion() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String userToken = registerToken("cash_claim_" + suffix);
        JsonNode spin = spinLottery(userToken);
        String recordId = spin.path("recordId").asText();

        MvcResult claimResult = mockMvc.perform(post("/api/lottery/records/%s/withdrawal-request".formatted(recordId))
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "bankAccount": {
                        "country": "Nigeria",
                        "accountName": "Cash Winner",
                        "bankName": "Lucky Bank",
                        "accountNumber": "70001234"
                      }
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sourceType").value("lottery_cash"))
            .andExpect(jsonPath("$.data.lotteryRecordId").value(recordId))
            .andExpect(jsonPath("$.data.prizeType").value("cash"))
            .andExpect(jsonPath("$.data.status").value("pending"))
            .andReturn();
        JsonNode claim = objectMapper.readTree(claimResult.getResponse().getContentAsString()).path("data");
        String withdrawalId = claim.path("id").asText();
        String assignedAgent = claim.path("assignedAgent").asText();

        mockMvc.perform(get("/api/bank-accounts/me")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.maskedAccountNumber").value("****1234"));

        mockMvc.perform(put("/api/bank-accounts/me")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"country":"Nigeria","accountName":"Cash Winner","bankName":"New Bank","accountNumber":"90005678"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Complete pending withdrawals before changing the bank account"));

        String agentToken = loginToken(assignedAgent);
        mockMvc.perform(post("/api/withdrawals/%s/status".formatted(withdrawalId))
                .header("Authorization", bearer(agentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"completed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("completed"));

        mockMvc.perform(get("/api/lottery/records/me")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("fulfilled")));

        mockMvc.perform(get("/api/balances/summary")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.availableTotal").value("0.00"))
            .andExpect(jsonPath("$.data.withdrawnTotal").value("0.00"));

        mockMvc.perform(put("/api/bank-accounts/me")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"country":"Nigeria","accountName":"Cash Winner","bankName":"New Bank","accountNumber":"90005678"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.maskedAccountNumber").value("****5678"));
    }

    @Test
    void physicalLotteryPrizeCreatesSupportFulfillmentOrderAndCompletesDraw() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "physical_claim_" + suffix;
        String userToken = registerToken(username);
        String recordId = createHistoricalLotteryRecord(username, "physical");

        MvcResult orderResult = mockMvc.perform(post("/api/lottery/records/%s/fulfillment-order".formatted(recordId))
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "recipientName":"Physical Winner",
                      "phone":"+2348012345678",
                      "country":"Nigeria",
                      "addressLine":"12 Prize Street, Lagos"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lotteryRecordId").value(recordId))
            .andExpect(jsonPath("$.data.prizeType").value("physical"))
            .andExpect(jsonPath("$.data.status").value("pending"))
            .andReturn();
        JsonNode order = objectMapper.readTree(orderResult.getResponse().getContentAsString()).path("data");
        String orderId = order.path("id").asText();
        String orderNo = order.path("orderNo").asText();
        String agentToken = loginToken(order.path("assignedAgent").asText());

        mockMvc.perform(get("/api/lottery-fulfillments")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(orderNo)));
        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Physical prize fulfillment"))))
            .andExpect(content().string(containsString("I submitted a delivery request for my")))
            .andExpect(content().string(containsString(orderNo)));
        mockMvc.perform(get("/api/lottery-fulfillments")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(orderNo)));

        mockMvc.perform(post("/api/lottery-fulfillments/%s/status".formatted(orderId))
                .header("Authorization", bearer(agentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"completed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("completed"));

        mockMvc.perform(get("/api/lottery/records/me")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("fulfilled")));
    }

    @Test
    void registerWithInviteCodeDoesNotCreateRegistrationRewardByDefault() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "invite_user_" + suffix;
        String email = username + "@example.com";
        disableReferralRegistrationCashback();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "NG",
                      "username": "%s",
                      "email": "%s",
                      "password": "demo12345",
                      "inviteCode": "CARDNOVA1"
                    }
                    """.formatted(username, email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.inviteCode").isString());

        UserEntity referredUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new AssertionError("Registered user missing"));
        assertEquals(0, referralRewardRepository.countByRewardTypeAndSourceKey("REGISTRATION", referredUser.getId()));
    }

    @Test
    void disabledReferralRegistrationRewardDoesNotCreateRecords() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "repeat_invite_" + suffix;
        String email = username + "@example.com";
        disableReferralRegistrationCashback();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "NG",
                      "username": "%s",
                      "email": "%s",
                      "password": "demo12345",
                      "inviteCode": "CARDNOVA1"
                    }
                    """.formatted(username, email)))
            .andExpect(status().isOk());

        UserEntity referredUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new AssertionError("Registered user missing"));

        referralRewardService.rewardRegistration(referredUser);
        referralRewardService.rewardRegistration(referredUser);

        assertEquals(0, referralRewardRepository.countByRewardTypeAndSourceKey("REGISTRATION", referredUser.getId()));
    }

    @Test
    void referralAdminEndpointsRequireAdmin() throws Exception {
        String userToken = loginToken("cardnova_user");

        mockMvc.perform(get("/api/admin/referral-rewards")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Admin access required"));

        mockMvc.perform(post("/api/admin/referral-rewards/config")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "registrationCashbackEnabled": true,
                      "registrationCashbackAmount": 1,
                      "tradeRebateEnabled": true,
                      "tradeRebatePercent": 5
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Admin access required"));
    }

    @Test
    void adminCanUpdateReferralRewardConfig() throws Exception {
        String adminToken = loginToken("admin_mia");

        mockMvc.perform(post("/api/admin/referral-rewards/config")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "registrationCashbackEnabled": true,
                      "registrationCashbackAmount": 2.50,
                      "tradeRebateEnabled": true,
                      "tradeRebatePercent": 7.5
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.registrationCashbackAmount").value("2.50"))
            .andExpect(jsonPath("$.data.tradeRebatePercent").value("7.5"))
            .andExpect(jsonPath("$.data.updatedBy").value("admin_mia"));
    }

    @Test
    void logoutAcceptsCurrentToken() throws Exception {
        String userToken = loginToken("cardnova_user");

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("logged_out"))
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void ratesEndpointReturnsCleanSeededRates() throws Exception {
        mockMvc.perform(get("/api/rates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(11)))
            .andExpect(content().string(containsString("Apple / iTunes")))
            .andExpect(content().string(containsString("APPLE_ITUNES")))
            .andExpect(content().string(containsString("American Express")));
    }

    @Test
    void currencyRatesAreSeparateFromGiftCardPayoutRates() throws Exception {
        String userToken = loginToken("cardnova_user");
        String adminToken = loginToken("admin_mia");

        mockMvc.perform(get("/api/currency-rates/me")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.countryCode").value("NG"))
            .andExpect(jsonPath("$.data.currencyCode").value("NGN"))
            .andExpect(jsonPath("$.data.localCurrencyPerUsd").value("1500"))
            .andExpect(jsonPath("$.data.displayRate").value("$1 = ₦1500"));

        mockMvc.perform(post("/api/admin/currency-rates")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "NG",
                      "localCurrencyPerUsd": 1510,
                      "enabled": true,
                      "note": "unauthorized"
                    }
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/currency-rates")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "NG",
                      "localCurrencyPerUsd": 1510,
                      "enabled": true,
                      "note": "manual test rate"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.localCurrencyPerUsd").value("1510"))
            .andExpect(jsonPath("$.data.updatedBy").value("admin_mia"));
    }

    @Test
    void rankingsEndpointReturnsSalesAndInvitationBoards() throws Exception {
        String userToken = loginToken("cardnova_user");

        mockMvc.perform(get("/api/rankings?mode=sales")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mode").value("sales"))
            .andExpect(jsonPath("$.data.currentUser.username").value("cardnova_user"));

        mockMvc.perform(get("/api/rankings?mode=invitation")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mode").value("invitation"))
            .andExpect(jsonPath("$.data.currentUser.username").value("cardnova_user"));
    }

    @Test
    void supportConversationsAreScopedByAuthenticatedUser() throws Exception {
        String user2Token = loginToken("gift_hunter");
        String user1Token = loginToken("cardnova_user");
        String adminToken = loginToken("admin_mia");

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(user2Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].customerUsername").value("gift_hunter"))
            .andExpect(jsonPath("$.data[0].assignmentStatus").value("AUTO_ASSIGNED"))
            .andExpect(jsonPath("$.data[0].assignedAgent").isString());

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].conversationId").value("support-1"));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(4)))
            .andExpect(content().string(containsString("cardnova_user")))
            .andExpect(content().string(containsString("john_smith")))
            .andExpect(content().string(containsString("mary_jane")))
            .andExpect(content().string(containsString("gift_hunter")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("admin_mia"))));
    }

    @Test
    void adminCanSendSupportMessagesAsStaff() throws Exception {
        String adminToken = loginToken("admin_mia");
        String customerToken = loginToken("john_smith");
        String uniqueMessage = "Admin staff reply " + UUID.randomUUID();

        MvcResult sentResult = mockMvc.perform(post("/api/support/conversations/support-2/messages")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "%s",
                      "messageType": "text"
                    }
                    """.formatted(uniqueMessage)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.author").value("me"))
            .andReturn();
        String messageId = objectMapper.readTree(sentResult.getResponse().getContentAsString())
            .at("/data/id")
            .asText();

        mockMvc.perform(get("/api/support/conversations/support-2/messages")
                .header("Authorization", bearer(customerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.id == '%s')].author".formatted(messageId)).value("support"))
            .andExpect(content().string(containsString(uniqueMessage)));
    }

    @Test
    void markingSupportConversationReadClearsUnreadCount() throws Exception {
        String user1Token = loginToken("cardnova_user");

        mockMvc.perform(post("/api/support/conversations/support-1/read")
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void customerMessagesBecomeReadAfterAssignedAgentReadsSupportConversation() throws Exception {
        String userToken = loginToken("john_smith");
        String agentToken = loginToken("support_luna");

        MvcResult sentResult = mockMvc.perform(post("/api/support/conversations/support-2/messages")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "please check this order",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.readState").value("sent"))
            .andReturn();
        String messageId = objectMapper.readTree(sentResult.getResponse().getContentAsString())
            .at("/data/id")
            .asText();

        mockMvc.perform(post("/api/support/conversations/support-2/read")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].messages[?(@.id == '%s')].readState".formatted(messageId)).value("read"));
    }

    @Test
    void systemMessagesDoNotCreateUnreadSupportBadges() throws Exception {
        String agentToken = loginToken("support_luna");

        mockMvc.perform(post("/api/support/conversations/support-1/read")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount").value(0));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "cardnova_user",
                      "password": "demo12345",
                      "countryCode": "NG"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.conversationId == 'support-1')].unreadCount").value(0));
    }

    @Test
    void markingSupportConversationReadIsSafeWhenRepeatedConcurrently() throws Exception {
        String user1Token = loginToken("cardnova_user");
        List<CompletableFuture<Void>> calls = new ArrayList<>();

        for (int index = 0; index < 8; index++) {
            calls.add(CompletableFuture.runAsync(() -> {
                try {
                    mockMvc.perform(post("/api/support/conversations/support-1/read")
                            .header("Authorization", bearer(user1Token)))
                        .andExpect(status().isOk());
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }));
        }

        CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new)).join();
    }

    @Test
    void sendingSupportMessagePersistsIntoConversation() throws Exception {
        String user1Token = loginToken("cardnova_user");

        mockMvc.perform(post("/api/support/conversations/support-1/messages")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Need payout help",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.author").value("me"))
            .andExpect(jsonPath("$.data.content").value("Need payout help"));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Need payout help")));
    }

    @Test
    void supportMessageDeltaReturnsOnlyMessagesAfterKnownCursor() throws Exception {
        String user1Token = loginToken("cardnova_user");

        MvcResult firstResult = mockMvc.perform(post("/api/support/conversations/support-1/messages")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Cursor baseline",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        String firstMessageId = objectMapper.readTree(firstResult.getResponse().getContentAsString())
            .at("/data/id")
            .asText();

        mockMvc.perform(post("/api/support/conversations/support-1/messages")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Cursor delta",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/support/conversations/support-1/messages")
                .param("afterId", firstMessageId)
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Cursor delta")))
            .andExpect(jsonPath("$.data[?(@.content == 'Cursor baseline')]").doesNotExist());
    }

    @Test
    void supportMessageDeltaRejectsCrossConversationAccess() throws Exception {
        String otherUserToken = loginToken("gift_hunter");

        mockMvc.perform(get("/api/support/conversations/support-1/messages")
                .header("Authorization", bearer(otherUserToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Support conversation not accessible"));
    }

    @Test
    void supportMessageSyncUsesServerSeqCursorAndReadAck() throws Exception {
        String userToken = loginToken("john_smith");
        String agentToken = loginToken("support_luna");

        MvcResult firstResult = mockMvc.perform(post("/api/support/conversations/support-2/messages")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Support sync baseline",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        long firstSeq = objectMapper.readTree(firstResult.getResponse().getContentAsString())
            .at("/data/serverSeq")
            .asLong();

        mockMvc.perform(post("/api/support/conversations/support-2/read")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk());

        MvcResult secondResult = mockMvc.perform(post("/api/support/conversations/support-2/messages")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Support sync delta",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        long secondSeq = objectMapper.readTree(secondResult.getResponse().getContentAsString())
            .at("/data/serverSeq")
            .asLong();

        mockMvc.perform(get("/api/support/conversations/support-2/messages/sync")
                .param("sinceSeq", Long.toString(firstSeq))
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messages[?(@.content == 'Support sync delta')]").exists())
            .andExpect(jsonPath("$.data.messages[?(@.content == 'Support sync baseline')]").doesNotExist())
            .andExpect(jsonPath("$.data.latestSeq").value(secondSeq))
            .andExpect(jsonPath("$.data.readSeq").value(firstSeq));

        mockMvc.perform(get("/api/support/conversations/support-2/messages/sync")
                .param("sinceSeq", Long.toString(firstSeq))
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messages[?(@.content == 'Support sync delta')]").exists())
            .andExpect(jsonPath("$.data.messages[?(@.content == 'Support sync baseline')]").doesNotExist())
            .andExpect(jsonPath("$.data.latestSeq").value(secondSeq))
            .andExpect(jsonPath("$.data.readSeq").value(0))
            .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    void supportMessageClientMessageIdIsIdempotent() throws Exception {
        String user1Token = loginToken("cardnova_user");
        String clientMessageId = "support-client-" + UUID.randomUUID();

        MvcResult firstResult = mockMvc.perform(post("/api/support/conversations/support-1/messages")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Idempotent support message",
                      "messageType": "text",
                      "clientMessageId": "%s"
                    }
                    """.formatted(clientMessageId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.clientMessageId").value(clientMessageId))
            .andExpect(jsonPath("$.data.serverSeq").isNumber())
            .andExpect(jsonPath("$.data.deliveryStatus").value("delivered"))
            .andReturn();

        JsonNode firstData = objectMapper.readTree(firstResult.getResponse().getContentAsString()).path("data");

        mockMvc.perform(post("/api/support/conversations/support-1/messages")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Idempotent support message duplicate body ignored",
                      "messageType": "text",
                      "clientMessageId": "%s"
                    }
                    """.formatted(clientMessageId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(firstData.path("id").asText()))
            .andExpect(jsonPath("$.data.serverSeq").value(firstData.path("serverSeq").asLong()))
            .andExpect(jsonPath("$.data.content").value("Idempotent support message"));
    }

    @Test
    void supportVideoSessionCreatesActionableVideoMessage() throws Exception {
        String user1Token = loginToken("cardnova_user");

        MvcResult videoResult = mockMvc.perform(post("/api/video-sessions")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "channelType": "support",
                      "channelId": "support-1"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.session.status").value("created"))
            .andReturn();
        String sessionId = objectMapper.readTree(videoResult.getResponse().getContentAsString())
            .at("/data/session/id")
            .asText();

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].messages[?(@.type == 'video')]").exists())
            .andExpect(content().string(containsString(sessionId)))
            .andExpect(content().string(containsString("video_call")));
    }

    @Test
    void friendsEndpointUsesTokenIdentity() throws Exception {
        String user2Token = loginToken("gift_hunter");

        mockMvc.perform(get("/api/friends")
                .header("Authorization", bearer(user2Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].username").value("cardnova_user"));
    }

    @Test
    void friendMessageDeltaReturnsOnlyMessagesAfterKnownCursor() throws Exception {
        String user1Token = loginToken("cardnova_user");
        String user2Token = loginToken("gift_hunter");

        MvcResult firstResult = mockMvc.perform(post("/api/friends/friendship-1/messages")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Friend cursor baseline",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        String firstMessageId = objectMapper.readTree(firstResult.getResponse().getContentAsString())
            .at("/data/id")
            .asText();

        mockMvc.perform(post("/api/friends/friendship-1/messages")
                .header("Authorization", bearer(user2Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Friend cursor delta",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/friends/friendship-1/messages")
                .param("afterId", firstMessageId)
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Friend cursor delta")))
            .andExpect(jsonPath("$.data[?(@.content == 'Friend cursor baseline')]").doesNotExist());
    }

    @Test
    void friendMessageSyncUsesServerSeqCursorAndReadAck() throws Exception {
        String user1Token = loginToken("cardnova_user");
        String user2Token = loginToken("gift_hunter");

        MvcResult firstResult = mockMvc.perform(post("/api/friends/friendship-1/messages")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Friend sync baseline",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        long firstSeq = objectMapper.readTree(firstResult.getResponse().getContentAsString())
            .at("/data/serverSeq")
            .asLong();

        mockMvc.perform(post("/api/friends/friendship-1/read")
                .header("Authorization", bearer(user2Token)))
            .andExpect(status().isOk());

        MvcResult secondResult = mockMvc.perform(post("/api/friends/friendship-1/messages")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Friend sync delta",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        long secondSeq = objectMapper.readTree(secondResult.getResponse().getContentAsString())
            .at("/data/serverSeq")
            .asLong();

        mockMvc.perform(get("/api/friends/friendship-1/messages/sync")
                .param("sinceSeq", Long.toString(firstSeq))
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messages[?(@.content == 'Friend sync delta')]").exists())
            .andExpect(jsonPath("$.data.messages[?(@.content == 'Friend sync baseline')]").doesNotExist())
            .andExpect(jsonPath("$.data.latestSeq").value(secondSeq))
            .andExpect(jsonPath("$.data.readSeq").value(firstSeq));

        mockMvc.perform(get("/api/friends/friendship-1/messages/sync")
                .param("sinceSeq", Long.toString(firstSeq))
                .header("Authorization", bearer(user2Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messages[?(@.content == 'Friend sync delta')]").exists())
            .andExpect(jsonPath("$.data.messages[?(@.content == 'Friend sync baseline')]").doesNotExist())
            .andExpect(jsonPath("$.data.latestSeq").value(secondSeq))
            .andExpect(jsonPath("$.data.readSeq").value(0))
            .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    void directMessageClientMessageIdIsIdempotent() throws Exception {
        String user1Token = loginToken("cardnova_user");
        String clientMessageId = "direct-client-" + UUID.randomUUID();

        MvcResult firstResult = mockMvc.perform(post("/api/friends/friendship-1/messages")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Idempotent direct message",
                      "messageType": "text",
                      "clientMessageId": "%s"
                    }
                    """.formatted(clientMessageId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.clientMessageId").value(clientMessageId))
            .andExpect(jsonPath("$.data.serverSeq").isNumber())
            .andExpect(jsonPath("$.data.deliveryStatus").value("delivered"))
            .andReturn();

        JsonNode firstData = objectMapper.readTree(firstResult.getResponse().getContentAsString()).path("data");

        mockMvc.perform(post("/api/friends/friendship-1/messages")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Idempotent direct message duplicate body ignored",
                      "messageType": "text",
                      "clientMessageId": "%s"
                    }
                    """.formatted(clientMessageId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(firstData.path("id").asText()))
            .andExpect(jsonPath("$.data.serverSeq").value(firstData.path("serverSeq").asLong()))
            .andExpect(jsonPath("$.data.content").value("Idempotent direct message"));
    }

    @Test
    void pushDeviceRegistrationIsIdempotentAndScopedToCurrentUser() throws Exception {
        String user1Token = loginToken("cardnova_user");
        String user2Token = loginToken("gift_hunter");
        String deviceToken = "native-token-" + UUID.randomUUID();

        MvcResult firstResult = mockMvc.perform(post("/api/push/devices")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "platform": "ios",
                      "provider": "tencent",
                      "deviceToken": "%s",
                      "deviceModel": "iPhone",
                      "appVersion": "1.0.0"
                    }
                    """.formatted(deviceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.platform").value("ios"))
            .andExpect(jsonPath("$.data.provider").value("tencent"))
            .andReturn();
        String deviceId = objectMapper.readTree(firstResult.getResponse().getContentAsString())
            .at("/data/id")
            .asText();

        mockMvc.perform(post("/api/push/devices")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "platform": "ios",
                      "provider": "tencent",
                      "deviceToken": "%s",
                      "deviceModel": "iPhone 15",
                      "appVersion": "1.0.1"
                    }
                    """.formatted(deviceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(deviceId))
            .andExpect(jsonPath("$.data.deviceModel").value("iPhone 15"));

        mockMvc.perform(delete("/api/push/devices/" + deviceId)
                .header("Authorization", bearer(user2Token)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Push device not accessible"));

        mockMvc.perform(delete("/api/push/devices/" + deviceId)
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void transactionsEndpointReturnsScopedOrders() throws Exception {
        String user1Token = loginToken("cardnova_user");
        String user2Token = loginToken("gift_hunter");

        mockMvc.perform(get("/api/transactions")
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(3)))
            .andExpect(content().string(containsString("CB240527-001")));

        mockMvc.perform(get("/api/transactions")
                .header("Authorization", bearer(user2Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void transactionStatusCanAdvanceButNotReopenCompletedTrade() throws Exception {
        String user1Token = loginToken("cardnova_user");

        mockMvc.perform(post("/api/transactions/trade-1/status")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "processing"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("processing"));

        mockMvc.perform(post("/api/transactions/trade-2/status")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "completed"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("completed"));

        mockMvc.perform(post("/api/transactions/trade-3/status")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "processing"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid transaction status transition"));
    }

    @Test
    void assignedAgentCanAdvanceOwnCustomerTrade() throws Exception {
        String agentToken = loginToken("support_luna");
        String otherAgentToken = loginToken("support_angela");

        mockMvc.perform(post("/api/transactions/trade-1/status")
                .header("Authorization", bearer(agentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "processing"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("processing"));

        mockMvc.perform(post("/api/transactions/trade-2/status")
                .header("Authorization", bearer(otherAgentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "completed"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Transaction not accessible"));
    }

    @Test
    void creatingRateRequiresAdminIdentity() throws Exception {
        String user1Token = loginToken("cardnova_user");
        String agentToken = loginToken("support_luna");
        String adminToken = loginToken("admin_mia");

        mockMvc.perform(post("/api/admin/rates")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Test Card",
                      "region": "NG",
                      "rate": "NGN 999.99 / $1"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Admin access required"));

        mockMvc.perform(post("/api/admin/rates")
                .header("Authorization", bearer(agentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Test Card",
                      "region": "NG",
                      "rate": "NGN 999.99 / $1"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Admin access required"));

        mockMvc.perform(post("/api/admin/rates")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Test Card",
                      "region": "NG",
                      "rate": "NGN 999.99 / $1"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cardName").value("Test Card"))
            .andExpect(jsonPath("$.data.currencyCode").value("NGN"))
            .andExpect(jsonPath("$.data.localPayoutPerUsd").value("999.99"))
            .andExpect(jsonPath("$.data.rate").value("$1 = ₦999.99"));

        mockMvc.perform(post("/api/admin/rates")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Nigeria Test Card",
                      "region": "Nigeria",
                      "rate": "NGN 1000 / $1"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.region").value("NG"))
            .andExpect(jsonPath("$.data.cardCode").doesNotExist());

        mockMvc.perform(post("/api/admin/rates")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Forged card name",
                      "cardCode": "STEAM",
                      "region": "NG",
                      "rate": "NGN 880 / $1"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cardCode").value("STEAM"))
            .andExpect(jsonPath("$.data.cardName").value("Steam"));

        mockMvc.perform(post("/api/admin/rates")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Amex",
                      "region": "NG",
                      "rate": "NGN 470 / $1"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cardCode").value("AMERICAN_EXPRESS"))
            .andExpect(jsonPath("$.data.cardName").value("American Express"));

        mockMvc.perform(post("/api/admin/rates")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Forged card name",
                      "cardCode": "NOT_A_CARD",
                      "region": "NG",
                      "rate": "NGN 100 / $1"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unsupported gift card code"));
    }

    @Test
    void sellOrderCreatesTransactionAndSupportMessage() throws Exception {
        String user1Token = loginToken("cardnova_user");

        mockMvc.perform(post("/api/transactions/sell-orders")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Razer Gold",
                      "cardCountry": "AUD",
                      "settlementCountry": "NG",
                      "faceValue": 50,
                      "quantity": 1,
                      "rate": "1$ ≈ ₦1076.56",
                      "settlementAmount": "₦53828",
                      "cardType": "Physical",
                      "speed": "Fast",
                      "cardData": "Code optional"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cardName").value("Razer Gold"))
            .andExpect(jsonPath("$.data.payoutAmount").value("₦53828"))
            .andExpect(jsonPath("$.data.status").value("pending"));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Sell order")))
            .andExpect(content().string(containsString("Razer Gold")));
    }

    @Test
    void sellOrderUsesServerRateAndKeepsSnapshotAfterRateChanges() throws Exception {
        String userToken = loginToken("cardnova_user");
        MvcResult createResult = mockMvc.perform(post("/api/transactions/sell-orders")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Razer Gold",
                      "cardCountry": "USD",
                      "settlementCountry": "US",
                      "faceValue": 10,
                      "quantity": 2,
                      "rate": "1",
                      "settlementAmount": "FORGED 1",
                      "cardType": "Digital",
                      "speed": "Fast",
                      "sendChatMessage": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.baseAmountUsd").value("20"))
            .andExpect(jsonPath("$.data.localAmount").value("21531.2"))
            .andExpect(jsonPath("$.data.currencyCode").value("NGN"))
            .andExpect(jsonPath("$.data.businessRate").value("1076.56"))
            .andExpect(jsonPath("$.data.payoutAmount").value("₦21531.2"))
            .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        String orderId = created.path("id").asText();
        GiftCardRateEntity rate = giftCardRateRepository.findById("rate-3").orElseThrow();
        BigDecimal originalRate = rate.getLocalPayoutPerUsd();
        try {
            rate.setLocalPayoutPerUsd(new BigDecimal("999.000000"));
            giftCardRateRepository.saveAndFlush(rate);

            mockMvc.perform(get("/api/transactions/{transactionId}", orderId)
                    .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.localAmount").value("21531.2"))
                .andExpect(jsonPath("$.data.businessRate").value("1076.56"));
        } finally {
            rate.setLocalPayoutPerUsd(originalRate);
            giftCardRateRepository.saveAndFlush(rate);
        }
    }

    @Test
    void lotteryCashDrawStoresCurrencySnapshot() throws Exception {
        String token = registerToken("lottery_snapshot_" + UUID.randomUUID().toString().substring(0, 8));
        MvcResult result = mockMvc.perform(post("/api/lottery/spin")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.prize.baseAmountUsd").isNotEmpty())
            .andExpect(jsonPath("$.data.prize.localAmount").isNotEmpty())
            .andExpect(jsonPath("$.data.prize.currencyCode").value("NGN"))
            .andExpect(jsonPath("$.data.prize.exchangeRate").isNotEmpty())
            .andReturn();

        JsonNode draw = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        String recordId = draw.path("recordId").asText();
        LotteryDrawRecordEntity record = lotteryDrawRecordRepository.findById(recordId).orElseThrow();
        assertNotNull(record.getBaseAmountUsd());
        assertNotNull(record.getLocalAmount());
        assertEquals("NGN", record.getCurrencyCode());
        assertEquals(0, new BigDecimal(draw.path("prize").path("exchangeRate").asText()).compareTo(record.getExchangeRateSnapshot()));
    }

    @Test
    void withdrawalCreatesRequestAndSupportMessage() throws Exception {
        String user1Token = loginToken("cardnova_user");

        mockMvc.perform(post("/api/withdrawals")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": "₦10000",
                      "country": "Nigeria",
                      "accountName": "Jay Card",
                      "bankName": "Demo Bank",
                      "accountNumber": "1234567890",
                      "contact": "13800138000"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.amount").value("₦10000"))
            .andExpect(jsonPath("$.data.status").value("pending"));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Withdrawal request")))
            .andExpect(content().string(containsString("Demo Bank")));
    }

    @Test
    void registrationBonusUsesPhoneCountryCodeAndIsSkippedByDefault() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "bonus_in_" + suffix;
        String token = registerPhoneToken(username, "+91 90000 " + randomDigits(5));

        mockMvc.perform(get("/api/account/registration-bonus")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.countryCode").value("+91"))
            .andExpect(jsonPath("$.data.bonusAmount").value("0.00"))
            .andExpect(jsonPath("$.data.status").value("skipped"));

        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AssertionError("Registered bonus user missing"));
        registrationBonusService.awardRegistrationBonus(user);
        registrationBonusService.awardRegistrationBonus(user);

        assertEquals(1, registrationBonusRecordRepository.countByUser_Id(user.getId()));
    }

    @Test
    void adminCanBroadcastByPhoneCountryCode() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String indiaUsername = "india_broadcast_" + suffix;
        String indiaToken = registerPhoneToken(indiaUsername, "+91 91111 " + randomDigits(5));
        String adminToken = loginToken("admin_mia");
        String giftHunterToken = loginToken("gift_hunter");
        String message = "India rate window " + suffix;

        mockMvc.perform(post("/api/broadcasts")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scope": "all",
                      "content": "%s",
                      "messageType": "text",
                      "countryCodes": ["+91"]
                    }
                    """.formatted(message)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.countryCodes").value("+91"))
            .andExpect(jsonPath("$.data.deliveredCount").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(indiaToken)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(message)));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(giftHunterToken)))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString(message))));
    }

    @Test
    void duplicateBankAccountsAreRejectedBeforeWithdrawalCreation() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String firstUsername = "risk_a_" + suffix;
        String secondUsername = "risk_b_" + suffix;
        String firstToken = registerPhoneToken(firstUsername, "+234 701 " + randomDigits(7));
        String secondToken = registerPhoneToken(secondUsername, "+233 24 " + randomDigits(7));
        String accountNumber = "445566" + suffix.substring(0, 4);

        createWithdrawal(firstToken, "Risk Alpha", accountNumber);
        mockMvc.perform(post("/api/withdrawals")
                .header("Authorization", bearer(secondToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": "₦10000",
                      "country": "Nigeria",
                      "accountName": "Risk Beta",
                      "bankName": "Duplicate Risk Bank",
                      "accountNumber": "%s",
                      "contact": "risk-contact",
                      "sendChatMessage": false
                    }
                    """.formatted(accountNumber)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("This bank account is already bound"));
    }

    @Test
    void supportAgentCanCancelOwnCustomerOrderWithReason() throws Exception {
        String userToken = loginToken("cardnova_user");
        String agentToken = loginToken("support_luna");
        String otherAgentToken = loginToken("support_angela");

        MvcResult createResult = mockMvc.perform(post("/api/transactions/sell-orders")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Apple(itunes)",
                      "cardCountry": "US",
                      "settlementCountry": "NG",
                      "faceValue": 20,
                      "quantity": 1,
                      "rate": "1$ ≈ ₦1051.75",
                      "settlementAmount": "₦21035",
                      "cardType": "Digital",
                      "speed": "Fast",
                      "cardData": "bad-code-demo",
                      "sendChatMessage": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("pending"))
            .andReturn();
        String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).at("/data/id").asText();

        mockMvc.perform(post("/api/transactions/%s/cancel".formatted(orderId))
                .header("Authorization", bearer(otherAgentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason": "Bad card"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Transaction not accessible"));

        mockMvc.perform(post("/api/transactions/%s/cancel".formatted(orderId))
                .header("Authorization", bearer(agentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason": "Bad card",
                      "note": "Unreadable code",
                      "notifyCustomer": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("canceled"))
            .andExpect(jsonPath("$.data.cancelReason").value("Bad card"))
            .andExpect(jsonPath("$.data.cancelNote").value("Unreadable code"))
            .andExpect(jsonPath("$.data.canceledBy").value("support_luna"))
            .andExpect(jsonPath("$.data.canceledAt").isString());
    }

    @Test
    void agentBroadcastsToAssignedCustomers() throws Exception {
        String agentToken = loginToken("support_luna");
        String user1Token = loginToken("cardnova_user");

        mockMvc.perform(post("/api/broadcasts")
                .header("Authorization", bearer(agentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scope": "own",
                      "content": "Today rate is updated",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.deliveredCount").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Today rate is updated")));
    }

    @Test
    void adminBroadcastsToAllActiveUsers() throws Exception {
        String adminToken = loginToken("admin_mia");
        String user2Token = loginToken("gift_hunter");

        mockMvc.perform(post("/api/broadcasts")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scope": "all",
                      "content": "Admin payout announcement",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.deliveredCount").value(greaterThanOrEqualTo(3)));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(user2Token)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Admin payout announcement")));
    }

    @Test
    void broadcastScopeIsEnforcedByRole() throws Exception {
        String agentToken = loginToken("support_luna");
        String adminToken = loginToken("admin_mia");

        mockMvc.perform(post("/api/broadcasts")
                .header("Authorization", bearer(agentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scope": "all",
                      "content": "Agent should not reach everyone",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Agents can only broadcast to own customers"));

        mockMvc.perform(post("/api/broadcasts")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scope": "own",
                      "content": "Admin own scope is invalid",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Admins can only broadcast to all users"));
    }

    @Test
    void translationEndpointIsLimitedToStaff() throws Exception {
        String userToken = loginToken("cardnova_user");
        String agentToken = loginToken("support_luna");

        mockMvc.perform(post("/api/translations/zh")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "text": "hello bro"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Agent or admin access required"));

        mockMvc.perform(post("/api/translations/zh")
                .header("Authorization", bearer(agentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "text": "hello bro"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.translatedText").isString());
    }

    @Test
    void agentCanUpdateOwnCustomerNote() throws Exception {
        String agentToken = loginToken("support_luna");
        String otherAgentToken = loginToken("support_angela");
        String userToken = loginToken("cardnova_user");

        mockMvc.perform(post("/api/support/conversations/support-2/note")
                .header("Authorization", bearer(agentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "note": "VIP Apple seller"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.agentNote").value("VIP Apple seller"));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.conversationId == 'support-2')].agentNote").value("VIP Apple seller"));

        mockMvc.perform(post("/api/support/conversations/support-2/note")
                .header("Authorization", bearer(otherAgentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "note": "Should fail"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Support conversation not accessible"));

        mockMvc.perform(post("/api/support/conversations/support-2/note")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "note": "Should fail"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Agent or admin access required"));
    }

    @Test
    void agentCanViewOwnCustomerProfileOnly() throws Exception {
        String agentToken = loginToken("support_luna");
        String otherAgentToken = loginToken("support_angela");
        String userToken = loginToken("cardnova_user");

        mockMvc.perform(get("/api/support/conversations/support-2/customer-profile")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.conversationId").value("support-2"))
            .andExpect(jsonPath("$.data.customer.username").value("john_smith"))
            .andExpect(jsonPath("$.data.customer.assignedAgent").value("support_luna"))
            .andExpect(jsonPath("$.data.balance.availableTotal").isString())
            .andExpect(jsonPath("$.data.orders").isArray())
            .andExpect(jsonPath("$.data.withdrawals").isArray())
            .andExpect(jsonPath("$.data.lotteryFulfillments").isArray())
            .andExpect(jsonPath("$.data.loans").isArray())
            .andExpect(jsonPath("$.data.videoSessions").isArray());

        mockMvc.perform(get("/api/support/conversations/support-2/customer-profile")
                .header("Authorization", bearer(otherAgentToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Support conversation not accessible"));

        mockMvc.perform(get("/api/support/conversations/support-2/customer-profile")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Agent or admin access required"));
    }

    @Test
    void supportLedgerIsScopedToStaffCustomers() throws Exception {
        String agentToken = loginToken("support_luna");
        String adminToken = loginToken("admin_mia");
        String userToken = loginToken("cardnova_user");

        mockMvc.perform(get("/api/support/ledger")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.scope").value("own"))
            .andExpect(jsonPath("$.data.summary.userCount").value(6))
            .andExpect(jsonPath("$.data.customers").isArray())
            .andExpect(jsonPath("$.data.customers[?(@.customerUsername == 'cardnova_user')]").exists())
            .andExpect(jsonPath("$.data.customers[?(@.customerUsername == 'gift_hunter')]").doesNotExist());

        mockMvc.perform(get("/api/support/ledger")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.scope").value("all"))
            .andExpect(jsonPath("$.data.customers[?(@.customerUsername == 'gift_hunter')]").exists());

        mockMvc.perform(get("/api/support/ledger")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Agent or admin access required"));
    }

    @Test
    void vipPointsAreAwardedOnceWhenSellOrderCompletes() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String userToken = registerToken("vip_points_" + suffix);

        MvcResult createResult = mockMvc.perform(post("/api/transactions/sell-orders")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Apple",
                      "cardCountry": "USA",
                      "settlementCountry": "Nigeria",
                      "faceValue": 25,
                      "quantity": 2,
                      "rate": "500",
                      "settlementAmount": "NGN 25000",
                      "cardType": "physical",
                      "speed": "standard",
                      "sendChatMessage": false
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .at("/data/id")
            .asText();

        for (int index = 0; index < 2; index++) {
            mockMvc.perform(post("/api/transactions/%s/status".formatted(orderId))
                    .header("Authorization", bearer(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "status": "completed"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"));
        }

        mockMvc.perform(get("/api/vip/me")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.level").value("VIP2"))
            .andExpect(jsonPath("$.data.points").value("50"))
            .andExpect(jsonPath("$.data.nextLevel").value("VIP3"));
    }

    @Test
    void vipOneLotteryChanceCanOnlyBeUsedOnce() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String userToken = registerToken("lottery_once_" + suffix);

        mockMvc.perform(get("/api/lottery/eligibility")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.vipLevel").value("VIP1"))
            .andExpect(jsonPath("$.data.eligible").value(true))
            .andExpect(jsonPath("$.data.periodType").value("ONCE"));

        MvcResult spinResult = mockMvc.perform(post("/api/lottery/spin")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.eligibility.eligible").value(false))
            .andExpect(jsonPath("$.data.recordId").isString())
            .andReturn();
        String prizeName = objectMapper.readTree(spinResult.getResponse().getContentAsString())
            .path("data")
            .path("prize")
            .path("name")
            .asText();
        assertTrue(List.of("₦100", "₦200", "₦500", "₦1000", "₦2000", "₦3000", "₦5000").contains(prizeName));

        mockMvc.perform(post("/api/lottery/spin")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Next chance available")));

        String otherUserToken = registerToken("lottery_invalid_" + suffix);
        MvcResult forcedPrizeResult = mockMvc.perform(post("/api/lottery/spin")
                .header("Authorization", bearer(otherUserToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "prizeName": "iPad"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        String forcedPrizeName = objectMapper.readTree(forcedPrizeResult.getResponse().getContentAsString())
            .path("data")
            .path("prize")
            .path("name")
            .asText();
        assertTrue(List.of("₦100", "₦200", "₦500", "₦1000", "₦2000", "₦3000", "₦5000").contains(forcedPrizeName));
    }

    @Test
    void supportSearchIsScopedAndFindsCustomersAndMessages() throws Exception {
        String agentToken = loginToken("support_luna");
        String otherAgentToken = loginToken("support_angela");

        mockMvc.perform(get("/api/support/search/customers")
                .param("keyword", "john")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.conversationId == 'support-2' && @.customerUsername == 'john_smith')]").exists());

        mockMvc.perform(get("/api/support/search/messages")
                .param("keyword", "USA $100")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.conversationId == 'support-2' && @.messageId == 'support-msg-6')]").exists());

        mockMvc.perform(get("/api/support/search/customers")
                .param("keyword", "john")
                .header("Authorization", bearer(otherAgentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void userHiddenRecordsDoNotHideOrdersOrMessagesFromStaff() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "hide_user_" + suffix;
        String userToken = registerToken(username);
        String adminToken = loginToken("admin_mia");
        String uniqueMessage = "hide-message-" + suffix;

        MvcResult createOrderResult = mockMvc.perform(post("/api/transactions/sell-orders")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cardName": "Steam",
                      "cardCountry": "USA",
                      "settlementCountry": "Nigeria",
                      "faceValue": 10,
                      "quantity": 1,
                      "rate": "400",
                      "settlementAmount": "NGN 4000",
                      "cardType": "ecode",
                      "speed": "fast",
                      "sendChatMessage": false
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        String orderId = objectMapper.readTree(createOrderResult.getResponse().getContentAsString())
            .at("/data/id")
            .asText();

        MvcResult conversationResult = mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andReturn();
        String conversationId = objectMapper.readTree(conversationResult.getResponse().getContentAsString())
            .at("/data/0/conversationId")
            .asText();
        String assignedAgent = objectMapper.readTree(conversationResult.getResponse().getContentAsString())
            .at("/data/0/assignedAgent")
            .asText();
        String assignedAgentToken = loginToken(assignedAgent);

        MvcResult messageResult = mockMvc.perform(post("/api/support/conversations/%s/messages".formatted(conversationId))
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "%s",
                      "messageType": "text"
                    }
                    """.formatted(uniqueMessage)))
            .andExpect(status().isOk())
            .andReturn();
        String messageId = objectMapper.readTree(messageResult.getResponse().getContentAsString())
            .at("/data/id")
            .asText();

        hideRecord(userToken, "ORDER", orderId, "ORDER");
        hideRecord(userToken, "MESSAGE", messageId, "SINGLE");

        MvcResult userOrdersResult = mockMvc.perform(get("/api/transactions")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode userOrders = objectMapper.readTree(userOrdersResult.getResponse().getContentAsString()).path("data");
        assertFalse(containsDataId(userOrders, orderId));

        MvcResult adminOrdersResult = mockMvc.perform(get("/api/transactions")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode adminOrders = objectMapper.readTree(adminOrdersResult.getResponse().getContentAsString()).path("data");
        assertTrue(containsDataId(adminOrders, orderId));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString(uniqueMessage))));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(assignedAgentToken)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(uniqueMessage)));
    }

    @Test
    void loanApplicationCanBeReviewedByAdmin() throws Exception {
        String user1Token = loginToken("cardnova_user");
        String adminToken = loginToken("admin_mia");

        MvcResult loanResult = mockMvc.perform(post("/api/loans")
                .header("Authorization", bearer(user1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": "NGN 100000",
                      "country": "Nigeria",
                      "purpose": "Need working capital for card trading",
                      "contact": "13800138000",
                      "repaymentPlan": "Repay after next payout"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("pending"))
            .andExpect(content().string(containsString("Need working capital")))
            .andReturn();

        String loanId = objectMapper.readTree(loanResult.getResponse().getContentAsString())
            .path("data")
            .path("id")
            .asText();

        mockMvc.perform(post("/api/loans/%s/status".formatted(loanId))
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "approved",
                      "reviewNote": "Approved for demo testing"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("approved"))
            .andExpect(jsonPath("$.data.reviewNote").value("Approved for demo testing"));

        mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(user1Token)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Loan application")))
            .andExpect(content().string(containsString("approved")));
    }

    @Test
    void adminCanQueryDirectConversationRecordsByUsername() throws Exception {
        String agentToken = loginToken("admin_mia");

        mockMvc.perform(get("/api/admin/direct/conversations")
                .param("username", "gift")
                .header("Authorization", bearer(agentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(content().string(containsString("gift_hunter")))
            .andExpect(content().string(containsString("Can we compare rates later today?")));
    }

    @Test
    void reciprocalBlacklistStillBlocksDirectMessagesAfterOneSideUnblocks() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String ownerUsername = "block_owner_" + suffix;
        String targetUsername = "block_target_" + suffix;
        String ownerToken = registerToken(ownerUsername);
        String targetToken = registerToken(targetUsername);

        MvcResult requestResult = mockMvc.perform(post("/api/friends/requests")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s"
                    }
                    """.formatted(targetUsername)))
            .andExpect(status().isOk())
            .andReturn();
        String friendshipId = objectMapper.readTree(requestResult.getResponse().getContentAsString())
            .path("data")
            .path("friendshipId")
            .asText();

        mockMvc.perform(post("/api/friends/requests/%s/accept".formatted(friendshipId))
                .header("Authorization", bearer(targetToken)))
            .andExpect(status().isOk());

        MvcResult ownerBlacklistResult = mockMvc.perform(post("/api/blacklist")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s"
                    }
                    """.formatted(targetUsername)))
            .andExpect(status().isOk())
            .andReturn();
        String ownerBlacklistId = objectMapper.readTree(ownerBlacklistResult.getResponse().getContentAsString())
            .path("data")
            .path("id")
            .asText();

        mockMvc.perform(post("/api/blacklist")
                .header("Authorization", bearer(targetToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s"
                    }
                    """.formatted(ownerUsername)))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/blacklist/%s".formatted(ownerBlacklistId))
                .header("Authorization", bearer(ownerToken)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/friends/%s/messages".formatted(friendshipId))
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "This should stay blocked",
                      "messageType": "text"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Friendship not accessible"));
    }

    @Test
    void websocketChannelAuthorizationRejectsCrossConversationAccess() {
        assertDoesNotThrow(() ->
            webSocketChannelAuthorizationService.requireAccess("user-5", "support", "support-2"));
        assertDoesNotThrow(() ->
            webSocketChannelAuthorizationService.requireAccess("agent-1", "support", "support-2"));
        assertDoesNotThrow(() ->
            webSocketChannelAuthorizationService.requireAccess("admin-1", "support", "support-2"));

        assertThrows(ForbiddenException.class, () ->
            webSocketChannelAuthorizationService.requireAccess("agent-2", "support", "support-2"));
        assertThrows(ForbiddenException.class, () ->
            webSocketChannelAuthorizationService.requireAccess("user-1", "support", "support-2"));
        assertThrows(ForbiddenException.class, () ->
            webSocketChannelAuthorizationService.requireAccess("user-5", "friend", "friendship-2"));
        assertThrows(ForbiddenException.class, () ->
            webSocketChannelAuthorizationService.requireAccess("user-2", "friend", "friendship-3"));
    }

    @Test
    void uploadImageValidatesRealFileContent() throws Exception {
        String userToken = loginToken("cardnova_user");
        MockMultipartFile fakeImage = new MockMultipartFile(
            "file",
            "fake.png",
            "image/png",
            "not really a png".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/uploads/images")
                .file(fakeImage)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unsupported image type"));

        byte[] pngHeaderOnly = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
        };
        MockMultipartFile realPng = new MockMultipartFile(
            "file",
            "avatar.txt",
            "text/plain",
            pngHeaderOnly
        );

        mockMvc.perform(multipart("/api/uploads/images")
                .file(realPng)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mimeType").value("image/png"))
            .andExpect(jsonPath("$.data.publicUrl", containsString(".png")));
    }

    @Test
    void referralTradeRewardIsIdempotentWhenCompletedRepeatedly() throws Exception {
        String referredToken = loginToken("gift_hunter");

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                .header("Authorization", bearer(referredToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "counterpartyUsername": "cardnova_user",
                      "friendshipId": "friendship-1",
                      "cardName": "Steam",
                      "faceValue": "$100",
                      "payoutAmount": "NGN 100000",
                      "note": "Idempotent reward test"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("pending"))
            .andReturn();
        String tradeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .at("/data/id")
            .asText();

        for (int index = 0; index < 2; index++) {
            mockMvc.perform(post("/api/transactions/%s/status".formatted(tradeId))
                    .header("Authorization", bearer(referredToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "status": "completed"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"));
        }

        org.junit.jupiter.api.Assertions.assertEquals(
            1,
            referralRewardRepository.countByRewardTypeAndSourceKey("TRADE_REBATE", tradeId)
        );
    }

    @Test
    void uploadImageRejectsOversizedFiles() throws Exception {
        String userToken = loginToken("cardnova_user");
        byte[] oversizedPng = new byte[5 * 1024 * 1024 + 1];
        oversizedPng[0] = (byte) 0x89;
        oversizedPng[1] = 0x50;
        oversizedPng[2] = 0x4E;
        oversizedPng[3] = 0x47;
        oversizedPng[4] = 0x0D;
        oversizedPng[5] = 0x0A;
        oversizedPng[6] = 0x1A;
        oversizedPng[7] = 0x0A;
        MockMultipartFile oversizedImage = new MockMultipartFile(
            "file",
            "oversized.png",
            "image/png",
            oversizedPng
        );

        mockMvc.perform(multipart("/api/uploads/images")
                .file(oversizedImage)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Image file is too large"));
    }

    @Test
    void userCanUpdateAvatarFromOwnedUpload() throws Exception {
        String userToken = loginToken("cardnova_user");
        byte[] pngHeaderOnly = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
        };
        MockMultipartFile avatar = new MockMultipartFile(
            "file",
            "avatar.png",
            "image/png",
            pngHeaderOnly
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/uploads/images")
                .file(avatar)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andReturn();
        String avatarUrl = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
            .path("data")
            .path("publicUrl")
            .asText();

        mockMvc.perform(post("/api/account/avatar")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "avatarUrl": "%s"
                    }
                    """.formatted(avatarUrl)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.avatarUrl").value(avatarUrl));

        mockMvc.perform(get("/api/account/me")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.avatarUrl").value(avatarUrl));
    }

    private String loginToken(String identifier) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "%s",
                      "password": "demo12345",
                      "countryCode": "NG"
                    }
                    """.formatted(identifier)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    private List<String> adminAgentIds(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/agents")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        List<String> agentIds = new ArrayList<>();
        for (JsonNode agent : data) {
            agentIds.add(agent.path("id").asText());
        }
        return agentIds;
    }

    private JsonNode supportConversations(String userToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/support/conversations")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private int countSupportMessagesWithContent(JsonNode conversations, String content) {
        int count = 0;
        for (JsonNode conversation : conversations) {
            for (JsonNode message : conversation.path("messages")) {
                if (content.equals(message.path("content").asText())) {
                    count++;
                }
            }
        }
        return count;
    }

    private ResultActions bindBankAccount(
        String token,
        String country,
        String accountName,
        String bankName,
        String accountNumber
    ) throws Exception {
        return mockMvc.perform(post("/api/bank-accounts")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "country": "%s",
                  "accountName": "%s",
                  "bankName": "%s",
                  "accountNumber": "%s"
                }
                """.formatted(country, accountName, bankName, accountNumber)));
    }

    private JsonNode spinLottery(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lottery/spin")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String createHistoricalLotteryRecord(String username, String prizeType) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        LotteryPrizeEntity prize = lotteryPrizeRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
            .filter(item -> prizeType.equalsIgnoreCase(item.getPrizeType()))
            .findFirst()
            .orElseThrow();
        LotteryDrawRecordEntity record = new LotteryDrawRecordEntity();
        record.setId(UUID.randomUUID().toString());
        record.setUser(user);
        record.setVipLevel("VIP1");
        record.setPrize(prize);
        record.setPeriodType("TEST");
        record.setPeriodKey(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        record.setDrawnAt(LocalDateTime.now());
        record.setFulfillmentStatus("PENDING");
        return lotteryDrawRecordRepository.saveAndFlush(record).getId();
    }

    private void disableReferralRegistrationCashback() throws Exception {
        String adminToken = loginToken("admin_mia");
        mockMvc.perform(post("/api/admin/referral-rewards/config")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "registrationCashbackEnabled": false,
                      "registrationCashbackAmount": 0,
                      "tradeRebateEnabled": true,
                      "tradeRebatePercent": 5
                    }
                    """))
            .andExpect(status().isOk());
    }

    private String registerToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "NG",
                      "username": "%s",
                      "email": "%s@example.com",
                      "password": "demo12345"
                    }
                    """.formatted(username, username)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    private String registerPhoneToken(String username, String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "countryCode": "%s",
                      "username": "%s",
                      "phone": "%s",
                      "password": "demo12345"
                    }
                    """.formatted(countryForPhone(phone), username, phone)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    private String randomDigits(int length) {
        String digits = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        while (digits.length() < length) {
            digits += UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        }
        return digits.substring(0, length);
    }

    private String countryForPhone(String phone) {
        String normalized = phone.replaceAll("[^0-9+]", "");
        if (normalized.startsWith("+91")) return "IN";
        if (normalized.startsWith("+233")) return "GH";
        if (normalized.startsWith("+237")) return "CM";
        if (normalized.startsWith("+254")) return "KE";
        if (normalized.startsWith("+1")) return "US";
        return "NG";
    }

    private void createWithdrawal(String token, String accountName, String accountNumber) throws Exception {
        mockMvc.perform(post("/api/withdrawals")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": "₦10000",
                      "country": "Nigeria",
                      "accountName": "%s",
                      "bankName": "Duplicate Risk Bank",
                      "accountNumber": "%s",
                      "contact": "risk-contact",
                      "sendChatMessage": false
                    }
                    """.formatted(accountName, accountNumber)))
            .andExpect(status().isOk());
    }

    private void hideRecord(String token, String targetType, String targetId, String hiddenScope) throws Exception {
        mockMvc.perform(post("/api/account/hidden-records")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "targetType": "%s",
                      "targetId": "%s",
                      "hiddenScope": "%s"
                    }
                    """.formatted(targetType, targetId, hiddenScope)))
            .andExpect(status().isOk());
    }

    private boolean containsDataId(JsonNode array, String id) {
        if (array == null || !array.isArray()) {
            return false;
        }
        for (JsonNode item : array) {
            if (id.equals(item.path("id").asText())) {
                return true;
            }
        }
        return false;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private AuthSafetyConfig authSafety(String profile, String jwtSecret, String allowedOrigins, boolean demoFallback) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return new AuthSafetyConfig(environment, jwtSecret, allowedOrigins, demoFallback);
    }
}
