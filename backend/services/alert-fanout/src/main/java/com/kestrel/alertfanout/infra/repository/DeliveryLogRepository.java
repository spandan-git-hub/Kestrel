package com.kestrel.alertfanout.infra.repository;

import com.kestrel.alertfanout.infra.entity.DeliveryLog;

import java.util.List;

public interface DeliveryLogRepository {

    List<DeliveryLog> findByAlertId(String alertId);

    DeliveryLog save(DeliveryLog log);

    List<DeliveryLog> findAll();

    long count();

    void deleteAll();

    default List<DeliveryLog> findByLocationIdOrderByTimestampDesc(String locationId) {
        return findAll().stream()
                .filter(l -> locationId.equals(l.getLocationId()))
                .sorted((a, b) -> {
                    if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                    if (a.getTimestamp() == null) return 1;
                    if (b.getTimestamp() == null) return -1;
                    return b.getTimestamp().compareTo(a.getTimestamp());
                })
                .toList();
    }

    default List<DeliveryLog> findTop100ByOrderByTimestampDesc() {
        return findAll().stream()
                .sorted((a, b) -> {
                    if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                    if (a.getTimestamp() == null) return 1;
                    if (b.getTimestamp() == null) return -1;
                    return b.getTimestamp().compareTo(a.getTimestamp());
                })
                .limit(100)
                .toList();
    }
}
