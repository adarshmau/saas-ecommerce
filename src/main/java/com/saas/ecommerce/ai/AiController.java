package com.saas.ecommerce.ai;

import com.saas.ecommerce.ai.dto.AiRequest;
import com.saas.ecommerce.ai.dto.AiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // Generate AI product description
    @PostMapping("/description")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<AiResponse> generateDescription(
            @Valid @RequestBody AiRequest request) {
        log.info("AI description requested for: {}",
                request.getProductName());
        return ResponseEntity.ok(
                aiService.generateProductDescription(request));
    }
}