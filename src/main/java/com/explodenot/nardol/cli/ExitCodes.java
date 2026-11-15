package com.explodenot.nardol.cli;

public final class ExitCodes {
    private ExitCodes() {}

    public static final int SUCCESS = 0;              // All reachable
    public static final int DEGRADED_OR_DOWN = 1;     // Some degraded/down
    public static final int ALL_DOWN_OR_CONTROL_FAIL = 2; // All down or controls failed
    public static final int USAGE_ERROR = 64;         // Usage error
    public static final int INTERNAL_ERROR = 70;      // Internal error
}
