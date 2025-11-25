package com.hospital.messaging;

import com.hospital.config.RabbitMQConfig;
import com.hospital.dto.ConversationMessageEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 会话消息事件发布者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig rabbitMQConfig;

    public void publish(ConversationMessageEventDTO event) {
        try {
            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getConversationExchange(),
                    rabbitMQConfig.getConversationRoutingKey(),
                    event
            );
            log.info("已发布会话消息事件：conversationId={}, messageId={}",
                    event.getConversationId(), event.getMessageId());
        } catch (Exception e) {
            log.error("发布会话消息事件失败：conversationId={}, messageId={}, error={}",
                    event.getConversationId(), event.getMessageId(), e.getMessage(), e);
        }
    }
}


