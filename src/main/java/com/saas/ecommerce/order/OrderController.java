package com.saas.ecommerce.order;


import com.saas.ecommerce.order.dto.OrderRequest;
import com.saas.ecommerce.order.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest orderRequest) {
        log.info("Create order request received");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(orderRequest));

    }
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String ,Object>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        Page<OrderResponse> results = orderService.getMyOrders(page, size);
        return ResponseEntity.ok(buildPageResponse(results));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','STORE_OWNER')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String id){

        return ResponseEntity.ok(orderService.getOrder(id));
    }
    @GetMapping
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<Map<String, Object>> getAllOrder(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OrderResponse> results = orderService.getAllOrders(page, size);
        return ResponseEntity.ok(buildPageResponse(results));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String id,
            @RequestParam OrderStatus status){
        return ResponseEntity.ok(orderService.updateStatus(id, status));


    }
    // Small private helper so we don't repeat this map-building logic twice
    private Map<String, Object> buildPageResponse(Page<OrderResponse> results) {

        Map<String, Object> resp = new HashMap<>();
        resp.put("items", results.getContent());
        resp.put("page", results.getNumber());
        resp.put("size", results.getSize());
        resp.put("totalElements", results.getTotalElements());
        resp.put("totalPages", results.getTotalPages());
        return resp;
    }




}
