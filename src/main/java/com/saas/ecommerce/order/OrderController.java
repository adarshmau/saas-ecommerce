package com.saas.ecommerce.order;


import com.saas.ecommerce.order.dto.OrderRequest;
import com.saas.ecommerce.order.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/orders")
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
    public ResponseEntity<List<OrderResponse>> getMyOrders(){

        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','STORE_OWNER')")
    public ResponseEntity<OrderResponse> getOrders(@PathVariable String id){

        return ResponseEntity.ok(orderService.getOrder(id));
    }
    @GetMapping
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<List<OrderResponse>> getAllOrder(){
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String id,
            @RequestParam OrderStatus status){
        return ResponseEntity.ok(orderService.updateStatus(id, status));


    }



}
