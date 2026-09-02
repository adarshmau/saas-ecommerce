package com.saas.ecommerce.payment;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    // ─── CREATE RAZORPAY ORDER ────────────────────────────────

    public String createRazorpayOrder(BigDecimal amount,
                                      String receipt) {
        try {
            RazorpayClient client = new RazorpayClient(
                    keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            // Razorpay needs amount in paise (1 INR = 100 paise)
            orderRequest.put("amount",
                    amount.multiply(BigDecimal.valueOf(100))
                            .intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);

            Order order = client.orders.create(orderRequest);
            log.info("Razorpay order created: {}",
                    (String) order.get("id"));
            return (String) order.get("id");

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}",
                    e.getMessage());
            throw new RuntimeException(
                    "Payment initiation failed: " + e.getMessage());
        }
    }

    // ─── VERIFY PAYMENT SIGNATURE ─────────────────────────────

    public boolean verifySignature(String razorpayOrderId,
                                   String razorpayPaymentId,
                                   String razorpaySignature) {
        try {
            // Razorpay signature = HMAC SHA256 of
            // "razorpayOrderId|razorpayPaymentId"
            String data = razorpayOrderId + "|" + razorpayPaymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            boolean valid = hexString.toString()
                    .equals(razorpaySignature);

            log.info("Payment signature verification: {}",
                    valid ? "VALID" : "INVALID");
            return valid;

        } catch (Exception e) {
            log.error("Signature verification failed: {}",
                    e.getMessage());
            return false;
        }
    }
    // ─── TEST HELPER — generates valid test signature ─────────────
// ✅ REMOVE BEFORE PRODUCTION
    public Map<String, String> generateTestPaymentData(
            String razorpayOrderId) {
        try {
            String fakePaymentId = "pay_TEST"
                    + System.currentTimeMillis();

            String data = razorpayOrderId + "|" + fakePaymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return Map.of(
                    "razorpayOrderId", razorpayOrderId,
                    "razorpayPaymentId", fakePaymentId,
                    "razorpaySignature", hexString.toString()
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Test data generation failed: " + e.getMessage());
        }
    }
}