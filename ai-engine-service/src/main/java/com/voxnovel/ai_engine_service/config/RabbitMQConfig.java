package com.voxnovel.ai_engine_service.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String AI_EXCHANGE = "ai.exchange";

    // Hàng đợi NHẬN LỆNH từ Core Content
    public static final String REQUEST_QUEUE = "ai.script.generate.request.queue";
    public static final String REQUEST_ROUTING_KEY = "ai.script.request";

    // Hàng đợi TRẢ KẾT QUẢ về cho Core Content
    public static final String RESPONSE_QUEUE = "ai.script.generate.response.queue";
    public static final String RESPONSE_ROUTING_KEY = "ai.script.response";

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(AI_EXCHANGE);
    }

    @Bean
    public Queue requestQueue() {
        return new Queue(REQUEST_QUEUE, true);
    }

    @Bean
    public Queue responseQueue() {
        return new Queue(RESPONSE_QUEUE, true);
    }

    @Bean
    public Binding requestBinding(Queue requestQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(requestQueue).to(aiExchange).with(REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding responseBinding(Queue responseQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(responseQueue).to(aiExchange).with(RESPONSE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}