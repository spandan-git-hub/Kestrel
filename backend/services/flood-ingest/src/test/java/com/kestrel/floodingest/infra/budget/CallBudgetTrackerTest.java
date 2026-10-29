package com.kestrel.floodingest.infra.budget;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CallBudgetTrackerTest {

    @Test
    @DisplayName("InMemory tracker enforces minute limit and day limit")
    void inMemoryTrackerEnforcesLimits() {
        InMemoryCallBudgetTracker tracker = new InMemoryCallBudgetTracker(2, 5);

        // First two calls should succeed
        assertThat(tracker.tryAcquirePermit()).isTrue();
        assertThat(tracker.tryAcquirePermit()).isTrue();

        // Third call should fail because minute limit is 2
        assertThat(tracker.tryAcquirePermit()).isFalse();

        CallBudgetTracker.BudgetStatus status = tracker.getBudgetStatus();
        assertThat(status.currentMinuteCount()).isEqualTo(2);
        assertThat(status.maxMinuteLimit()).isEqualTo(2);
        assertThat(status.currentDayCount()).isEqualTo(2);
        assertThat(status.maxDayLimit()).isEqualTo(5);
        assertThat(status.isAllowed()).isFalse();
    }

    @Test
    @DisplayName("InMemory tracker enforces day limit when minute limit is large")
    void inMemoryTrackerEnforcesDayLimit() {
        InMemoryCallBudgetTracker tracker = new InMemoryCallBudgetTracker(10, 2);

        assertThat(tracker.tryAcquirePermit()).isTrue();
        assertThat(tracker.tryAcquirePermit()).isTrue();
        // Exceeds day limit 2
        assertThat(tracker.tryAcquirePermit()).isFalse();
    }

    @Test
    @DisplayName("Key formatting matches expected BUDGET#om#min# and BUDGET#om#day# format")
    void keyFormattingMatchesSpec() {
        long epochSec = 1793264400L; // Fixed epoch
        String minKey = DynamoDbCallBudgetTracker.getMinuteKey(epochSec);
        assertThat(minKey).isEqualTo("BUDGET#om#min#" + (epochSec / 60));

        String today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String dayKey = DynamoDbCallBudgetTracker.getDayKey();
        assertThat(dayKey).isEqualTo("BUDGET#om#day#" + today);
    }

    @Test
    @DisplayName("DynamoDbCallBudgetTracker executes TransactWriteItems and returns true on success")
    void dynamoDbTrackerSuccess() {
        AtomicBoolean transactCalled = new AtomicBoolean(false);
        DynamoDbClient client = new DynamoDbClient() {
            @Override
            public String serviceName() {
                return "dynamodb";
            }

            @Override
            public void close() {}

            @Override
            public software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse transactWriteItems(TransactWriteItemsRequest request) {
                transactCalled.set(true);
                return software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse.builder().build();
            }
        };

        DynamoDbTableNameResolver resolver = new DynamoDbTableNameResolver("test");
        DynamoDbCallBudgetTracker tracker = new DynamoDbCallBudgetTracker(client, resolver, 500, 8000);

        assertThat(tracker.getTableName()).isEqualTo("kestrel_flood_call_budget_test");

        boolean result = tracker.tryAcquirePermit();
        assertThat(result).isTrue();
        assertThat(transactCalled.get()).isTrue();
    }

    @Test
    @DisplayName("DynamoDbCallBudgetTracker catches TransactionCanceledException when budget exceeded and returns false")
    void dynamoDbTrackerCatchesCancellation() {
        DynamoDbClient client = new DynamoDbClient() {
            @Override
            public String serviceName() {
                return "dynamodb";
            }

            @Override
            public void close() {}

            @Override
            public software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse transactWriteItems(TransactWriteItemsRequest request) {
                throw TransactionCanceledException.builder().message("Conditional check failed").build();
            }
        };

        DynamoDbTableNameResolver resolver = new DynamoDbTableNameResolver("test");
        DynamoDbCallBudgetTracker tracker = new DynamoDbCallBudgetTracker(client, resolver, 500, 8000);

        boolean result = tracker.tryAcquirePermit();
        assertThat(result).isFalse();
    }
}

