package com.cardnova.giftchat.config;

import com.cardnova.giftchat.service.RealtimeRedisBridge;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableConfigurationProperties({RealtimeRedisProperties.class, TencentChatProperties.class})
public class RealtimeRedisConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.realtime.redis", name = "enabled", havingValue = "true")
    RedisMessageListenerContainer realtimeRedisListenerContainer(
        RedisConnectionFactory connectionFactory,
        RealtimeRedisBridge realtimeRedisBridge,
        RealtimeRedisProperties properties
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(realtimeRedisBridge, "handleMessage");
        listenerAdapter.setSerializer(new StringRedisSerializer());
        container.addMessageListener(listenerAdapter, new ChannelTopic(properties.channel()));
        return container;
    }
}
