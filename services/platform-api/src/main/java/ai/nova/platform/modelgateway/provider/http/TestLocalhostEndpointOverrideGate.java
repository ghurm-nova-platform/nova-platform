package ai.nova.platform.modelgateway.provider.http;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Test profile only: permits MockWebServer on localhost / 127.0.0.1.
 * This bean is not on the production classpath profile path.
 */
@Component
@Profile("test")
public class TestLocalhostEndpointOverrideGate implements LocalhostEndpointOverrideGate {

    @Override
    public boolean allowsLocalhostOverrides() {
        return true;
    }
}
