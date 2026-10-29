package com.kestrel.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Channel {
    TELEGRAM,
    WEB_PUSH,
    WHATSAPP_LINK;

    @JsonCreator
    public static Channel fromString(String value) {
        if (value == null) return null;
        String upper = value.trim().toUpperCase();
        if ("PUSH".equals(upper) || "WEBPUSH".equals(upper) || "WEB_PUSH".equals(upper)) {
            return WEB_PUSH;
        }
        if ("WHATSAPP".equals(upper) || "WHATSAPP_LINK".equals(upper)) {
            return WHATSAPP_LINK;
        }
        for (Channel c : values()) {
            if (c.name().equalsIgnoreCase(upper)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown delivery channel: " + value);
    }
}
