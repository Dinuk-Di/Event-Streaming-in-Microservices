package com.ordermanagement.price_aggregation_service.stream;
import com.ordermanagement.kafka.avro.Order;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Collections;
import java.util.Map;

// Helper class to store the aggregation state
record PriceAggregation(Long count, Double sum, Double average) {
    public PriceAggregation() {
        this(0L, 0.0, 0.0);
    }
}

@Configuration
public class PriceAggregationStream {

    @Value("${app.topics.orders}")
    private String ordersTopic;

    @Value("${app.topics.avg-prices}")
    private String avgPricesTopic;
    
    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;
    
    // Autowired by @EnableKafkaStreams
    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {

        // Manually configure the SpecificAvroSerde
        final Serde<Order> orderSerde = new SpecificAvroSerde<>();
        orderSerde.configure(Collections.singletonMap("schema.registry.url", schemaRegistryUrl), false);

        // SerDe for our state-holding class (PriceAggregation)
        final Serde<PriceAggregation> aggSerde = new JsonSerde<>(PriceAggregation.class);

        // 1. Stream from the 'orders' topic
        KStream<String, Order> orderStream = streamsBuilder
                .stream(ordersTopic, Consumed.with(Serdes.String(), orderSerde));

        // 2. Aggregate the price [cite: 5]
        KTable<String, Double> averagePriceTable = orderStream
                // Re-key everything to a single static key for a *global* average
                .selectKey((k, v) -> "global-average")
                .groupByKey(Grouped.with(Serdes.String(), orderSerde))
                .aggregate(
                        PriceAggregation::new, // Initializer
                        (key, newOrder, aggregate) -> { // Aggregator
                            long newCount = aggregate.count() + 1;
                            double newSum = aggregate.sum() + newOrder.getPrice();
                            double newAverage = newSum / newCount;
                            return new PriceAggregation(newCount, newSum, newAverage);
                        },
                        Materialized.<String, PriceAggregation, KeyValueStore<Bytes, byte[]>>as("price-agg-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(aggSerde)
                )
                // 3. Map to get just the final average
                .mapValues(PriceAggregation::average);

        // 4. Output the result to the 'average-prices' topic
        averagePriceTable.toStream()
                .to(avgPricesTopic, Produced.with(Serdes.String(), Serdes.Double()));
    }
}