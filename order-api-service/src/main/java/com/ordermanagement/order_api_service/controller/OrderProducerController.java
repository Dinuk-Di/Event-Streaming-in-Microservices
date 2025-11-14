package com.ordermanagement.order_api_service.controller;

import com.ordermanagement.dto.Order;
import com.ordermanagement.order_api_service.producer.OrderProducer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderProducerController {

    @Autowired
    private OrderProducer orderProducer;

    @PostMapping("/orders")
    public String sendMessage(@RequestBody Order order) {
        orderProducer.sendMessage(order);
        return "Order sent successfully with ID: " + order;
    }
}