package com.explodenot.nardol.model;

import java.time.Instant;

public record ProbeSample(
    Instant timestamp,
    ProbeOutcome outcome,
    double latencyMs,
    boolean successful
) {}
