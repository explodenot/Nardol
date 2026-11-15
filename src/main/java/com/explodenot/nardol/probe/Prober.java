package com.explodenot.nardol.probe;

import com.explodenot.nardol.model.AddressFamily;
import com.explodenot.nardol.model.ProbeOutcome;
import com.explodenot.nardol.model.Target;

import java.time.Duration;

public interface Prober {
    ProbeOutcome probe(Target target, Duration timeout, AddressFamily addressFamily);
}
