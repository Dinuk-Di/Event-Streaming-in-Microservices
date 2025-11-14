package com.ordermanagement.order_processing_service.controller;
import com.ordermanagement.order_processing_service.service.PriceAggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;


@RestController
@RequestMapping("/aggregation")
public class AggregationController {

    @Autowired
    private PriceAggregationService priceAggregationService;

    @GetMapping("/stats")
    public Map<String, Object> getAggregationStats() {
        return priceAggregationService.getAggregationStats();
    }
}