package com.kestrel.common.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventSerializationTests {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = EventObjectMapperFactory.createObjectMapper();
    }

    @Test
    void testLocationUpsertedEventSerialization() throws Exception {
        LocationUpsertedPayload payload = new LocationUpsertedPayload(
            "mumbai-kurla",
            "Kurla Ward L, Mumbai",
            "Maharashtra",
            19.0726,
            72.8797,
            "Mithi River",
            45.0,
            85.0,
            140.0,
            Language.mr,
            true
        );

        EventEnvelope<LocationUpsertedPayload> envelope = EventEnvelope.of(
            "LocationUpserted",
            "subscriber-registry",
            "test-corr-1",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        assertThat(json).contains("\"eventType\":\"LocationUpserted\"");
        assertThat(json).contains("\"primaryLanguage\":\"mr\"");
        assertThat(json).contains("\"riverName\":\"Mithi River\"");

        EventEnvelope<LocationUpsertedPayload> deserialized = objectMapper.readValue(
            json,
            new TypeReference<EventEnvelope<LocationUpsertedPayload>>() {}
        );

        assertThat(deserialized.eventId()).isEqualTo(envelope.eventId());
        assertThat(deserialized.payload().locationId()).isEqualTo("mumbai-kurla");
        assertThat(deserialized.payload().threshold2yr()).isEqualTo(45.0);
        assertThat(deserialized.payload().primaryLanguage()).isEqualTo(Language.mr);
    }

    @Test
    void testFloodSignalEventSerialization() throws Exception {
        FloodSignalPayload.ForecastDayStats day1 = new FloodSignalPayload.ForecastDayStats(
            LocalDate.of(2026, 10, 24),
            12.5,
            20.0,
            35.5,
            50.0,
            75.2,
            0.65,
            0.20,
            0.0
        );

        FloodSignalPayload payload = new FloodSignalPayload(
            "patna-ward12",
            Instant.parse("2026-10-22T12:00:00Z"),
            false,
            51,
            List.of(day1)
        );

        EventEnvelope<FloodSignalPayload> envelope = EventEnvelope.of(
            "FloodSignal",
            "flood-ingest",
            "test-corr-2",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        assertThat(json).contains("2026-10-22T12:00:00Z");
        assertThat(json).contains("2026-10-24");
        assertThat(json).contains("\"memberCount\":51");

        EventEnvelope<FloodSignalPayload> deserialized = objectMapper.readValue(
            json,
            new TypeReference<EventEnvelope<FloodSignalPayload>>() {}
        );

        assertThat(deserialized.payload().locationId()).isEqualTo("patna-ward12");
        assertThat(deserialized.payload().days()).hasSize(1);
        assertThat(deserialized.payload().days().getFirst().share2yr()).isEqualTo(0.65);
    }

    @Test
    void testAlertEventSerialization() throws Exception {
        AlertEventPayload payload = new AlertEventPayload(
            "alert-uuid-123",
            "guwahati-brahmaputra",
            Hazard.FLOOD,
            "DECISION",
            Severity.WATCH,
            Severity.WARNING,
            Provenance.MODEL_ONLY,
            Instant.parse("2026-10-22T14:30:00Z"),
            LocalDate.of(2026, 10, 25),
            0.72,
            true
        );

        EventEnvelope<AlertEventPayload> envelope = EventEnvelope.of(
            "AlertEvent",
            "rules-engine",
            "test-corr-3",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        assertThat(json).contains("\"hazard\":\"FLOOD\"");
        assertThat(json).contains("\"toSeverity\":\"WARNING\"");
        assertThat(json).contains("\"provenance\":\"MODEL_ONLY\"");

        EventEnvelope<AlertEventPayload> deserialized = objectMapper.readValue(
            json,
            new TypeReference<EventEnvelope<AlertEventPayload>>() {}
        );

        assertThat(deserialized.payload().alertId()).isEqualTo("alert-uuid-123");
        assertThat(deserialized.payload().toSeverity()).isEqualTo(Severity.WARNING);
        assertThat(deserialized.payload().peakShare()).isEqualTo(0.72);
    }

    @Test
    void testSubscriptionChangedEventSerialization() throws Exception {
        SubscriptionChangedPayload payload = new SubscriptionChangedPayload(
            "sub-token-xyz",
            "cuttack-mahanadi",
            Channel.TELEGRAM,
            "tg-user-12345",
            Language.en,
            Severity.WARNING,
            "CREATED"
        );

        EventEnvelope<SubscriptionChangedPayload> envelope = EventEnvelope.of(
            "SubscriptionChanged",
            "subscriber-registry",
            "test-corr-4",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        assertThat(json).contains("\"channel\":\"TELEGRAM\"");
        assertThat(json).contains("\"action\":\"CREATED\"");

        EventEnvelope<SubscriptionChangedPayload> deserialized = objectMapper.readValue(
            json,
            new TypeReference<EventEnvelope<SubscriptionChangedPayload>>() {}
        );

        assertThat(deserialized.payload().token()).isEqualTo("sub-token-xyz");
        assertThat(deserialized.payload().channel()).isEqualTo(Channel.TELEGRAM);
        assertThat(deserialized.payload().action()).isEqualTo("CREATED");
    }

    @Test
    void testTelegramDeliveryPayloadSerialization() throws Exception {
        TelegramDeliveryPayload payload = new TelegramDeliveryPayload(
            "deliv-101",
            "alert-999",
            "patna-ward12",
            "Ward 12, Patna",
            Severity.SEVERE,
            "123456789",
            "tok-abc",
            Language.hi,
            "चेतावनी: गंगा नदी खतरे के निशान से ऊपर!",
            Provenance.MODEL_ONLY,
            "https://wa.me/?text=alert",
            Instant.parse("2026-10-29T12:00:00Z")
        );

        EventEnvelope<TelegramDeliveryPayload> envelope = EventEnvelope.of(
            "TelegramDelivery",
            "alert-fanout",
            "corr-tg-1",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        assertThat(json).contains("\"chatId\":\"123456789\"");
        assertThat(json).contains("\"subscriberToken\":\"tok-abc\"");
        assertThat(json).contains("\"severity\":\"SEVERE\"");

        EventEnvelope<TelegramDeliveryPayload> deserialized = objectMapper.readValue(
            json,
            new TypeReference<EventEnvelope<TelegramDeliveryPayload>>() {}
        );

        assertThat(deserialized.payload().deliveryId()).isEqualTo("deliv-101");
        assertThat(deserialized.payload().chatId()).isEqualTo("123456789");
        assertThat(deserialized.payload().language()).isEqualTo(Language.hi);
        assertThat(deserialized.payload().messageText()).contains("गंगा नदी");
    }

    @Test
    void testWebPushDeliveryPayloadSerialization() throws Exception {
        WebPushDeliveryPayload payload = new WebPushDeliveryPayload(
            "deliv-wp-201",
            "alert-kurla-500",
            "mumbai-kurla",
            "Kurla Ward L, Mumbai",
            Severity.WARNING,
            "https://fcm.googleapis.com/fcm/send/test-token",
            "p256dh-key-xyz",
            "auth-key-123",
            "tok-resident-01",
            Language.mr,
            "सावधान: पूर इशारा",
            "मिठी नदीची पातळी इशारा पातळीच्या वर जाण्याची शक्यता आहे.",
            "/dashboard?loc=mumbai-kurla",
            Provenance.MODEL_ONLY,
            Instant.parse("2026-10-29T14:00:00Z")
        );

        EventEnvelope<WebPushDeliveryPayload> envelope = EventEnvelope.of(
            "WebPushDelivery",
            "alert-fanout",
            "corr-wp-1",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        assertThat(json).contains("\"endpoint\":\"https://fcm.googleapis.com/fcm/send/test-token\"");
        assertThat(json).contains("\"subscriberToken\":\"tok-resident-01\"");
        assertThat(json).contains("\"language\":\"mr\"");

        EventEnvelope<WebPushDeliveryPayload> deserialized = objectMapper.readValue(
            json,
            new TypeReference<EventEnvelope<WebPushDeliveryPayload>>() {}
        );

        assertThat(deserialized.payload().deliveryId()).isEqualTo("deliv-wp-201");
        assertThat(deserialized.payload().endpoint()).isEqualTo("https://fcm.googleapis.com/fcm/send/test-token");
        assertThat(deserialized.payload().title()).contains("पूर इशारा");
    }
}
