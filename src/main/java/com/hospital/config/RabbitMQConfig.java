package com.hospital.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 *
 * 负责声明医生接诊完成后提醒患者进行体质测试所需的交换机、队列以及路由键。
 */
@Configuration
public class RabbitMQConfig {

    @Value("${hospital.rabbitmq.consultation.exchange:hospital.consultation.exchange}")
    private String consultationExchange;

    @Value("${hospital.rabbitmq.consultation.queue:hospital.consultation.completed.queue}")
    private String consultationQueue;

    @Value("${hospital.rabbitmq.consultation.routing-key:hospital.consultation.completed}")
    private String consultationRoutingKey;

    @Value("${hospital.rabbitmq.conversation.exchange:hospital.conversation.exchange}")
    private String conversationExchange;

    @Value("${hospital.rabbitmq.conversation.queue:hospital.conversation.message.queue}")
    private String conversationQueue;

    @Value("${hospital.rabbitmq.conversation.routing-key:hospital.conversation.message}")
    private String conversationRoutingKey;

    private final ObjectProvider<com.fasterxml.jackson.databind.ObjectMapper> objectMapperProvider;

    public RabbitMQConfig(ObjectProvider<com.fasterxml.jackson.databind.ObjectMapper> objectMapperProvider) {
        this.objectMapperProvider = objectMapperProvider;
    }

    @Bean
    public DirectExchange consultationDirectExchange() {
        return new DirectExchange(consultationExchange, true, false);
    }

    @Bean
    public Queue consultationCompletedQueue() {
        return new Queue(consultationQueue, true);
    }

    @Bean
    public DirectExchange conversationDirectExchange() {
        return new DirectExchange(conversationExchange, true, false);
    }

    @Bean
    public Queue conversationMessageQueue() {
        return new Queue(conversationQueue, true);
    }

    @Bean
    public Binding consultationCompletedBinding() {
        return BindingBuilder
                .bind(consultationCompletedQueue())
                .to(consultationDirectExchange())
                .with(consultationRoutingKey);
    }

    @Bean
    public Binding conversationMessageBinding() {
        return BindingBuilder
                .bind(conversationMessageQueue())
                .to(conversationDirectExchange())
                .with(conversationRoutingKey);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                objectMapperProvider.getIfAvailable(com.fasterxml.jackson.databind.ObjectMapper::new);
        return new Jackson2JsonMessageConverter(mapper.copy());
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

    public String getConsultationExchange() {
        return consultationExchange;
    }

    public String getConsultationQueue() {
        return consultationQueue;
    }

    public String getConsultationRoutingKey() {
        return consultationRoutingKey;
    }

    public String getConversationExchange() {
        return conversationExchange;
    }

    public String getConversationQueue() {
        return conversationQueue;
    }

    public String getConversationRoutingKey() {
        return conversationRoutingKey;
    }
}

