package com.cardnova.giftchat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
class GiftChatServerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginReturnsJwtForActiveUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "demo@cardnova.app",
                      "password": "demo12345"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isString())
            .andExpect(jsonPath("$.data.roleCode").value("USER"))
            .andExpect(jsonPath("$.data.username").value("cardnova_user"))
            .andExpect(jsonPath("$.data.nextRoute").value("/pages/support/index"));
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
            .andExpect(jsonPath("$.data.nextRoute").value("/pages/admin-rates/index"));
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
            .andExpect(content().string(containsString("Apple(itunes)")))
            .andExpect(content().string(containsString("American Express")));
    }

    @Test
    void supportConversationsAreScopedByAuthenticatedUser() throws Exception {
        String user2Token = loginToken("gift_hunter");
        String user1Token = loginToken("cardnova_user");

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
                      "password": "demo12345"
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
            .andExpect(jsonPath("$.data.cardName").value("Test Card"));
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

    private String loginToken(String identifier) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "%s",
                      "password": "demo12345"
                    }
                    """.formatted(identifier)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    private String registerToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
