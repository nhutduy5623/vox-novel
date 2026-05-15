package com.voxnovel.core_content_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    // --- REQUEST (ĐI) ---
    @Value("${rabbitmq.queue.request}")
    private String requestQueue;

    @Value("${rabbitmq.routing.request}")
    private String requestRouting;

    // --- RESPONSE (VỀ) --- CẦN THÊM 2 BIẾN NÀY
    @Value("${rabbitmq.queue.response}")
    private String responseQueue;

    @Value("${rabbitmq.routing.response}")
    private String responseRouting;

    // 1. Khai báo Exchange (Nhà ga trung chuyển) - ĐÃ CHUẨN DIRECT
    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(exchange);
    }

    // 2. Khai báo Queue ĐI
    @Bean
    public Queue requestQueue() {
        return new Queue(requestQueue, true);
    }

    // 3. Khai báo Queue VỀ (CẦN THÊM BEAN NÀY)
    @Bean
    public Queue responseQueue() {
        return new Queue(responseQueue, true);
    }

    // 4. Thiết lập Binding cho Queue ĐI
    @Bean
    public Binding requestBinding(Queue requestQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(requestQueue).to(aiExchange).with(requestRouting);
    }

    // 5. Thiết lập Binding cho Queue VỀ
    @Bean
    public Binding responseBinding(Queue responseQueue, DirectExchange aiExchange) {
        // LƯU Ý: Phải dùng responseRouting ở đây nhé!
        return BindingBuilder.bind(responseQueue).to(aiExchange).with(responseRouting);
    }

    // 6. Quan trọng: Giúp Spring tự động chuyển Object sang JSON để gửi đi
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
