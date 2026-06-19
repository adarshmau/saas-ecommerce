package com.saas.ecommerce.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@RequestBody Map<String,String> request){

        String token = authService.register(
                request.get("name"),
                request.get("email"),
                request.get("password"),
                request.get("tenantId"),
                request.get("role")
        );
        return ResponseEntity.ok(Map.of("token",token));

    }
    @PostMapping("/login")
    public ResponseEntity<Map<String,String>> login(@RequestBody Map<String,String>  request){
        String token= authService.login(
                request.get("email"),
                request.get("password")
        );
        return ResponseEntity.ok(Map.of("token",token));

    }
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authHeader) {

        // Extract token from "Bearer eyJhbGci..."
        String token = authHeader.substring(7);

        // Get remaining expiry time
        long expirySeconds = jwtService.getExpirySeconds(token);

        // Store in Redis — token is now blacklisted
        tokenBlacklistService.blacklist(token, expirySeconds);

        log.info("User logged out — token blacklisted");

        return ResponseEntity.ok(
                Map.of("message", "Logged out successfully"));
    }



}
