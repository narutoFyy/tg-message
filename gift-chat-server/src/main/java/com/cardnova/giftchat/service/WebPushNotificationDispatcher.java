package com.cardnova.giftchat.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class WebPushNotificationDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebPushNotificationDispatcher.class);

    private final WebPushDeliveryService deliveryService;
    private final ThreadPoolTaskExecutor executor;

    public WebPushNotificationDispatcher(WebPushDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
        this.executor = new ThreadPoolTaskExecutor();
        this.executor.setCorePoolSize(2);
        this.executor.setMaxPoolSize(4);
        this.executor.setQueueCapacity(2_000);
        this.executor.setThreadNamePrefix("web-push-");
        this.executor.setWaitForTasksToCompleteOnShutdown(true);
        this.executor.setAwaitTerminationSeconds(10);
        this.executor.initialize();
    }

    public void dispatchAfterCommit(String userId, String conversationId) {
        Runnable dispatch = () -> enqueue(userId, conversationId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatch.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatch.run();
            }
        });
    }

    private void enqueue(String userId, String conversationId) {
        try {
            executor.execute(() -> deliveryService.deliverSupportMessage(userId, conversationId));
        } catch (TaskRejectedException exception) {
            LOGGER.warn("Web Push queue rejected support conversation {}", conversationId, exception);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
