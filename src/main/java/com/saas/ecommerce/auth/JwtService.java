package com.saas.ecommerce.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${application.jwt.secret}")
    private String secretKey;

    @Value("${application.jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey(){

        return Keys.hmacShaKeyFor(secretKey.getBytes());
    };
    public String generateToken(String email,String role,String tenantId){
        return Jwts.builder()
                .subject(email)
                .claim("role",role)
                .claim("tenantId",tenantId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+expiration))
                .signWith(getSigningKey())
                .compact();
    }
//Claims is basically a Map containing all the data — email, role, tenantId, expiry etc.
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    //Checks two things:
    //Is the signature valid? (if tampered → exception → returns false)
    //Is the expiry date after right now? (if expired → returns false)
    public Boolean isTokenValid(String token) {
        try
        {
            return extractAllClaims(token)
                    .getExpiration()
                    .after(new Date());
        } catch (Exception e) {
            return false;
        }

    }


}
