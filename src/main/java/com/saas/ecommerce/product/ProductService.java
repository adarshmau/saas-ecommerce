package com.saas.ecommerce.product;

import com.saas.ecommerce.common.exception.ResourceNotFoundException;
import com.saas.ecommerce.product.dto.ProductRequest;
import com.saas.ecommerce.product.dto.ProductResponse;
import com.saas.ecommerce.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    // ─── GET ALL WITH PAGINATION---------------------------------------

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(int page, int size,
                                                String sortBy,
                                                String sortDir) {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching products for tenant: {}", tenantId);
        return productRepository
                .findByTenantIdAndActiveTrue(
                        tenantId,
                        buildPageable(page, size, sortBy, sortDir))
                .map(productMapper::toResponse);
    }

    // ─── SEARCH WITH PAGINATION -------------------------------------------

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String q, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        log.info("Searching products for tenant: {} q={}",
                tenantId, q);

        Pageable pageable = buildPageable(
                page, size, "createdAt", "desc");

        if (q == null || q.isBlank()) {
            return productRepository
                    .findByTenantIdAndActiveTrue(tenantId, pageable)
                    .map(productMapper::toResponse);
        }

        return productRepository
                .searchByTenantIdAndQuery(tenantId, q, pageable)
                .map(productMapper::toResponse);
    }

    // ─── GET SINGLE ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductResponse getProduct(String id) {
        String tenantId = TenantContext.getTenantId();
        Product product = productRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    // ─── CREATE ───────────────────────────────────────────────

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating product for tenant: {}", tenantId);

        // ✅ removed redundant flush + findById
        Product saved = productRepository.save(
                productMapper.toEntity(request, tenantId));
        return productMapper.toResponse(saved);
    }

    // ─── UPDATE ───────────────────────────────────────────────

    @Transactional
    public ProductResponse updateProduct(String id,
                                         ProductRequest request) {
        String tenantId = TenantContext.getTenantId();
        Product product = productRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        productMapper.updateEntity(product, request);

        // ✅ removed redundant flush + findById
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    // ─── DELETE (soft) ────────────────────────────────────────

    @Transactional
    public void deleteProduct(String id) {
        String tenantId = TenantContext.getTenantId();
        Product product = productRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        product.setActive(false);
        productRepository.save(product);

        log.info("Soft deleted product: {} for tenant: {}",
                id, tenantId);
    }

    // ─── HELPER ───────────────────────────────────────────────

    private Pageable buildPageable(int page, int size,
                                   String sortBy, String sortDir) {
        // ✅ Safe bounds
        if (size > 50) size = 50;
        if (size < 1) size = 10;
        if (page < 0) page = 0;

        // ✅ Whitelist allowed sort fields — security fix
        List<String> allowedSortFields = List.of(
                "name", "price", "createdAt", "stockQuantity");
        if (!allowedSortFields.contains(sortBy)) {
            sortBy = "createdAt"; // safe default
        }

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return PageRequest.of(page, size, sort);
    }
}