# Database ER — Auth, Organizations, Projects, Agents, Prompts

```mermaid
erDiagram
  ORGANIZATIONS ||--o{ USERS : has
  ORGANIZATIONS ||--o{ PROJECTS : owns
  USERS ||--o{ USER_ROLES : has
  ROLES ||--o{ USER_ROLES : grants
  ROLES ||--o{ ROLE_PERMISSIONS : includes
  PERMISSIONS ||--o{ ROLE_PERMISSIONS : granted_by
  USERS ||--o{ REFRESH_TOKENS : issues
  USERS ||--o{ PROJECTS : creates

  ORGANIZATIONS {
    uuid id PK
    string name UK
    string slug UK
    timestamptz created_at
    timestamptz updated_at
    uuid created_by
    uuid updated_by
  }

  USERS {
    uuid id PK
    uuid organization_id FK
    string email
    string password_hash
    string display_name
    boolean enabled
  }

  ROLES {
    uuid id PK
    string code UK
    string name
  }

  PERMISSIONS {
    uuid id PK
    string code UK
    string name
  }

  PROJECTS ||--o{ AGENTS : owns
  PROJECTS ||--o{ PROMPTS : owns
  PROJECTS ||--o{ AGENT_EXECUTIONS : runs
  USERS ||--o{ AGENTS : audits
  AGENTS ||--o{ AGENT_AUDIT_LOG : history
  AGENTS ||--o{ AGENT_EXECUTIONS : executes
  PROMPTS ||--o{ PROMPT_VERSIONS : versions
  PROMPTS ||--o{ PROMPT_TAGS : tagged
  PROMPT_VERSIONS ||--o{ PROMPT_VARIABLES : defines
  PROMPT_VERSIONS ||--o{ AGENT_EXECUTIONS : used_by
  PROMPTS ||--o{ PROMPT_AUDIT_LOG : history
  PROMPTS ||--o{ AGENTS : referenced_by
  PROMPT_VERSIONS ||--o{ AGENTS : referenced_by
  AGENT_EXECUTIONS ||--o{ EXECUTION_MESSAGES : contains
  AGENT_EXECUTIONS ||--o{ EXECUTION_METRICS : records

  PROJECTS {
    uuid id PK
    uuid organization_id FK
    string name
    string description
    string status
    string visibility
    timestamptz created_at
    timestamptz updated_at
    uuid created_by
    uuid updated_by
  }

  AGENTS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    string name
    text system_prompt
    uuid prompt_id FK
    uuid prompt_version_id FK
    string model_provider
    string model_name
    decimal temperature
    int max_tokens
    string status
    string visibility
    int version
    uuid created_by
    uuid updated_by
  }

  AGENT_AUDIT_LOG {
    uuid id PK
    uuid agent_id FK
    uuid organization_id FK
    uuid project_id FK
    string action
    text old_value
    text new_value
    uuid performed_by
    timestamptz performed_at
  }

  PROMPTS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    string name
    string description
    string prompt_type
    string status
    uuid current_draft_version_id FK
    uuid published_version_id FK
    int version
    uuid created_by
    uuid updated_by
  }

  PROMPT_VERSIONS {
    uuid id PK
    uuid prompt_id FK
    uuid organization_id FK
    uuid project_id FK
    int version_number
    text content
    string change_summary
    string status
    uuid created_by
    uuid published_by
  }

  PROMPT_VARIABLES {
    uuid id PK
    uuid prompt_version_id FK
    string name
    string data_type
    boolean required_flag
  }

  PROMPT_TAGS {
    uuid id PK
    uuid prompt_id FK
    string tag_name
  }

  PROMPT_AUDIT_LOG {
    uuid id PK
    uuid prompt_id FK
    uuid prompt_version_id FK
    uuid organization_id FK
    uuid project_id FK
    string action
    uuid performed_by
    string correlation_id
  }

  AGENT_EXECUTIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid agent_id FK
    uuid prompt_version_id FK
    uuid conversation_id
    string provider
    string model
    string status
    int input_tokens
    int output_tokens
    int total_tokens
    int latency_ms
    uuid created_by
  }

  EXECUTION_MESSAGES {
    uuid id PK
    uuid execution_id FK
    string role
    text content
  }

  EXECUTION_METRICS {
    uuid id PK
    uuid execution_id FK
    string metric_name
    string metric_value
  }

  REFRESH_TOKENS {
    uuid id PK
    uuid user_id FK
    string token_hash UK
    timestamptz expires_at
    timestamptz revoked_at
  }
```

Unique constraints:

- `organizations.name`, `organizations.slug`
- `projects (organization_id, name)`
- `agents (project_id, name)`
- `prompts (project_id, name)`
- `prompt_versions (prompt_id, version_number)`
- `prompt_variables (prompt_version_id, name)`
- `prompt_tags (prompt_id, tag_name)`
- `execution_metrics (execution_id, metric_name)`
