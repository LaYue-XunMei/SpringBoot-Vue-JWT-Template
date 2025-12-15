package com.example.config;

import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitConfiguration {
    /**
     * RabbitMQ消息队列配置
     */

    @Bean("emailQueue")
    public Queue emailQueue() {
        return QueueBuilder
                .durable("mail")
                .build();
    }

    // 配置 JSON 消息转换器
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
