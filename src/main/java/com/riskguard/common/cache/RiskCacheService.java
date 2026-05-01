package com.riskguard.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class RiskCacheService {

    private static final Logger log = LoggerFactory.getLogger(RiskCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RiskCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void putRuleVersion(Long ruleId, Integer version, Object snapshot) {
        put("risk:rule:%d:%d".formatted(ruleId, version), snapshot);
    }

    public void putStrategyVersion(Long strategyId, Integer version, Object snapshot) {
        put("risk:strategy:version:%d:%d".formatted(strategyId, version), snapshot);
    }

    public void putActiveStrategy(String eventType, Object snapshot) {
        put("risk:strategy:active:%s".formatted(eventType), snapshot);
    }

    public <T> Optional<T> getActiveStrategy(String eventType, Class<T> type) {
        return get("risk:strategy:active:%s".formatted(eventType), type);
    }

    public <T> Optional<T> getIdempotentDecision(String requestNo, Class<T> type) {
        return get("risk:idempotent:%s".formatted(requestNo), type);
    }

    public void putIdempotentDecision(String requestNo, Object decision) {
        put("risk:idempotent:%s".formatted(requestNo), decision, 24L, TimeUnit.HOURS);
    }

    public void evictActiveStrategy(String eventType) {
        try {
            redisTemplate.delete("risk:strategy:active:%s".formatted(eventType));
        } catch (RuntimeException ex) {
            log.warn("Failed to evict active strategy cache for eventType={}", eventType, ex);
        }
    }

    private void put(String key, Object value) {
        put(key, value, null, null);
    }

    private void put(String key, Object value, Long timeout, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            if (timeout == null || unit == null) {
                redisTemplate.opsForValue().set(key, json);
            } else {
                redisTemplate.opsForValue().set(key, json, timeout, unit);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize cache payload: " + key, ex);
        } catch (RuntimeException ex) {
            log.warn("Failed to refresh cache key={}", key, ex);
        }
    }

    private <T> Optional<T> get(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, type));
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Failed to read cache key={}", key, ex);
            return Optional.empty();
        }
    }
}
