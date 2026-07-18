package ai.nova.platform.modelgateway.secrets.vault;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.secrets")
public class SecretsProperties {

    /**
     * Base64-encoded 32-byte AES-256 master key. Empty means vault encrypt/decrypt will fail
     * with a clear error until {@code NOVA_SECRET_MASTER_KEY} is configured.
     */
    private String masterKey = "";

    public String getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }
}
