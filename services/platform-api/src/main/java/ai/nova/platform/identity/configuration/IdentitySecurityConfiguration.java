package ai.nova.platform.identity.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Identity-module security wiring. HTTP filter chain remains in
 * {@link ai.nova.platform.security.SecurityConfig}; this configuration enables
 * identity properties and marks the Identity package as the authentication entry point.
 */
@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentitySecurityConfiguration {
}
