package com.saas.ecommerce.product;

import com.saas.ecommerce.product.dto.ProductRequest;
import com.saas.ecommerce.product.dto.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor // fixed
public class ProductController {

    private final ProductService productService;

    // GET all products with pagination + sorting
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE_OWNER')") // added
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<ProductResponse> results = productService
                .getAllProducts(page, size, sortBy, sortDir);
        return ResponseEntity.ok(buildPageResponse(results));
    }

    // GET search products
    @GetMapping("/search")  //added
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE_OWNER')")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ProductResponse> results = productService
                .searchProducts(q, page, size);
        return ResponseEntity.ok(buildPageResponse(results));
    }

    // GET single product
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE_OWNER')") // added
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable String id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    // POST create product
    @PostMapping
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    // PUT update product
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(
                productService.updateProduct(id, request));
    }

    // DELETE soft delete product
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helper -------------------------------------------------------------------
    private Map<String, Object> buildPageResponse(
            Page<ProductResponse> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("items", page.getContent());
        response.put("page", page.getNumber());
        response.put("size", page.getSize());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("hasNext", page.hasNext());       // ✅
        response.put("hasPrevious", page.hasPrevious()); // ✅
        return response;
    }
}