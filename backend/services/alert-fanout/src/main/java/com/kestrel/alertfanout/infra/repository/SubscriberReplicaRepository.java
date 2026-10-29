package com.kestrel.alertfanout.infra.repository;

import com.kestrel.alertfanout.infra.entity.SubscriberReplica;
import com.kestrel.common.events.Channel;

import java.util.List;
import java.util.Optional;

public interface SubscriberReplicaRepository {

    SubscriberReplica save(SubscriberReplica replica);

    Optional<SubscriberReplica> findByLocationIdAndToken(String locationId, String token);

    List<SubscriberReplica> findByLocationId(String locationId);

    List<SubscriberReplica> findByChannel(Channel channel);

    void deleteByLocationIdAndToken(String locationId, String token);

    void deleteByToken(String token);

    List<SubscriberReplica> findAll();

    long count();

    void deleteAll();
}

