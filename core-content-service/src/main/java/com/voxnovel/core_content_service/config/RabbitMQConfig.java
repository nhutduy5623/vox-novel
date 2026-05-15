package com.voxnovel.core_content_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue.request}")
    private String requestQueue;

    @Value("${rabbitmq.routing.request}")
    private String requestRouting;

    // 1. Khai báo Exchange (Nhà ga trung chuyển)
    @Bean
    public TopicExchange aiExchange() {
        return new TopicExchange(exchange);
    }

    // 2. Khai báo Queue (Thùng chứa tin nhắn chờ AI đến lấy)
    @Bean
    public Queue requestQueue() {
        return new Queue(requestQueue, true); // true = bền vững, server restart không mất queue
    }

    // 3. Thiết lập Binding (Nối Queue vào Exchange bằng một cái "mác" - Routing Key)
    @Bean
    public Binding bindingRequest(Queue requestQueue, TopicExchange aiExchange) {
        return BindingBuilder.bind(requestQueue).to(aiExchange).with(requestRouting);
    }

    // 4. Quan trọng: Giúp Spring tự động chuyển Object sang JSON để gửi đi
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
