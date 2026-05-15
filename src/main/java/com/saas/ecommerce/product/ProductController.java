package com.saas.ecommerce.product;

import com.saas.ecommerce.product.dto.ProductRequest;
import com.saas.ecommerce.product.dto.ProductResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/products")
@AllArgsConstructor
public class ProductController {


    private final ProductService productService;

    @GetMapping()
    public ResponseEntity<List<ProductResponse>> getAllProduct() {
        return  ResponseEntity.ok(productService.getAllProducts());

    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PostMapping()
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<ProductResponse> createProduct( @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));

    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public  ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id,request));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id){
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();

    }


    }



