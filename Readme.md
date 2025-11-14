# Kafka Order Processing System (Microservices)

This project is a distributed, event-driven system built to demonstrate key principles of Apache Kafka. It implements an order processing pipeline using a microservice architecture with Spring Boot.

The system handles producing, consuming, and processing order messages with Avro serialization, provides a simple **real-time aggregation within the consumer**, and implements robust fault-tolerance patterns like **Retry** and **Dead Letter Queues (DLQ)**.

-----

## 🏛️ System Architecture

The system is composed of **two independent microservices** that communicate via Kafka topics. A REST API (Producer) is the entry point, and a Consumer service reacts to the data.

### Data Flow

1.  A client (e.g., Postman) sends a JSON request to the `order-api-service`.
2.  `order-api-service` validates the request, serializes it into an **Avro** message, and produces it to the `orders` topic.
3.  The `order-processing-service` consumes this message and performs two actions:
      * **Business Logic:** It applies validation logic (e.g., checking for negative prices).
          * **Success:** Logs the processed order.
          * **Temporary Failure:** Uses Spring Kafka's built-in retry logic.
          * **Permanent Failure:** Routes the poison pill message to the `orders-dlq` topic.
      * **Aggregation:** If the order is successful, it updates an internal, in-memory `PriceAggregationService` with the new price.
4.  A client can query a new REST endpoint (`GET /aggregation/average`) on the `order-processing-service` to see the current running average at any time.

-----

## ✨ Features

This project fulfills all the core assignment requirements:

  * **Kafka Producer/Consumer:** A dedicated producer (`order-api-service`) and consumer (`order-processing-service`).
  * **Avro Serialization:** All messages on the `orders` topic are serialized using Avro and managed by the Confluent Schema Registry.
  * **Real-time Aggregation:** The `order-processing-service` maintains a stateful, in-memory running average of prices. This is exposed via a REST endpoint (`/aggregation/average`) for live demonstration.
  * **Retry Logic:** The `order-processing-service` uses Spring Kafka's `@RetryableTopic` annotation to automatically retry failed messages with exponential backoff.
  * **Dead Letter Queue (DLQ):** After all retries are exhausted, permanently failing messages are routed to a separate `orders-dlq` topic for manual inspection.

-----

## 🛠️ Technology Stack

  * **Backend:** Java 17 & Spring Boot
  * **Messaging:** Apache Kafka
  * **Serialization:** Apache Avro & Confluent Schema Registry
  * **Containerization:** Docker & Docker Compose
  * **Build:** Apache Maven

-----

## 📂 Project Structure

This repository is a mono-repo containing **two** separate Spring Boot applications.

  * `order-api-service/`: **(Producer)**
      * A simple Spring Web service.
      * Exposes a single POST endpoint: `/orders`.
      * Serializes requests into Avro `Order` messages and sends them to Kafka.
  * `order-processing-service/`: **(Consumer)**
      * A Spring Kafka application.
      * Listens to the `orders` topic with `@KafkaListener`.
      * Contains failure simulation logic and the `@RetryableTopic` configuration.
      * Includes a separate listener for the `orders-dlq` topic.
      * It also holds the `PriceAggregationService` and its REST endpoint (`/aggregation/average`) to show the running average.

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

The Java `Order.java` class must be generated from the `.avsc` schema file. You must run this in **each** of the two service directories.

```bash
# In /order-api-service
mvn generate-sources

# In /order-processing-service
mvn generate-sources
```

### 3\. Run the Microservices

You can run both services simultaneously from your IDE or by using the Maven Spring Boot plugin in separate terminal windows.

```bash
# Terminal 1: Start the API (Producer)
# (Port 8090)
cd order-api-service
mvn spring-boot:run

# Terminal 2: Start the Consumer
# (Port 8091)
cd order-processing-service
mvn spring-boot:run
```

-----

## 🧪 How to Demonstrate the System

You can test all features using `curl` or Postman.

### 1\. Send a Successful Order

This will be successfully processed and included in the price aggregation.

```bash
curl -X POST http://localhost:8090/orders \
-H "Content-Type: application/json" \
-d '{
    "orderId": "1001",
    "product": "Laptop",
    "price": 1200.0
}'
```

  * **Expected Result:**
      * `order-processing-service` log: "Successfully processed order 1001..."

### 2\. Test Retry Logic (Temporary Failure)

Send a product named "fail-temp" to trigger a `RuntimeException`.

```bash
curl -X POST http://localhost:8090/orders \
-H "Content-Type: application/json" \
-d '{
    "orderId": "1002",
    "product": "fail-temp",
    "price": 50.0
}'
```

  * **Expected Result:**
      * `order-processing-service` log: "Simulating temporary failure..." followed by 2 retries with 1s and 2s delays.
      * You will see the "Received order: 1002" log appear 3 times in total.

### 3\. Test DLQ (Permanent Failure)

Send a product named "fail-perm" to trigger a `NonRetryableException`.

```bash
curl -X POST http://localhost:8090/orders \
-H "Content-Type: application/json" \
-d '{
    "orderId": "1003",
    "product": "fail-perm",
    "price": -10.0
}'
```

  * **Expected Result:**
      * `order-processing-service` log: "Simulating permanent failure..."
      * The message will **not** be retried.
      * You will immediately see the log: **"--- \!\!\! DLQ MESSAGE RECEIVED \!\!\! ---"** from the `handleDltMessage` method.

### 4\. Check the Real-time Average

After sending successful orders, you can query the `order-processing-service`'s aggregation endpoint (on **port 8091**) to see the running average.

```bash
# Run this in a new terminal:
curl -X GET http://localhost:8091/aggregation/average
```

  * **Expected Result (after sending order 1001):**
    ```json
    {
      "totalOrders": 1,
      "totalValue": "1200.00",
      "runningAveragePrice": "1200.00"
    }
    ```
  * **If you then send a second successful order for `800.0`:**
    ```json
    {
      "totalOrders": 2,
      "totalValue": "2000.00",
      "runningAveragePrice": "1000.00"
    }
    ```