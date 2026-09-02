package com.saas.ecommerce.ai;

import com.saas.ecommerce.ai.dto.AiRequest;
import com.saas.ecommerce.ai.dto.AiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ─── GENERATE PRODUCT DESCRIPTION ────────────────────────

    public AiResponse generateProductDescription(AiRequest request) {

        // 1 — Sanitize input
        String productName = sanitize(request.getProductName());
        String category = request.getCategory() != null
                ? sanitize(request.getCategory()) : "general";
        String audience = request.getTargetAudience() != null
                ? sanitize(request.getTargetAudience())
                : "general customers";

        // 2 — Build prompt
        String prompt = buildProductPrompt(
                productName, category, audience);

        // 3 — Call Gemini
        String description = callGemini(prompt, productName);

        return new AiResponse(productName, description, true);
    }

    // ─── GENERATE ANALYTICS SUMMARY ──────────────────────────

    public AiResponse generateAnalyticsSummary(
            long totalOrders,
            long deliveredOrders,
            long cancelledOrders,
            double totalRevenue,
            String topProduct) {

        String prompt = buildAnalyticsPrompt(
                totalOrders, deliveredOrders,
                cancelledOrders, totalRevenue, topProduct);

        String summary = callGemini(prompt, "analytics");

        return new AiResponse("Analytics Summary", summary, true);
    }

    // ─── CALL GEMINI API ──────────────────────────────────────

    private String callGemini(String prompt, String context) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.5,
                            "maxOutputTokens", 200
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            String url = apiUrl + "?key=" + apiKey;

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return extractText(response.getBody());

        } catch (Exception e) {
            log.error("Gemini API call failed for {}: {}",
                    context, e.getMessage());
            return getFallback(context);
        }
    }

    // ─── EXTRACT TEXT FROM RESPONSE ──────────────────────────

    @SuppressWarnings("unchecked")
    private String extractText(Map responseBody) {
        try {
            List<Map> candidates = (List<Map>)
                    responseBody.get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            return parts.get(0).get("text").toString().trim();
        } catch (Exception e) {
            log.error("Failed to extract Gemini response: {}",
                    e.getMessage());
            return "AI description temporarily unavailable.";
        }
    }

    // ─── BUILD PROMPTS ────────────────────────────────────────

    private String buildProductPrompt(String productName,
                                      String category,
                                      String audience) {
        return """
                You are a professional ecommerce copywriter.
                Write a compelling product description for the
                following product.

                Product Name: %s
                Category: %s
                Target Audience: %s

                Instructions:
                - Write 2-3 sentences only
                - Focus on key benefits not just features
                - Keep it engaging and professional
                - Do not include price or availability
                - Return only the description, no extra text
                """.formatted(productName, category, audience);
    }

    private String buildAnalyticsPrompt(long totalOrders,
                                        long delivered,
                                        long cancelled,
                                        double revenue,
                                        String topProduct) {
        return """
                You are a business analyst.
                Analyze this ecommerce store data and give
                a 2-3 sentence plain English summary with
                one actionable insight.

                Total Orders: %d
                Delivered Orders: %d
                Cancelled Orders: %d
                Total Revenue: ₹%.2f
                Top Selling Product: %s

                Return only the summary, no extra text.
                """.formatted(totalOrders, delivered,
                cancelled, revenue, topProduct);
    }

    // ─── SANITIZE (prevent prompt injection) ─────────────────

    private String sanitize(String input) {
        if (input == null) return "";
        String cleaned = input.trim()
                .replaceAll("[<>\"'{}]", "");
        return cleaned.substring(
                0, Math.min(cleaned.length(), 100));
    }

    // ─── FALLBACK ─────────────────────────────────────────────

    private String getFallback(String context) {
        if (context.equals("analytics")) {
            return "Analytics summary temporarily unavailable. " +
                    "Please check the numbers directly.";
        }
        return "AI description temporarily unavailable. " +
                "Please add description manually.";
    }
}