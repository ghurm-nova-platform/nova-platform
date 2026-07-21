package ai.nova.platform.identity.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.identity")
public class IdentityProperties {

    private boolean enabled = true;
    private Duration sessionIdleTimeout = Duration.ofMinutes(30);
    private Duration sessionAbsoluteTimeout = Duration.ofDays(1);
    private int maxConcurrentSessions = 5;
    private final Jwt jwt = new Jwt();
    private final Refresh refresh = new Refresh();
    private final Password password = new Password();
    private final Mfa mfa = new Mfa();
    private final Ldap ldap = new Ldap();
    private final Ad ad = new Ad();
    private final Oidc oidc = new Oidc();
    private final OAuth oauth = new OAuth();
    private final Saml saml = new Saml();
    private final Scim scim = new Scim();
    private final Sync sync = new Sync();
    private final Session session = new Session();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getSessionIdleTimeout() {
        return sessionIdleTimeout;
    }

    public void setSessionIdleTimeout(Duration sessionIdleTimeout) {
        this.sessionIdleTimeout = sessionIdleTimeout;
    }

    public Duration getSessionAbsoluteTimeout() {
        return sessionAbsoluteTimeout;
    }

    public void setSessionAbsoluteTimeout(Duration sessionAbsoluteTimeout) {
        this.sessionAbsoluteTimeout = sessionAbsoluteTimeout;
    }

    public int getMaxConcurrentSessions() {
        return maxConcurrentSessions;
    }

    public void setMaxConcurrentSessions(int maxConcurrentSessions) {
        this.maxConcurrentSessions = maxConcurrentSessions;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Refresh getRefresh() {
        return refresh;
    }

    public Password getPassword() {
        return password;
    }

    public Mfa getMfa() {
        return mfa;
    }

    public Ldap getLdap() {
        return ldap;
    }

    public Ad getAd() {
        return ad;
    }

    public Oidc getOidc() {
        return oidc;
    }

    public OAuth getOauth() {
        return oauth;
    }

    public Saml getSaml() {
        return saml;
    }

    public Scim getScim() {
        return scim;
    }

    public Sync getSync() {
        return sync;
    }

    public Session getSession() {
        return session;
    }

    public static class Jwt {
        private boolean enabled = true;
        private Duration expiration = Duration.ofMinutes(15);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getExpiration() {
            return expiration;
        }

        public void setExpiration(Duration expiration) {
            this.expiration = expiration;
        }
    }

    public static class Refresh {
        private Duration expiration = Duration.ofDays(7);

        public Duration getExpiration() {
            return expiration;
        }

        public void setExpiration(Duration expiration) {
            this.expiration = expiration;
        }
    }

    public static class Password {
        private boolean enabled = true;
        private int minLength = 12;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSpecial = true;
        private int historyCount = 5;
        private int maxAgeDays = 0;
        private boolean temporaryPasswordForcesChange = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public boolean isRequireUppercase() {
            return requireUppercase;
        }

        public void setRequireUppercase(boolean requireUppercase) {
            this.requireUppercase = requireUppercase;
        }

        public boolean isRequireLowercase() {
            return requireLowercase;
        }

        public void setRequireLowercase(boolean requireLowercase) {
            this.requireLowercase = requireLowercase;
        }

        public boolean isRequireDigit() {
            return requireDigit;
        }

        public void setRequireDigit(boolean requireDigit) {
            this.requireDigit = requireDigit;
        }

        public boolean isRequireSpecial() {
            return requireSpecial;
        }

        public void setRequireSpecial(boolean requireSpecial) {
            this.requireSpecial = requireSpecial;
        }

        public int getHistoryCount() {
            return historyCount;
        }

        public void setHistoryCount(int historyCount) {
            this.historyCount = historyCount;
        }

        public int getMaxAgeDays() {
            return maxAgeDays;
        }

        public void setMaxAgeDays(int maxAgeDays) {
            this.maxAgeDays = maxAgeDays;
        }

        public boolean isTemporaryPasswordForcesChange() {
            return temporaryPasswordForcesChange;
        }

        public void setTemporaryPasswordForcesChange(boolean temporaryPasswordForcesChange) {
            this.temporaryPasswordForcesChange = temporaryPasswordForcesChange;
        }
    }

    public static class Mfa {
        private boolean enabled = true;
        private String issuer = "Nova";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class Ldap {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Ad {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Oidc {
        private boolean enabled = false;
        private boolean skipJwksVerify = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isSkipJwksVerify() {
            return skipJwksVerify;
        }

        public void setSkipJwksVerify(boolean skipJwksVerify) {
            this.skipJwksVerify = skipJwksVerify;
        }
    }

    public static class OAuth {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Saml {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Scim {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Sync {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Session {
        private Duration timeout = Duration.ofMinutes(30);

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
}
