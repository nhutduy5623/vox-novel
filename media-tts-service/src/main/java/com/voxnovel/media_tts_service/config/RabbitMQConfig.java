package com.voxnovel.media_tts_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.media.exchange}")
    private String exchange;

    @Value("${rabbitmq.media.queue.tts-request}")
    private String ttsRequestQueue;

    @Value("${rabbitmq.media.routing.tts-request}")
    private String ttsRequestRouting;

    @Value("${rabbitmq.media.queue.tts-response}")
    private String ttsResponseQueue;

    @Value("${rabbitmq.media.routing.tts-response}")
    private String ttsResponseRouting;

    @Value("${rabbitmq.media.queue.merge-request}")
    private String mergeRequestQueue;

    @Value("${rabbitmq.media.routing.merge-request}")
    private String mergeRequestRouting;

    @Value("${rabbitmq.media.queue.merge-response}")
    private String mergeResponseQueue;

    @Value("${rabbitmq.media.routing.merge-response}")
    private String mergeResponseRouting;

    // 1. Khai báo trạm trung chuyển
    @Bean
    public DirectExchange mediaExchange() {
        return new DirectExchange(exchange);
    }

    // 2. Hòm thư ĐI (Media nhận lệnh từ đây)
    @Bean
    public Queue ttsRequestQueue() {
        return new Queue(ttsRequestQueue, true);
    }

    // 3. Hòm thư VỀ (Media gửi kết quả vào đây)
    @Bean
    public Queue ttsResponseQueue() {
        return new Queue(ttsResponseQueue, true);
    }

    @Bean
    public Queue mergeRequestQueue() {
        return new Queue(mergeRequestQueue, true);
    }

    @Bean
    public Queue mergeResponseQueue() {
        return new Queue(mergeResponseQueue, true);
    }

    // 4. Móc nối hòm thư ĐI với Trạm
    @Bean
    public Binding ttsRequestBinding(Queue ttsRequestQueue, DirectExchange mediaExchange) {
        return BindingBuilder.bind(ttsRequestQueue).to(mediaExchange).with(ttsRequestRouting);
    }

    // 5. Móc nối hòm thư VỀ với Trạm
    @Bean
    public Binding ttsResponseBinding(Queue ttsResponseQueue, DirectExchange mediaExchange) {
        return BindingBuilder.bind(ttsResponseQueue).to(mediaExchange).with(ttsResponseRouting);
    }

    @Bean
    public Binding mergeRequestBinding(Queue mergeRequestQueue, DirectExchange mediaExchange) {
        return BindingBuilder.bind(mergeRequestQueue).to(mediaExchange).with(mergeRequestRouting);
    }

    @Bean
    public Binding mergeResponseBinding(Queue mergeResponseQueue, DirectExchange mediaExchange) {
        return BindingBuilder.bind(mergeResponseQueue).to(mediaExchange).with(mergeResponseRouting);
    }

    // 6. TUYỆT ĐỐI KHÔNG ĐƯỢC QUÊN: Bộ chuyển đổi Object <-> JSON
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
