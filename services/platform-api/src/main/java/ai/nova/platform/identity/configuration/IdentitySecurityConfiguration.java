package ai.nova.platform.identity.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * Identity-module security wiring. HTTP filter chain remains in
 * {@link ai.nova.platform.security.SecurityConfig}; identity properties are
 * registered via {@link IdentityProperties} {@code @Component}.
 */
@Configuration
public class IdentitySecurityConfiguration {
}
