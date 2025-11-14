package com.ordermanagement.order_processing_service.service;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PriceAggregationService {

    private double totalSum = 0.0;
    private long totalCount = 0;

    public synchronized void addNewPrice(float price) {
        this.totalSum += price;
        this.totalCount++;
    }

    public synchronized double getRunningAverage() {
        if (totalCount == 0) {
            return 0.0;
        }
        return totalSum / totalCount;
    }

    public synchronized Map<String, Object> getAggregationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalCount);
        stats.put("totalValue", String.format("%.2f", totalSum));
        stats.put("runningAveragePrice", String.format("%.2f", getRunningAverage()));
        return stats;
    }
}