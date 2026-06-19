package com.saas.ecommerce.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

// BLACKLIST_PREFIX = "blacklist:"
//        → Redis is a shared store — prefix namespaces our keys
//→ Key becomes: "blacklist:eyJhbGci..."
//        → Avoids collision with other keys in Redis
//redisTemplate.opsForValue().set(key, value, ttl, unit)
//→ opsForValue() → for simple String operations
//→ set() with TTL → stores AND sets auto-expiry in one call
//→ After expirySeconds → Redis automatically deletes the key

//Boolean.TRUE.equals(redisTemplate.hasKey(...))
//        → hasKey() returns Boolean (nullable)
//        → Boolean.TRUE.equals() safely handles null
//        → Returns true only if key EXISTS in Redis
//→ If key doesn't exist → token is not blacklisted → allow
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    // Prefix to namespace our keys in Redis
    private static final String BLACKLIST_PREFIX = "blacklist:";

    // Add token to blacklist when user logs out
    public void blacklist(String token, long expirySeconds) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,  // key
                "blacklisted",             // value
                expirySeconds,             // TTL amount
                TimeUnit.SECONDS           // TTL unit
        );
        log.info("Token blacklisted for {} seconds", expirySeconds);
    }

    // Check if token is blacklisted on every request
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}