package com.ordermanagement.order_processing_service.consumer;

import com.ordermanagement.dto.Order;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import com.ordermanagement.order_processing_service.exception.NonRetryableException;
import com.ordermanagement.order_processing_service.service.PriceAggregationService;

@Service
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    private final PriceAggregationService priceAggregationService;

    public OrderConsumer(PriceAggregationService priceAggregationService) {
        this.priceAggregationService = priceAggregationService;
    }

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        exclude = { NonRetryableException.class },
        dltTopicSuffix = "-dlq"
    )
    @KafkaListener(topics = "${app.topics.orders}", groupId = "order-processor-group")
    public void handleOrder(Order order, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received order: {}", order.getOrderId());

        try {
            // 1. Simulate a temporary failure (triggers RETRY) 
            if ("fail_temp".equalsIgnoreCase(order.getProduct())) {
                log.warn("Simulating temporary failure for order: {}", order.getOrderId());
                throw new RuntimeException("Temporary processing error!");
            }

            // 2. Simulate a permanent failure (triggers immediate DLQ) 
            if ("fail_perm".equalsIgnoreCase(order.getProduct())) {
                log.warn("Simulating permanent failure for order: {}", order.getOrderId());
                throw new NonRetryableException("Permanent/bad data error!");
            }

            // Happy Path: Process the order and update aggregation 
            log.info("Processing order {} for product {} with price {}", 
                     order.getOrderId(), order.getProduct(), order.getPrice());
            
            priceAggregationService.addNewPrice(order.getPrice());

        } catch (Exception e) {
            log.error("Failed to process order {}: {}", order.getOrderId(), e.getMessage());
            throw e;
        }
    }

    @KafkaListener(topics = "${kafka.topic.orders.dlq}", 
                   groupId = "dlq-handler-group")
    public void handleDltMessage(ConsumerRecord<String, String> record) {
        log.error("--- !!! DLQ MESSAGE RECEIVED !!! ---");
        log.error("Failed message on topic: {}", record.topic());
        log.error("Partition: {}, Offset: {}", record.partition(), record.offset());
        log.error("Message Key: {}", record.key());
        log.error("Message Value: {}", record.value());

        record.headers().forEach(header -> {
            log.error("Header - Key: {}, Value: {}", 
                     header.key(), new String(header.value()));
        });
    }
}