package com.kestrel.alertfanout.infra.limiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory fallback and test token bucket rate limiter for Telegram Bot API dispatches.
 * Tracks per-second counts with 25 msg/s target and 30 msg/s ceiling.
 */
@Component
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
public class InMemoryTelegramRateLimiter implements TelegramRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTelegramRateLimiter.class);

    private final ConcurrentHashMap<String, AtomicLong> secondBuckets = new ConcurrentHashMap<>();
    private final int targetLimit;
    private final int ceilingLimit;

    public InMemoryTelegramRateLimiter(
            @Value("${kestrel.telegram.rate-limit.target:25}") int targetLimit,
            @Value("${kestrel.telegram.rate-limit.ceiling:30}") int ceilingLimit) {
        this.targetLimit = targetLimit;
        this.ceilingLimit = ceilingLimit;
    }

    public InMemoryTelegramRateLimiter() {
        this(25, 30);
    }

    @Override
    public synchronized boolean tryAcquire() {
        long nowEpochSec = Instant.now().getEpochSecond();
        String key = "TG_BUCKET#sec#" + nowEpochSec;
        AtomicLong counter = secondBuckets.computeIfAbsent(key, k -> new AtomicLong(0));
        if (counter.get() >= ceilingLimit) {
            log.warn("Telegram dispatch ceiling exceeded in memory ({}/{} msg/s)", counter.get(), ceilingLimit);
            return false;
        }
        counter.incrementAndGet();
        return true;
    }

    @Override
    public boolean acquireWithBackoff(long maxWaitMillis) {
        long deadline = System.currentTimeMillis() + maxWaitMillis;
        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire()) {
                return true;
            }
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Override
    public RateLimitStatus getStatus() {
        long nowEpochSec = Instant.now().getEpochSecond();
        String key = "TG_BUCKET#sec#" + nowEpochSec;
        AtomicLong counter = secondBuckets.get(key);
        long count = counter != null ? counter.get() : 0;
        return new RateLimitStatus(count, targetLimit, ceilingLimit, count < ceilingLimit);
    }

    public void reset() {
        secondBuckets.clear();
    }
}

