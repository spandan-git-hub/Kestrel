package com.kestrel.common.core.budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component("coreInMemoryCallBudgetTracker")
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
public class InMemoryCallBudgetTracker implements CallBudgetTracker {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCallBudgetTracker.class);

    private final int maxMinuteLimit;
    private final int maxDayLimit;
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public InMemoryCallBudgetTracker(
            @Value("${kestrel.open-meteo.rate-limit.minute-max:500}") int maxMinuteLimit,
            @Value("${kestrel.open-meteo.rate-limit.day-max:8000}") int maxDayLimit) {
        this.maxMinuteLimit = maxMinuteLimit;
        this.maxDayLimit = maxDayLimit;
    }

    public InMemoryCallBudgetTracker() {
        this(500, 8000);
    }

    @Override
    public synchronized boolean tryAcquirePermit() {
        long nowEpochSec = Instant.now().getEpochSecond();
        String minuteKey = DynamoDbCallBudgetTracker.getMinuteKey(nowEpochSec);
        String dayKey = DynamoDbCallBudgetTracker.getDayKey();

        AtomicInteger minuteCounter = counters.computeIfAbsent(minuteKey, k -> new AtomicInteger(0));
        AtomicInteger dayCounter = counters.computeIfAbsent(dayKey, k -> new AtomicInteger(0));

        if (minuteCounter.get() >= maxMinuteLimit) {
            log.warn("Minute call budget exceeded in InMemoryCallBudgetTracker: {}/{}", minuteCounter.get(), maxMinuteLimit);
            return false;
        }

        if (dayCounter.get() >= maxDayLimit) {
            log.warn("Day call budget exceeded in InMemoryCallBudgetTracker: {}/{}", dayCounter.get(), maxDayLimit);
            return false;
        }

        minuteCounter.incrementAndGet();
        dayCounter.incrementAndGet();
        return true;
    }

    @Override
    public long getCallCount(String budgetKey) {
        AtomicInteger counter = counters.get(budgetKey);
        return counter != null ? counter.get() : 0L;
    }

    public void setCallCount(String budgetKey, int count) {
        counters.computeIfAbsent(budgetKey, k -> new AtomicInteger(0)).set(count);
    }

    @Override
    public BudgetStatus getBudgetStatus() {
        long nowEpochSec = Instant.now().getEpochSecond();
        String minuteKey = DynamoDbCallBudgetTracker.getMinuteKey(nowEpochSec);
        String dayKey = DynamoDbCallBudgetTracker.getDayKey();

        long minCount = getCallCount(minuteKey);
        long dayCount = getCallCount(dayKey);
        boolean allowed = minCount < maxMinuteLimit && dayCount < maxDayLimit;

        return new BudgetStatus(minCount, maxMinuteLimit, dayCount, maxDayLimit, allowed);
    }

    public int getMaxMinuteLimit() {
        return maxMinuteLimit;
    }

    public int getMaxDayLimit() {
        return maxDayLimit;
    }
}

