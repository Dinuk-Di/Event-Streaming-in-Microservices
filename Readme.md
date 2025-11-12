-----

# Kafka Order Processing System (Microservices)

This project is a distributed, event-driven system built to demonstrate key principles of Apache Kafka. It implements an order processing pipeline using a microservice architecture with Spring Boot.

The system handles producing, consuming, and processing order messages with Avro serialization, provides real-time aggregation using Kafka Streams, and implements robust fault-tolerance patterns like Retry and Dead Letter Queues (DLQ).

-----

## 🏛️ System Architecture

The system is composed of three independent microservices that communicate via Kafka topics. A REST API (Producer) is the entry point, while two separate services (a Consumer and a Stream Processor) react to the data in parallel.

### Data Flow

1.  A client (e.g., Postman) sends a JSON request to the `order-api-service`.
2.  `order-api-service` validates the request, serializes it into an **Avro** message, and produces it to the `orders` topic.
3.  This message is consumed independently by two services:
      * `order-processing-service`: Applies business logic (e.g., price validation).
          * **Success:** Logs the processed order.
          * **Temporary Failure:** Uses Spring Kafka's built-in retry logic.
          * **Permanent Failure:** Routes the poison pill message to the `orders-dlq` topic.
      * `price-aggregation-service`: Uses **Kafka Streams** to perform a real-time running average calculation of all order prices.
4.  The `price-aggregation-service` publishes its result (the new average) to the `average-prices` topic.

-----

## ✨ Features

This project fulfills all the core assignment requirements:

  * **Kafka Producer/Consumer:** A dedicated producer (`order-api-service`) and consumer (`order-processing-service`).
  * **Avro Serialization:** All messages on the `orders` topic are serialized using Avro and managed by the Confluent Schema Registry.
  * **Real-time Aggregation:** The `price-aggregation-service` uses Kafka Streams to compute a stateful, fault-tolerant running average of prices.
  * **Retry Logic:** The `order-processing-service` uses Spring Kafka's `@RetryableTopic` annotation to automatically retry failed messages with exponential backoff.
  * **Dead Letter Queue (DLQ):** After all retries are exhausted, permanently failing messages (e.g., negative prices) are routed to a separate `orders-dlq` topic for manual inspection.

-----

## 🛠️ Technology Stack

  * **Backend:** Java 17 & Spring Boot
  * **Messaging:** Apache Kafka
  * **Stream Processing:** Kafka Streams
  * **Serialization:** Apache Avro & Confluent Schema Registry
  * **Containerization:** Docker & Docker Compose
  * **Build:** Apache Maven

-----

## 📂 Project Structure

This repository is a mono-repo containing three separate Spring Boot applications.

  * `order-api-service/`: **(Producer)**
      * A simple Spring Web service.
      * Exposes a single POST endpoint: `/order`.
      * Serializes requests into Avro `Order` messages and sends them to Kafka.
  * `order-processing-service/`: **(Consumer)**
      * A Spring Kafka application.
      * Listens to the `orders` topic with `@KafkaListener`.
      * Contains failure simulation logic and the `@RetryableTopic` configuration.
      * Includes a separate listener for the `orders-dlq` topic.
  * `price-aggregation-service/`: **(Stream Processor)**
      * A Spring Kafka Streams application.
      * Builds a KStreams topology to read from `orders`, perform a `groupByKey().aggregate()`, and publish to `average-prices`.

-----

## 🚀 Getting Started

### Prerequisites

  * Java 17 (or newer)
  * Apache Maven
  * Docker & Docker Compose

### 1\. Start the Kafka Environment

First, start the necessary infrastructure (Kafka, Zookeeper, Schema Registry) using Docker Compose.

```bash
docker-compose up -d
```

### 2\. Build Avro Classes

The Java `Order.java` class must be generated from the `.avsc` schema file. You must run this in **each** of the three service directories.

```bash
# In /order-api-service
mvn generate-sources

# In /order-processing-service
mvn generate-sources

# In /price-aggregation-service
mvn generate-sources
```

### 3\. Run the Microservices

You can run all three services simultaneously from your IDE or by using the Maven Spring Boot plugin in separate terminal windows.

```bash
# Terminal 1: Start the API
cd order-api-service
mvn spring-boot:run

# Terminal 2: Start the Consumer
cd order-processing-service
mvn spring-boot:run

# Terminal 3: Start the Aggregator
cd price-aggregation-service
mvn spring-boot:run
```

-----

## 🧪 How to Demonstrate the System

You can test all features using `curl` or Postman.

### 1\. Send a Successful Order

This will be successfully processed and included in the price aggregation.

```bash
curl -X POST http://localhost:8080/order \
-H "Content-Type: application/json" \
-d '{
    "product": "Laptop",
    "price": 1200.0
}'
```

  * **Expected Result:**
      * `order-processing-service` log: "Successfully processed order..."
      * `price-aggregation-service` log: (Shows topology processing)

### 2\. Test Retry Logic (Temporary Failure)

Send a product named "fail-temp" to trigger a `RuntimeException`.

```bash
curl -X POST http://localhost:8080/order \
-H "Content-Type: application/json" \
-d '{
    "product": "fail-temp",
    "price": 50.0
}'
```

  * **Expected Result:**
      * `order-processing-service` log: "Simulating temporary failure..." followed by 2 retries with 1s and 2s delays. The message will eventually be processed successfully (as the code only fails once).

### 3\. Test DLQ (Permanent Failure)

Send an order with a negative price to trigger a permanent `IllegalArgumentException`.

```bash
curl -X POST http://localhost:8080/order \
-H "Content-Type: application/json" \
-d '{
    "product": "Bad Item",
    "price": -10.0
}'
```

  * **Expected Result:**
      * `order-processing-service` log: "Permanent failure\! Invalid price..."
      * After 3 failed attempts, you will see the log: **"\!\!\! DLQ RECEIVED \!\!\!"** from the `handleDlt` method.

### 4\. Check the Real-time Average

You can inspect the `average-prices` topic directly from the Kafka container to see the running average in real-time.

```bash
# Open a new terminal and run this command:
docker exec -it kafka /usr/bin/kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic average-prices \
    --from-beginning \
    --property print.key=true \
    --property key.separator=" | " \
    --property value.deserializer=org.apache.kafka.common.serialization.DoubleDeserializer
```

After sending the `1200.0` order, you will see:
`global-average | 1200.0`

If you then send a second order for `800.0`, you will see a new message appear:
`global-average | 1000.0`