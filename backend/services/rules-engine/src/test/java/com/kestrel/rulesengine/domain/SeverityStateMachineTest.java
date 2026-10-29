package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SeverityStateMachineTest {

    private final SeverityStateMachine stateMachine = new SeverityStateMachine();
    private final Instant now = Instant.parse("2026-10-23T10:00:00Z");
    private final Instant freshData = now.minus(Duration.ofMinutes(15));

    @Test
    @DisplayName("Stable NONE: candidate NONE remains NONE, candidateRuns resets, no notification")
    void stableNone() {
        StateTransition transition = stateMachine.evaluateTransition(
                Severity.NONE,
                Severity.NONE,
                0,
                Severity.NONE,
                freshData,
                null,
                now,
                false
        );

        assertThat(transition.targetSeverity()).isEqualTo(Severity.NONE);
        assertThat(transition.candidateRuns()).isEqualTo(0);
        assertThat(transition.severityChanged()).isFalse();
        assertThat(transition.isStale()).isFalse();
        assertThat(transition.shouldNotify()).isFalse();
    }

    @Test
    @DisplayName("WATCH escalation requires only 1 run from NONE")
    void watchEscalationRequiresOneRun() {
        StateTransition transition = stateMachine.evaluateTransition(
                Severity.NONE,
                Severity.NONE,
                0,
                Severity.WATCH,
                freshData,
                null,
                now,
                false
        );

        assertThat(transition.targetSeverity()).isEqualTo(Severity.WATCH);
        assertThat(transition.candidateRuns()).isEqualTo(0);
        assertThat(transition.severityChanged()).isTrue();
        assertThat(transition.shouldNotify()).isTrue();
    }

    @Test
    @DisplayName("WARNING escalation requires 2 consecutive runs")
    void warningEscalationRequiresTwoRuns() {
        // Run 1: First observation of WARNING candidate
        StateTransition run1 = stateMachine.evaluateTransition(
                Severity.NONE,
                Severity.NONE,
                0,
                Severity.WARNING,
                freshData,
                null,
                now,
                false
        );

        assertThat(run1.targetSeverity()).isEqualTo(Severity.NONE);
        assertThat(run1.candidateSeverity()).isEqualTo(Severity.WARNING);
        assertThat(run1.candidateRuns()).isEqualTo(1);
        assertThat(run1.severityChanged()).isFalse();
        assertThat(run1.shouldNotify()).isFalse();

        // Run 2: Second consecutive observation of WARNING candidate
        StateTransition run2 = stateMachine.evaluateTransition(
                Severity.NONE,
                Severity.WARNING,
                run1.candidateRuns(),
                Severity.WARNING,
                freshData,
                null,
                now.plus(Duration.ofMinutes(30)),
                false
        );

        assertThat(run2.targetSeverity()).isEqualTo(Severity.WARNING);
        assertThat(run2.candidateRuns()).isEqualTo(0);
        assertThat(run2.severityChanged()).isTrue();
        assertThat(run2.shouldNotify()).isTrue();
    }

    @Test
    @DisplayName("SEVERE escalation requires 2 consecutive runs from WARNING")
    void severeEscalationRequiresTwoRuns() {
        // Run 1: SEVERE candidate observed while currently WARNING
        StateTransition run1 = stateMachine.evaluateTransition(
                Severity.WARNING,
                Severity.WARNING,
                0,
                Severity.SEVERE,
                freshData,
                now.minus(Duration.ofHours(1)),
                now,
                false
        );

        assertThat(run1.targetSeverity()).isEqualTo(Severity.WARNING);
        assertThat(run1.candidateRuns()).isEqualTo(1);
        assertThat(run1.severityChanged()).isFalse();
        assertThat(run1.shouldNotify()).isFalse();

        // Run 2: Second SEVERE observation
        StateTransition run2 = stateMachine.evaluateTransition(
                Severity.WARNING,
                Severity.SEVERE,
                run1.candidateRuns(),
                Severity.SEVERE,
                freshData,
                now.minus(Duration.ofHours(1)),
                now.plus(Duration.ofMinutes(20)),
                false
        );

        assertThat(run2.targetSeverity()).isEqualTo(Severity.SEVERE);
        assertThat(run2.candidateRuns()).isEqualTo(0);
        assertThat(run2.severityChanged()).isTrue();
        assertThat(run2.shouldNotify()).isTrue();
    }

    @Test
    @DisplayName("De-escalation steps down one level at a time after 2 consecutive runs")
    void deescalationOneLevelAtATime() {
        // Currently SEVERE, candidate is NONE
        // Run 1 of candidate NONE
        StateTransition run1 = stateMachine.evaluateTransition(
                Severity.SEVERE,
                Severity.SEVERE,
                0,
                Severity.NONE,
                freshData,
                now.minus(Duration.ofHours(2)),
                now,
                false
        );
        assertThat(run1.targetSeverity()).isEqualTo(Severity.SEVERE);
        assertThat(run1.candidateRuns()).isEqualTo(1);
        assertThat(run1.severityChanged()).isFalse();

        // Run 2 of candidate NONE -> Steps down 1 level to WARNING
        StateTransition run2 = stateMachine.evaluateTransition(
                Severity.SEVERE,
                Severity.NONE,
                run1.candidateRuns(),
                Severity.NONE,
                freshData,
                now.minus(Duration.ofHours(2)),
                now.plus(Duration.ofMinutes(15)),
                false
        );
        assertThat(run2.targetSeverity()).isEqualTo(Severity.WARNING);
        assertThat(run2.candidateRuns()).isEqualTo(0);
        assertThat(run2.severityChanged()).isTrue();
        assertThat(run2.shouldNotify()).isTrue();

        // Run 3: candidate still NONE, but new baseline is WARNING
        StateTransition run3 = stateMachine.evaluateTransition(
                Severity.WARNING,
                Severity.NONE,
                0,
                Severity.NONE,
                freshData,
                now.plus(Duration.ofMinutes(15)),
                now.plus(Duration.ofMinutes(30)),
                false
        );
        assertThat(run3.targetSeverity()).isEqualTo(Severity.WARNING);
        assertThat(run3.candidateRuns()).isEqualTo(1);

        // Run 4: Second run at WARNING with candidate NONE -> steps down to WATCH
        StateTransition run4 = stateMachine.evaluateTransition(
                Severity.WARNING,
                Severity.NONE,
                run3.candidateRuns(),
                Severity.NONE,
                freshData,
                now.plus(Duration.ofMinutes(15)),
                now.plus(Duration.ofMinutes(45)),
                false
        );
        assertThat(run4.targetSeverity()).isEqualTo(Severity.WATCH);
        assertThat(run4.severityChanged()).isTrue();
        assertThat(run4.shouldNotify()).isTrue();
    }

    @Test
    @DisplayName("Simulation mode overrides persistence: 1 run immediately escalates to WARNING or SEVERE")
    void simulationImmediatePersistence() {
        StateTransition simWarning = stateMachine.evaluateTransition(
                Severity.NONE,
                Severity.NONE,
                0,
                Severity.WARNING,
                freshData,
                null,
                now,
                true // isSimulated
        );

        assertThat(simWarning.targetSeverity()).isEqualTo(Severity.WARNING);
        assertThat(simWarning.severityChanged()).isTrue();
        assertThat(simWarning.shouldNotify()).isTrue();

        StateTransition simSevere = stateMachine.evaluateTransition(
                Severity.NONE,
                Severity.NONE,
                0,
                Severity.SEVERE,
                freshData,
                null,
                now,
                true // isSimulated
        );

        assertThat(simSevere.targetSeverity()).isEqualTo(Severity.SEVERE);
        assertThat(simSevere.severityChanged()).isTrue();
        assertThat(simSevere.shouldNotify()).isTrue();
    }

    @Test
    @DisplayName("Stale data guard: forecast older than 36h suppresses escalation and sets isStale=true")
    void staleDataGuardSuppressesEscalation() {
        Instant staleData = now.minus(Duration.ofHours(37));

        StateTransition transition = stateMachine.evaluateTransition(
                Severity.NONE,
                Severity.NONE,
                0,
                Severity.SEVERE,
                staleData,
                null,
                now,
                false
        );

        assertThat(transition.targetSeverity()).isEqualTo(Severity.NONE);
        assertThat(transition.isStale()).isTrue();
        assertThat(transition.severityChanged()).isFalse();
        assertThat(transition.shouldNotify()).isFalse();
        assertThat(transition.reason()).contains("stale (>36h");
    }

    @ParameterizedTest(name = "Hours since last alert: {0}h -> shouldNotify: {1}")
    @CsvSource({
            "2, false",
            "11, false",
            "12, true",
            "24, true"
    })
    @DisplayName("12-hour cooldown suppresses duplicate notifications for unchanged active severity")
    void cooldownSuppressesDuplicates(long hoursAgo, boolean expectedShouldNotify) {
        Instant lastNotified = now.minus(Duration.ofHours(hoursAgo));

        StateTransition transition = stateMachine.evaluateTransition(
                Severity.WARNING,
                Severity.WARNING,
                0,
                Severity.WARNING,
                freshData,
                lastNotified,
                now,
                false
        );

        assertThat(transition.targetSeverity()).isEqualTo(Severity.WARNING);
        assertThat(transition.severityChanged()).isFalse();
        assertThat(transition.shouldNotify()).isEqualTo(expectedShouldNotify);
    }

    @Test
    @DisplayName("evaluateTransition without isSimulated strictly enforces real persistence rules")
    void evaluateTransitionRealProductionDefaults() {
        // Run 1 of WARNING should NOT escalate
        StateTransition run1 = stateMachine.evaluateTransition(
                Severity.NONE,
                Severity.NONE,
                0,
                Severity.WARNING,
                freshData,
                null,
                now
        );
        assertThat(run1.targetSeverity()).isEqualTo(Severity.NONE);
        assertThat(run1.candidateRuns()).isEqualTo(1);
        assertThat(run1.severityChanged()).isFalse();

        // Run 2 of WARNING escalates
        StateTransition run2 = stateMachine.evaluateTransition(
                Severity.NONE,
                Severity.WARNING,
                run1.candidateRuns(),
                Severity.WARNING,
                freshData,
                null,
                now.plus(Duration.ofMinutes(15))
        );
        assertThat(run2.targetSeverity()).isEqualTo(Severity.WARNING);
        assertThat(run2.candidateRuns()).isEqualTo(0);
        assertThat(run2.severityChanged()).isTrue();
        assertThat(run2.shouldNotify()).isTrue();
    }
}
