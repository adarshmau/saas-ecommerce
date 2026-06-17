package com.saas.ecommerce.analytics;

import com.saas.ecommerce.analytics.dto.OrderStatsResponse;
import com.saas.ecommerce.analytics.dto.RevenueResponse;
import com.saas.ecommerce.analytics.dto.TopProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;



    @GetMapping("/revenue")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<RevenueResponse> getRevenue() {
        return ResponseEntity.ok(analyticsService.getRevenue());
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<OrderStatsResponse> getOrderStats() {
        return ResponseEntity.ok(
                analyticsService.getOrderStats());
    }

    @GetMapping("/products")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<List<TopProductResponse>> getTopProducts() {
        return ResponseEntity.ok(
                analyticsService.getTopProducts());
    }


}
