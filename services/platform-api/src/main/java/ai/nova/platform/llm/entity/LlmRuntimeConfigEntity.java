package ai.nova.platform.llm.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "llm_runtime_config")
public class LlmRuntimeConfigEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "config_key", nullable = false, length = 120)
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LlmRuntimeConfigEntity() {
    }

    public LlmRuntimeConfigEntity(UUID id, UUID organizationId, String configKey, String configValue, Instant now) {
        this.id = id;
        this.organizationId = organizationId;
        this.configKey = configKey;
        this.configValue = configValue;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getConfigKey() { return configKey; }
    public String getConfigValue() { return configValue; }
    public int getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public void touch(Instant now) { this.updatedAt = now; }
}
