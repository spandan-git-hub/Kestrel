package com.kestrel.common.core.budget;

public interface CallBudgetTracker {

    /**
     * Checks and atomically increments usage counters for one Open-Meteo API call.
     * Enforces minute quota (< 500 req/min, hard limit 600) and day quota (< 8000 req/day, hard limit 10,000).
     *
     * @return true if permit was acquired; false if rate limits/budgets are exceeded
     */
    boolean tryAcquirePermit();

    /**
     * Retrieves current call count for a specific budget key (e.g., BUDGET#om#min#..., BUDGET#om#day#...).
     *
     * @param budgetKey the partition key
     * @return current count, or 0 if item not found
     */
    long getCallCount(String budgetKey);

    /**
     * Budget status record representing current usage and quota boundaries.
     */
    record BudgetStatus(
            long currentMinuteCount,
            int maxMinuteLimit,
            long currentDayCount,
            int maxDayLimit,
            boolean isAllowed
    ) {}

    /**
     * Returns the current status of both the minute and day call budgets.
     */
    BudgetStatus getBudgetStatus();
}

