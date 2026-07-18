package ai.nova.platform.modelgateway.provider.http;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Production / non-test: localhost provider endpoints are never allowed. */
@Component
@Profile("!test")
public class DenyLocalhostEndpointOverrideGate implements LocalhostEndpointOverrideGate {

    @Override
    public boolean allowsLocalhostOverrides() {
        return false;
    }
}
