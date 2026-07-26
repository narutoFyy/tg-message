package com.cardnova.giftchat.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class TencentMessageMirrorDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TencentMessageMirrorDispatcher.class);

    private final TencentMessageMirrorService tencentMessageMirrorService;
    private final ThreadPoolTaskExecutor executor;

    public TencentMessageMirrorDispatcher(TencentMessageMirrorService tencentMessageMirrorService) {
        this.tencentMessageMirrorService = tencentMessageMirrorService;
        this.executor = new ThreadPoolTaskExecutor();
        this.executor.setCorePoolSize(2);
        this.executor.setMaxPoolSize(2);
        this.executor.setQueueCapacity(1_000);
        this.executor.setThreadNamePrefix("tencent-mirror-");
        this.executor.setWaitForTasksToCompleteOnShutdown(true);
        this.executor.setAwaitTerminationSeconds(10);
        this.executor.initialize();
    }

    public void dispatchSupportMessage(String messageId) {
        dispatch(() -> tencentMessageMirrorService.mirrorSupportMessage(messageId), "support", messageId);
    }

    public void dispatchDirectMessage(String messageId) {
        dispatch(() -> tencentMessageMirrorService.mirrorDirectMessage(messageId), "direct", messageId);
    }

    private void dispatch(Runnable task, String messageKind, String messageId) {
        try {
            executor.execute(task);
        } catch (TaskRejectedException exception) {
            // The message remains PENDING and can be picked up by the existing IM retry job.
            LOGGER.warn("Tencent {} message mirror queue rejected message {}", messageKind, messageId, exception);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
