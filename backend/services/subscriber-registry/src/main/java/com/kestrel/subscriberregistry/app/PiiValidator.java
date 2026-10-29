package com.kestrel.subscriberregistry.app;

import java.util.regex.Pattern;

/**
 * Enforces DPDP Act 2023 Zero-PII principles.
 * Verifies that subscriber handles/identifiers do not contain phone numbers or email addresses.
 */
public final class PiiValidator {

    // Matches Indian 10-digit mobile numbers (optionally prefixed by +91, 91, or 0)
    private static final Pattern INDIAN_PHONE_PATTERN = Pattern.compile("^(\\+91[-.\\s]?|91[-.\\s]?|0)?[6-9]\\d{9}$");

    // Matches generic international phone numbers with leading '+'
    private static final Pattern INTL_PHONE_PATTERN = Pattern.compile("^\\+[0-9]{7,15}$");

    // Matches standard email format
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private PiiValidator() {}

    public static boolean containsPii(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }
        String trimmed = identifier.trim();
        String compact = trimmed.replaceAll("[\\s\\-\\(\\)]", "");

        if (INDIAN_PHONE_PATTERN.matcher(compact).matches()) {
            return true;
        }
        if (INTL_PHONE_PATTERN.matcher(compact).matches()) {
            return true;
        }
        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        return false;
    }
}

