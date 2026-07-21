package ai.nova.platform.llm.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "llm_prompt_templates")
public class LlmPromptTemplateEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 120)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private LlmPromptCategory category;

    @Column(name = "system_prompt")
    private String systemPrompt;

    @Column(name = "user_prompt_template", nullable = false)
    private String userPromptTemplate;

    @Column(name = "assistant_prompt_template")
    private String assistantPromptTemplate;

    @Column(name = "variables_json", nullable = false)
    private String variablesJson;

    @Column(name = "template_version", nullable = false)
    private int templateVersion;

    @Column(nullable = false)
    private boolean enabled;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LlmPromptTemplateEntity() {
    }

    public LlmPromptTemplateEntity(
            UUID id,
            UUID organizationId,
            String code,
            String name,
            LlmPromptCategory category,
            String userPromptTemplate,
            Instant now) {
        this.id = id;
        this.organizationId = organizationId;
        this.code = code;
        this.name = name;
        this.category = category;
        this.userPromptTemplate = userPromptTemplate;
        this.variablesJson = "[]";
        this.templateVersion = 1;
        this.enabled = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public LlmPromptCategory getCategory() { return category; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getUserPromptTemplate() { return userPromptTemplate; }
    public String getAssistantPromptTemplate() { return assistantPromptTemplate; }
    public String getVariablesJson() { return variablesJson; }
    public int getTemplateVersion() { return templateVersion; }
    public boolean isEnabled() { return enabled; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setCategory(LlmPromptCategory category) { this.category = category; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public void setUserPromptTemplate(String userPromptTemplate) { this.userPromptTemplate = userPromptTemplate; }
    public void setAssistantPromptTemplate(String assistantPromptTemplate) { this.assistantPromptTemplate = assistantPromptTemplate; }
    public void setVariablesJson(String variablesJson) { this.variablesJson = variablesJson; }
    public void setTemplateVersion(int templateVersion) { this.templateVersion = templateVersion; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void touch(Instant now) { this.updatedAt = now; }
}
