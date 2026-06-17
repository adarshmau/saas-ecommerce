package com.saas.ecommerce.analytics;


import com.saas.ecommerce.analytics.dto.OrderStatsResponse;
import com.saas.ecommerce.analytics.dto.RevenueResponse;
import com.saas.ecommerce.analytics.dto.TopProductResponse;
import com.saas.ecommerce.order.OrderItemRepository;
import com.saas.ecommerce.order.OrderRepository;
import com.saas.ecommerce.order.OrderStatus;
import com.saas.ecommerce.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    //----------------revenue
    @Transactional(readOnly = true)
    public RevenueResponse getRevenue()
    {
        String tenantId = TenantContext.getTenantId();

        BigDecimal totalRevenue = orderRepository.findTotalRevenueByTenantId(tenantId);
        BigDecimal todayRevenue = orderRepository.findTodayRevenueByTenantId(tenantId);
        BigDecimal thisMonthRevenue = orderRepository.findThisMonthRevenueByTenantId(tenantId);
        Long totalOrders = orderRepository.countByTenantId(tenantId);
        Long completedOrders = orderRepository.countByTenantIdAndStatus(tenantId, OrderStatus.DELIVERED);
        log.info("Revenue stats fetched for tenant: {}", tenantId);

        return new RevenueResponse(
                totalRevenue,
                todayRevenue,
                thisMonthRevenue,
                totalOrders,
                completedOrders);

    }

    //─── ORDER STATS------------------------------------------
    public OrderStatsResponse getOrderStats() {
        String tenantId = TenantContext.getTenantId();
        Long total = orderRepository.countByTenantId(tenantId);
        Long pending = orderRepository.countByTenantIdAndStatus(
                tenantId, OrderStatus.PENDING);
        Long confirmed = orderRepository.countByTenantIdAndStatus(
                tenantId, OrderStatus.CONFIRMED);
        Long shipped = orderRepository.countByTenantIdAndStatus(
                tenantId, OrderStatus.SHIPPED);
        Long Delivered = orderRepository.countByTenantIdAndStatus(
                tenantId, OrderStatus.DELIVERED);
        Long cancelled = orderRepository.countByTenantIdAndStatus(
                tenantId, OrderStatus.CANCELLED);

        log.info("Order stats fetched for tenant: {}", tenantId);
        return new OrderStatsResponse(total, pending, confirmed, shipped,Delivered, cancelled);
    }
    // TOP PRODUCTS-------------------------------------------------------------
    public List<TopProductResponse> getTopProducts() {

            String tenantId = TenantContext.getTenantId();
            log.info("Top products fetched for tenant: {}", tenantId);

            return orderItemRepository.findTopProductsByTenantId(
                    tenantId, PageRequest.of(0, 10));
    }
}





