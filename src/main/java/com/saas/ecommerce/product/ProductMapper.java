package com.saas.ecommerce.product;

import com.saas.ecommerce.product.dto.ProductRequest;
import com.saas.ecommerce.product.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    //Entity -->response
    public ProductResponse toResponse(Product product) {

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();

    }
    //Request --> Entity(For creation)
    public Product toEntity(ProductRequest request, String tenantId) {

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setTenantId(tenantId);
        return product;
    }
    //Request --> existing Entity(for update)
    public void updateEntity(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());






    }












}
