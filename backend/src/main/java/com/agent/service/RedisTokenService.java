package com.agent.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis service for JWT token management and user session tracking.
 */
@Service
public class RedisTokenService {

    private static final String TOKEN_PREFIX = "token:";
    private static final String USER_SESSION_PREFIX = "user_session:";

    private static final long TOKEN_TTL = 24; // hours (matches JWT expiration)

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Save token when user logs in.
     */
    public void saveUserToken(String username, String token, String role) {
        // Save token -> username mapping
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token, username, TOKEN_TTL, TimeUnit.HOURS);
        // Save user session tracking
        redisTemplate.opsForValue().set(USER_SESSION_PREFIX + username, token, TOKEN_TTL, TimeUnit.HOURS);
    }

    /**
     * Get username by token.
     */
    public String getUsernameByToken(String token) {
        Object val = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        return val != null ? val.toString() : null;
    }

    /**
     * Check if token is still valid (exists in Redis and not expired).
     */
    public boolean isTokenValid(String token) {
        Boolean exists = redisTemplate.hasKey(TOKEN_PREFIX + token);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Remove token and user session (logout).
     */
    public void removeTokenAndSession(String token, String username) {
        redisTemplate.delete(TOKEN_PREFIX + token);
        if (username != null) {
            redisTemplate.delete(USER_SESSION_PREFIX + username);
        }
    }

    /**
     * Remove user session.
     */
    public void removeUserSession(String username) {
        redisTemplate.delete(USER_SESSION_PREFIX + username);
    }
}
