package com.kestrel.alertfanout.app;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class MockDispatchQueue {

    public static final int MAX_CAPACITY = 100;

    public record DispatchItem(
            Long id,
            String alertId,
            String locationId,
            String locationName,
            Severity severity,
            Channel channel,
            String recipient,
            Language language,
            Instant timestamp,
            String messageExcerpt,
            Provenance provenance,
            String status,
            String whatsAppUrl
    ) {}

    private final ConcurrentLinkedDeque<DispatchItem> deque = new ConcurrentLinkedDeque<>();

    public void enqueue(DispatchItem item) {
        deque.addFirst(item);
        while (deque.size() > MAX_CAPACITY) {
            deque.pollLast();
        }
    }

    public List<DispatchItem> getRecent(int limit) {
        int target = Math.max(1, Math.min(limit, MAX_CAPACITY));
        List<DispatchItem> list = new ArrayList<>(deque);
        if (list.size() > target) {
            return Collections.unmodifiableList(list.subList(0, target));
        }
        return Collections.unmodifiableList(list);
    }

    public List<DispatchItem> getAllRecent() {
        return getRecent(MAX_CAPACITY);
    }

    public int size() {
        return deque.size();
    }

    public void clear() {
        deque.clear();
    }
}
