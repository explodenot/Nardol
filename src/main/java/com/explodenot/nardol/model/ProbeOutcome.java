package com.explodenot.nardol.model;

public sealed interface ProbeOutcome permits ProbeOutcome.Success, ProbeOutcome.Failure {

    record Success(
        double dnsMs,
        double connectMs,
        double totalMs,
        String resolvedIp,
        AddressFamily addressFamily,
        String protocol,
        String cipher,
        int httpStatusCode
    ) implements ProbeOutcome {}

    sealed interface Failure extends ProbeOutcome permits
        Failure.DnsFailure,
        Failure.Timeout,
        Failure.Refused,
        Failure.Unreachable,
        Failure.TlsFailure,
        Failure.HttpFailure,
        Failure.Other {

        String message();

        record DnsFailure(String message) implements Failure {}
        record Timeout(String message) implements Failure {}
        record Refused(String message) implements Failure {}
        record Unreachable(String message) implements Failure {}
        record TlsFailure(String message) implements Failure {}
        record HttpFailure(String message, int statusCode) implements Failure {}
        record Other(String message) implements Failure {}
    }
}
