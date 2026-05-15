package com.saas.ecommerce.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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



}
