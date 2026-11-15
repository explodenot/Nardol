package com.explodenot.gli.model;

import java.time.Instant;

public record ProbeSample(
    Instant timestamp,
    ProbeOutcome outcome,
    double latencyMs,
    boolean successful
) {}
