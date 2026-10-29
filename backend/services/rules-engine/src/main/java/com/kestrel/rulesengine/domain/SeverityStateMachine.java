package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.Severity;

import java.time.Duration;
import java.time.Instant;

/**
 * Production Severity State Machine with Real Hysteresis, Persistence Tracking,
 * 12-Hour Notification Cooldown, and 36-Hour Stale Data Suppression.
 */
public class SeverityStateMachine {

    public static final long STALE_DATA_THRESHOLD_HOURS = 36;
    public static final long COOLDOWN_HOURS = 12;

    public StateTransition evaluateTransition(
            Severity currentSeverity,
            Severity currentCandidate,
            int currentCandidateRuns,
            Severity evaluatedCandidate,
            Instant dataAsOf,
            Instant lastNotificationAt,
            Instant now) {
        return evaluateTransition(
                currentSeverity,
                currentCandidate,
                currentCandidateRuns,
                evaluatedCandidate,
                dataAsOf,
                lastNotificationAt,
                now,
                false
        );
    }

    public StateTransition evaluateTransition(
            Severity currentSeverity,
            Severity currentCandidate,
            int currentCandidateRuns,
            Severity evaluatedCandidate,
            Instant dataAsOf,
            Instant lastNotificationAt,
            Instant now,
            boolean isSimulated) {

        if (currentSeverity == null) {
            currentSeverity = Severity.NONE;
        }
        if (evaluatedCandidate == null) {
            evaluatedCandidate = Severity.NONE;
        }

        Duration dataAge = (dataAsOf != null && now != null) ? Duration.between(dataAsOf, now) : Duration.ZERO;
        long dataAgeHours = Math.max(0, dataAge.toHours());
        boolean isStale = (dataAgeHours > STALE_DATA_THRESHOLD_HOURS);

        // Guard: Suppress state transitions on stale data (>36 hours)
        if (isStale) {
            String reason = "State transition suppressed: forecast data is stale (>36h, age=" + dataAgeHours + "h)";
            return new StateTransition(
                    currentSeverity,
                    evaluatedCandidate,
                    0,
                    false, // severityChanged
                    true,  // isStale
                    false, // shouldNotify
                    reason
            );
        }

        int newCandidateRuns;
        if (evaluatedCandidate == currentSeverity) {
            newCandidateRuns = 0;
        } else if (evaluatedCandidate == currentCandidate) {
            newCandidateRuns = currentCandidateRuns + 1;
        } else {
            newCandidateRuns = 1;
        }

        Severity targetSeverity = currentSeverity;
        int nextCandidateRuns = newCandidateRuns;
        String reason;

        if (evaluatedCandidate.ordinal() > currentSeverity.ordinal()) {
            // Escalation persistence: WATCH requires 1 run, WARNING & SEVERE require 2 consecutive runs
            int requiredRuns = isSimulated ? 1 : getEscalationRequiredRuns(evaluatedCandidate);
            if (newCandidateRuns >= requiredRuns) {
                targetSeverity = evaluatedCandidate;
                nextCandidateRuns = 0;
                reason = "Escalated from " + currentSeverity + " to " + evaluatedCandidate + " (" + requiredRuns + " run(s))";
            } else {
                targetSeverity = currentSeverity;
                reason = "Escalation pending: " + newCandidateRuns + "/" + requiredRuns + " runs for " + evaluatedCandidate;
            }
        } else if (evaluatedCandidate.ordinal() < currentSeverity.ordinal()) {
            // De-escalation hysteresis: Requires 2 consecutive calm runs below threshold; step down 1 level
            int requiredRuns = isSimulated ? 1 : 2;
            if (newCandidateRuns >= requiredRuns) {
                targetSeverity = stepDownOneLevel(currentSeverity);
                nextCandidateRuns = 0;
                reason = "De-escalated from " + currentSeverity + " to " + targetSeverity + " (step down 1 level after " + requiredRuns + " runs)";
            } else {
                targetSeverity = currentSeverity;
                reason = "De-escalation pending: " + newCandidateRuns + "/" + requiredRuns + " runs for " + evaluatedCandidate;
            }
        } else {
            targetSeverity = currentSeverity;
            nextCandidateRuns = 0;
            reason = "Stable at " + currentSeverity;
        }

        boolean severityChanged = (targetSeverity != currentSeverity);
        boolean shouldNotify;

        if (severityChanged) {
            shouldNotify = true;
        } else if (targetSeverity != Severity.NONE) {
            boolean cooldownExpired = (lastNotificationAt == null
                    || Duration.between(lastNotificationAt, now).toHours() >= COOLDOWN_HOURS);
            shouldNotify = cooldownExpired;
        } else {
            shouldNotify = false;
        }

        return new StateTransition(
                targetSeverity,
                evaluatedCandidate,
                nextCandidateRuns,
                severityChanged,
                isStale,
                shouldNotify,
                reason
        );
    }

    private int getEscalationRequiredRuns(Severity candidate) {
        if (candidate == Severity.WATCH) {
            return 1;
        }
        return 2; // WARNING and SEVERE require 2 consecutive runs
    }

    public static Severity stepDownOneLevel(Severity current) {
        if (current == null) return Severity.NONE;
        return switch (current) {
            case SEVERE -> Severity.WARNING;
            case WARNING -> Severity.WATCH;
            case WATCH -> Severity.NONE;
            case NONE -> Severity.NONE;
        };
    }
}
