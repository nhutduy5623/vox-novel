package com.voxnovel.core_content_service.rabbitmq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiScriptProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing.request}")
    private String routingKey;

    public void sendGenerateScriptRequest(Long chapterId) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, chapterId);
            log.info("Producer đã đẩy request AI cho Chapter [{}] vào queue", chapterId);
        } catch (Exception e) {
            log.error("Lỗi khi gửi message vào RabbitMQ cho Chapter [{}]: ", chapterId, e);
            // Có thể ném ra Custom Exception để Service bắt và xử lý (VD: không đổi trạng thái nữa)
            throw new RuntimeException("Không thể kết nối tới Message Broker");
        }
    }
}