package com.saas.ecommerce.product;

import com.saas.ecommerce.common.exception.ResourceNotFoundException;
import com.saas.ecommerce.product.dto.ProductRequest;
import com.saas.ecommerce.product.dto.ProductResponse;
import com.saas.ecommerce.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductResponse> getAllProducts() {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching all products for tenant: {}", tenantId);
        return productRepository.findByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public ProductResponse getProduct(String id) {
        String tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating product for tenant: {}", tenantId);
        Product saved = productRepository.save(productMapper.toEntity(request, tenantId));
        productRepository.flush();
        return productMapper.toResponse(productRepository.findById(saved.getId()).get());
    }

    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest request) {
        String tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productMapper.updateEntity(product, request);
        Product saved = productRepository.save(product);
        productRepository.flush();
        return productMapper.toResponse(productRepository.findById(saved.getId()).get());
    }

    @Transactional
    public void deleteProduct(String id) {
        String tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Soft deleted product: {} for tenant: {}", id, tenantId);
    }
}  // ✅ only one closing brace at the end