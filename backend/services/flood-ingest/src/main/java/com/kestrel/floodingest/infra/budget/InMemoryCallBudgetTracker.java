package com.kestrel.floodingest.infra.budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
public class InMemoryCallBudgetTracker implements CallBudgetTracker {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCallBudgetTracker.class);

    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final int maxMinuteLimit;
    private final int maxDayLimit;

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

        AtomicLong minuteCounter = counters.computeIfAbsent(minuteKey, k -> new AtomicLong(0));
        AtomicLong dayCounter = counters.computeIfAbsent(dayKey, k -> new AtomicLong(0));

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
        log.debug("InMemory budget consumed: minuteKey={} (count={}), dayKey={} (count={})",
                minuteKey, minuteCounter.get(), dayKey, dayCounter.get());
        return true;
    }

    @Override
    public long getCallCount(String budgetKey) {
        AtomicLong counter = counters.get(budgetKey);
        return counter != null ? counter.get() : 0L;
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

    public void setCallCount(String budgetKey, long count) {
        counters.computeIfAbsent(budgetKey, k -> new AtomicLong()).set(count);
    }

    public void reset() {
        counters.clear();
    }
}

