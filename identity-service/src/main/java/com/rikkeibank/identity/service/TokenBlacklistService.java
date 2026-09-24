package com.rikkeibank.identity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TokenBlacklistService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // Resilient fallback storage when Redis is offline
    private final Set<String> localBlacklist = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, Long> userMinValidTimestamps = new ConcurrentHashMap<>();

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final String USER_FORCE_LOGOUT_PREFIX = "user:force_logout:";

    public void blacklistToken(String token, long ttlMillis) {
        localBlacklist.add(token);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "revoked", ttlMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("Redis unavailable, using local memory blacklist: {}", e.getMessage());
            }
        }
    }

    public boolean isTokenBlacklisted(String token) {
        if (localBlacklist.contains(token)) {
            return true;
        }
        if (redisTemplate != null) {
            try {
                Boolean hasKey = redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token);
                return Boolean.TRUE.equals(hasKey);
            } catch (Exception e) {
                log.warn("Redis check failed, relying on local blacklist: {}", e.getMessage());
            }
        }
        return false;
    }

    public void forceLogoutUser(Long userId) {
        long now = System.currentTimeMillis();
        userMinValidTimestamps.put(userId, now);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(USER_FORCE_LOGOUT_PREFIX + userId, String.valueOf(now), 7, TimeUnit.DAYS);
            } catch (Exception e) {
                log.warn("Redis unavailable for force logout, updated local cache: {}", e.getMessage());
            }
        }
        log.info("Force logout executed for userId: {}. All previous tokens revoked.", userId);
    }

    public boolean isUserForceLoggedOut(Long userId, long tokenIssuedAtMillis) {
        Long minValid = userMinValidTimestamps.get(userId);
        if (redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get(USER_FORCE_LOGOUT_PREFIX + userId);
                if (val != null) {
                    minValid = Long.parseLong(val);
                }
            } catch (Exception ignored) {
            }
        }
        return minValid != null && tokenIssuedAtMillis < minValid;
    }
}
