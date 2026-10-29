package com.kestrel.alertfanout.infra.limiter;

/**
 * Token-bucket rate limiter interface for Telegram Bot API dispatches.
 * Enforces production rate limit boundaries (target 25 msg/s, max 30 msg/s ceiling).
 */
public interface TelegramRateLimiter {

    /**
     * Attempts to acquire 1 dispatch permit immediately.
     *
     * @return true if permit acquired within ceiling; false if rate limit exceeded.
     */
    boolean tryAcquire();

    /**
     * Attempts to acquire 1 dispatch permit, backing off up to maxWaitMillis before aborting.
     *
     * @param maxWaitMillis maximum milliseconds to wait
     * @return true if permit acquired; false if timed out
     */
    boolean acquireWithBackoff(long maxWaitMillis);

    record RateLimitStatus(long currentPerSecondCount, long targetLimit, long ceilingLimit, boolean allowed) {}

    /**
     * Returns the current status of the Telegram dispatch rate limit window.
     */
    RateLimitStatus getStatus();
}

