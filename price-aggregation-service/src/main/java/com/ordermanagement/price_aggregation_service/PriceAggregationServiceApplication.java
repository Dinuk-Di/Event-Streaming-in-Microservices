package com.ordermanagement.price_aggregation_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@EnableKafkaStreams
@SpringBootApplication
public class PriceAggregationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PriceAggregationServiceApplication.class, args);
	}

}
