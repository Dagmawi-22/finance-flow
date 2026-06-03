package com.financeflow.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeflow.domain.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String RESPONSE_METADATA_KEY = "idempotentResponse";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;
    private final IdempotencyProperties properties;

    public <T> T execute(
            String idempotencyKey,
            IdempotencyScope scope,
            Class<T> responseType,
            Supplier<T> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        var cached = readCached(scope, idempotencyKey, responseType);
        if (cached.isPresent()) {
            return cached.get();
        }

        if (!acquireLock(scope, idempotencyKey)) {
            throw new IdempotencyConflictException();
        }

        try {
            cached = readCached(scope, idempotencyKey, responseType);
            if (cached.isPresent()) {
                return cached.get();
            }

            var result = action.get();
            writeCached(scope, idempotencyKey, result);
            return result;
        } finally {
            releaseLock(scope, idempotencyKey);
        }
    }

    public void attachResponse(java.util.UUID transactionId, Object response) {
        transactionRepository.findById(transactionId).ifPresent(transaction -> {
            var metadata = transaction.getMetadata() != null
                    ? new HashMap<>(transaction.getMetadata())
                    : new HashMap<String, Object>();
            metadata.put(RESPONSE_METADATA_KEY, objectMapper.convertValue(response, Map.class));
            transaction.setMetadata(metadata);
            transactionRepository.save(transaction);
        });
    }

    private <T> Optional<T> readCached(IdempotencyScope scope, String idempotencyKey, Class<T> responseType) {
        var redisValue = redis.opsForValue().get(scope.cacheKey(idempotencyKey));
        if (redisValue != null) {
            return Optional.of(deserialize(redisValue, responseType));
        }

        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .flatMap(transaction -> {
                    var metadata = transaction.getMetadata();
                    if (metadata == null || !metadata.containsKey(RESPONSE_METADATA_KEY)) {
                        return Optional.empty();
                    }
                    return Optional.of(objectMapper.convertValue(metadata.get(RESPONSE_METADATA_KEY), responseType));
                });
    }

    private void writeCached(IdempotencyScope scope, String idempotencyKey, Object response) {
        try {
            redis.opsForValue().set(
                    scope.cacheKey(idempotencyKey),
                    objectMapper.writeValueAsString(response),
                    properties.ttl());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to cache idempotent response", ex);
        }
    }

    private boolean acquireLock(IdempotencyScope scope, String idempotencyKey) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(
                scope.lockKey(idempotencyKey),
                "1",
                properties.lockTtl()));
    }

    private void releaseLock(IdempotencyScope scope, String idempotencyKey) {
        redis.delete(scope.lockKey(idempotencyKey));
    }

    private <T> T deserialize(String json, Class<T> responseType) {
        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read cached idempotent response", ex);
        }
    }
}
