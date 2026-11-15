package com.explodenot.gli.probe;

import com.explodenot.gli.model.AddressFamily;
import com.explodenot.gli.model.ProbeOutcome;
import com.explodenot.gli.model.Target;

import java.time.Duration;

public interface Prober {
    ProbeOutcome probe(Target target, Duration timeout, AddressFamily addressFamily);
}
