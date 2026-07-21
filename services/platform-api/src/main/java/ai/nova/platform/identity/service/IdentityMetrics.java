package ai.nova.platform.identity.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class IdentityMetrics {

    private final Counter successfulLogins;
    private final Counter failedLogins;
    private final Counter jwtIssued;
    private final Counter refreshTokensIssued;
    private final Counter mfaUsage;
    private final Counter syncJobs;
    private final Counter providerAvailabilityChecks;
    private final Timer loginLatency;
    private final AtomicLong activeSessions = new AtomicLong();

    public IdentityMetrics(MeterRegistry meterRegistry) {
        this.successfulLogins = Counter.builder("nova.identity.logins.success")
                .description("Successful authentication attempts")
                .register(meterRegistry);
        this.failedLogins = Counter.builder("nova.identity.logins.failure")
                .description("Failed authentication attempts")
                .register(meterRegistry);
        this.jwtIssued = Counter.builder("nova.identity.jwt.issued")
                .description("JWT access tokens issued")
                .register(meterRegistry);
        this.refreshTokensIssued = Counter.builder("nova.identity.refresh.issued")
                .description("Refresh tokens issued")
                .register(meterRegistry);
        this.mfaUsage = Counter.builder("nova.identity.mfa.usage")
                .description("MFA enroll or verify operations")
                .register(meterRegistry);
        this.syncJobs = Counter.builder("nova.identity.sync.jobs")
                .description("Identity provider synchronization jobs")
                .register(meterRegistry);
        this.providerAvailabilityChecks = Counter.builder("nova.identity.provider.availability")
                .description("Identity provider availability checks")
                .register(meterRegistry);
        this.loginLatency = Timer.builder("nova.identity.login.latency")
                .description("Authentication latency")
                .register(meterRegistry);
        Gauge.builder("nova.identity.sessions.active", activeSessions, AtomicLong::get)
                .description("Approximate active session counter")
                .register(meterRegistry);
    }

    public void recordLoginSuccess(long durationMs) {
        successfulLogins.increment();
        loginLatency.record(java.time.Duration.ofMillis(Math.max(0, durationMs)));
    }

    public void recordLoginFailure() {
        failedLogins.increment();
    }

    public void recordJwtIssued() {
        jwtIssued.increment();
    }

    public void recordRefreshIssued() {
        refreshTokensIssued.increment();
    }

    public void recordMfaUsage() {
        mfaUsage.increment();
    }

    public void recordSyncJob() {
        syncJobs.increment();
    }

    public void recordProviderCheck() {
        providerAvailabilityChecks.increment();
    }

    public void sessionCreated() {
        activeSessions.incrementAndGet();
    }

    public void sessionEnded() {
        activeSessions.updateAndGet(value -> Math.max(0, value - 1));
    }
}
