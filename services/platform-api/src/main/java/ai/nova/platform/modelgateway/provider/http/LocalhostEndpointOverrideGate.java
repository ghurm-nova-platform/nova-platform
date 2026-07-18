package ai.nova.platform.modelgateway.provider.http;

/**
 * Controls whether MockWebServer / localhost endpoint overrides are permitted.
 * Production implementations must always deny; only the {@code test} profile may allow.
 */
public interface LocalhostEndpointOverrideGate {

    boolean allowsLocalhostOverrides();
}
