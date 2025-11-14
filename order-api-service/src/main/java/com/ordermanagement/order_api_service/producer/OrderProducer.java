package com.ordermanagement.order_api_service.producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.ordermanagement.dto.Order;


@Service
public class OrderProducer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProducer.class);
    
    @Value("${app.topics.orders}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    public void sendMessage(Order order) {
        LOGGER.info(String.format("Order event => %s", order.toString()));
        kafkaTemplate.send(topic,order);
    }
}
