package com.cardnova.giftchat;

import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.SupportMessageRepository;
import com.cardnova.giftchat.repository.TradeOrderRepository;
import com.cardnova.giftchat.repository.TradeOrderSettlementAuditRepository;
import com.cardnova.giftchat.repository.UserRepository;
import com.cardnova.giftchat.repository.VipPointLedgerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:giftchat-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=30000"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GiftChatConcurrencyTests {

    private static final int REGISTRATION_CONCURRENCY = 20;
    private static final int BUSINESS_CONCURRENCY = 50;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupportConversationRepository supportConversationRepository;

    @Autowired
    private SupportMessageRepository supportMessageRepository;

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    @Autowired
    private TradeOrderSettlementAuditRepository tradeOrderSettlementAuditRepository;

    @Autowired
    private VipPointLedgerRepository vipPointLedgerRepository;

    @Test
    void concurrentRegistrationChatSellOrdersSettlementAndVipRemainConsistent() throws Exception {
        String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        List<Registration> registrations = runConcurrent(REGISTRATION_CONCURRENCY, index ->
            register("load_" + runId + "_" + index)
        );

        assertEquals(REGISTRATION_CONCURRENCY, registrations.size());
        assertEquals(REGISTRATION_CONCURRENCY, registrations.stream().map(Registration::token).distinct().count());
        for (Registration registration : registrations) {
            UserEntity user = userRepository.findByUsername(registration.username()).orElseThrow();
            List<SupportConversationEntity> conversations =
                supportConversationRepository.findByCustomerUser_IdOrderByUpdatedAtDesc(user.getId());
            assertEquals(1, conversations.size(), "A customer must have exactly one support conversation");
            assertNotNull(conversations.getFirst().getAssignedAgent(), "Every new customer must be assigned");
        }

        long lunaCount = supportConversationRepository.countByAssignedAgent_IdAndCustomerUser_StatusCode("agent-1", "ACTIVE");
        long angelaCount = supportConversationRepository.countByAssignedAgent_IdAndCustomerUser_StatusCode("agent-2", "ACTIVE");
        assertTrue(Math.abs(lunaCount - angelaCount) <= 1,
            "Active customer assignments must remain balanced: " + lunaCount + " vs " + angelaCount);

        String messagePrefix = "load-message-" + runId + "-";
        List<Integer> messageStatuses = runConcurrent(BUSINESS_CONCURRENCY, index -> {
            Registration registration = registrations.get(index % registrations.size());
            UserEntity user = userRepository.findByUsername(registration.username()).orElseThrow();
            String conversationId = supportConversationRepository
                .findFirstByCustomerUser_IdOrderByUpdatedAtDesc(user.getId()).orElseThrow().getId();
            return sendMessage(registration.token(), conversationId, messagePrefix + index, index);
        });
        assertEquals(BUSINESS_CONCURRENCY, messageStatuses.stream().filter(status -> status == 200).count());
        assertEquals(BUSINESS_CONCURRENCY, supportMessageRepository.findAll().stream()
            .filter(message -> message.getClientMessageId() != null)
            .filter(message -> message.getClientMessageId().startsWith(messagePrefix))
            .count());

        String requestPrefix = "load-order-" + runId + "-";
        List<OrderResponse> orders = runConcurrent(BUSINESS_CONCURRENCY, index -> {
            Registration registration = registrations.get(index % registrations.size());
            return createSellOrder(registration.token(), requestPrefix + index);
        });
        assertEquals(BUSINESS_CONCURRENCY, orders.stream().filter(order -> order.status() == 200).count());
        assertEquals(BUSINESS_CONCURRENCY, orders.stream().map(OrderResponse::orderId).distinct().count());
        assertEquals(BUSINESS_CONCURRENCY, tradeOrderRepository.findAll().stream()
            .filter(order -> order.getClientRequestId() != null)
            .filter(order -> order.getClientRequestId().startsWith(requestPrefix))
            .count());

        OrderResponse settlementTarget = orders.getFirst();
        Registration ownerRegistration = registrations.getFirst();
        String ownerId = userRepository.findByUsername(ownerRegistration.username()).orElseThrow().getId();
        String agentToken = login(settlementTarget.counterpartyUsername());

        List<Integer> completionStatuses = runConcurrent(BUSINESS_CONCURRENCY,
            index -> completeOrder(agentToken, settlementTarget.orderId()));
        assertEquals(1, completionStatuses.stream().filter(status -> status == 200).count());
        assertEquals(BUSINESS_CONCURRENCY - 1,
            completionStatuses.stream().filter(status -> status == 409).count());
        assertEquals(1, tradeOrderSettlementAuditRepository.findAll().stream()
            .filter(audit -> audit.getTradeOrder().getId().equals(settlementTarget.orderId()))
            .filter(audit -> "COMPLETED".equals(audit.getActionCode()))
            .count());
        assertEquals(1, vipPointLedgerRepository.findAll().stream()
            .filter(ledger -> ("MANUAL_ORDER:" + settlementTarget.orderId()).equals(ledger.getSourceKey()))
            .count());
        assertEquals(new BigDecimal("50.00"), vipPointLedgerRepository.sumPointsByUserId(ownerId));

        List<ReadResponse> reads = runConcurrent(BUSINESS_CONCURRENCY, index ->
            readOrderAndVip(ownerRegistration.token(), settlementTarget.orderId())
        );
        assertEquals(BUSINESS_CONCURRENCY, reads.stream().filter(ReadResponse::successful).count());
        assertTrue(reads.stream().allMatch(read -> "completed".equals(read.orderStatus())));
        assertTrue(reads.stream().allMatch(read -> "VIP1".equals(read.vipLevel())));
        assertTrue(reads.stream().allMatch(read -> "50".equals(read.vipPoints())));
    }

    private Registration register(String username) {
        try {
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
                .andReturn();
            assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
            JsonNode data = responseData(result);
            return new Registration(username, data.path("accessToken").asText());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int sendMessage(String token, String conversationId, String clientMessageId, int index) {
        try {
            MvcResult result = mockMvc.perform(post("/api/support/conversations/{conversationId}/messages", conversationId)
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "content": "Concurrent message %d",
                          "messageType": "TEXT",
                          "clientMessageId": "%s"
                        }
                        """.formatted(index, clientMessageId)))
                .andReturn();
            return result.getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private OrderResponse createSellOrder(String token, String clientRequestId) {
        try {
            MvcResult result = mockMvc.perform(post("/api/transactions/sell-orders")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "cardName": "Razer Gold",
                          "cardCountry": "USD",
                          "settlementCountry": "NG",
                          "faceValue": 10,
                          "quantity": 1,
                          "rate": "server-calculated",
                          "settlementAmount": "server-calculated",
                          "cardType": "Digital",
                          "speed": "Fast",
                          "sendChatMessage": false,
                          "clientRequestId": "%s"
                        }
                        """.formatted(clientRequestId)))
                .andReturn();
            JsonNode data = responseData(result);
            return new OrderResponse(
                result.getResponse().getStatus(),
                data.path("id").asText(),
                data.path("counterpartyUsername").asText()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int completeOrder(String token, String orderId) {
        try {
            MvcResult result = mockMvc.perform(post("/api/transactions/{orderId}/complete", orderId)
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"finalLocalAmount":10000,"vipPoints":50,"reason":"Concurrency verification"}
                        """))
                .andReturn();
            return result.getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ReadResponse readOrderAndVip(String token, String orderId) {
        try {
            MvcResult orderResult = mockMvc.perform(get("/api/transactions/{orderId}", orderId)
                    .header("Authorization", bearer(token)))
                .andReturn();
            MvcResult vipResult = mockMvc.perform(get("/api/vip/me")
                    .header("Authorization", bearer(token)))
                .andReturn();
            JsonNode order = responseData(orderResult);
            JsonNode vip = responseData(vipResult);
            return new ReadResponse(
                orderResult.getResponse().getStatus() == 200 && vipResult.getResponse().getStatus() == 200,
                order.path("status").asText(),
                vip.path("level").asText(),
                vip.path("points").asText()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"identifier":"%s","password":"demo12345","countryCode":"NG"}
                    """.formatted(username)))
            .andReturn();
        assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return responseData(result).path("accessToken").asText();
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private <T> List<T> runConcurrent(int concurrency, IntFunction<T> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>(concurrency);
        try {
            for (int index = 0; index < concurrency; index++) {
                int taskIndex = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent test start timed out");
                    }
                    return action.apply(taskIndex);
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Concurrent workers did not become ready");
            start.countDown();
            List<T> results = new ArrayList<>(concurrency);
            for (Future<T> future : futures) {
                results.add(future.get(90, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Concurrent workers did not stop");
        }
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Registration(String username, String token) {
    }

    private record OrderResponse(int status, String orderId, String counterpartyUsername) {
    }

    private record ReadResponse(boolean successful, String orderStatus, String vipLevel, String vipPoints) {
    }
}
