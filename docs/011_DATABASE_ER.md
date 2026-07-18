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
  PROJECTS ||--o{ CONVERSATIONS : hosts
  USERS ||--o{ AGENTS : audits
  AGENTS ||--o{ AGENT_AUDIT_LOG : history
  AGENTS ||--o{ AGENT_EXECUTIONS : executes
  AGENTS ||--o{ CONVERSATIONS : chats
  PROMPTS ||--o{ PROMPT_VERSIONS : versions
  PROMPTS ||--o{ PROMPT_TAGS : tagged
  PROMPT_VERSIONS ||--o{ PROMPT_VARIABLES : defines
  PROMPT_VERSIONS ||--o{ AGENT_EXECUTIONS : used_by
  PROMPTS ||--o{ PROMPT_AUDIT_LOG : history
  PROMPTS ||--o{ AGENTS : referenced_by
  PROMPT_VERSIONS ||--o{ AGENTS : referenced_by
  AGENT_EXECUTIONS ||--o{ EXECUTION_MESSAGES : contains
  AGENT_EXECUTIONS ||--o{ EXECUTION_METRICS : records
  CONVERSATIONS ||--o{ CONVERSATION_MESSAGES : contains
  CONVERSATIONS ||--o{ CONVERSATION_AUDIT_LOG : history
  CONVERSATIONS ||--o{ CONVERSATION_EXECUTION_REQUESTS : idempotency
  AGENT_EXECUTIONS ||--o{ CONVERSATION_MESSAGES : may_link
  PROJECTS ||--o{ TOOLS : hosts
  TOOLS ||--o{ AGENT_TOOL_ASSIGNMENTS : assigned
  AGENTS ||--o{ AGENT_TOOL_ASSIGNMENTS : uses
  AGENT_EXECUTIONS ||--o{ EXECUTION_TOOL_CALLS : invokes
  TOOLS ||--o{ EXECUTION_TOOL_CALLS : called_as
  TOOLS ||--o{ TOOL_AUDIT_LOG : history

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

  CONVERSATIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid agent_id FK
    string title
    string status
    int message_count
    int version
    uuid created_by
    uuid updated_by
  }

  CONVERSATION_MESSAGES {
    uuid id PK
    uuid conversation_id FK
    uuid execution_id FK
    string role
    text content
    int sequence_number
    uuid client_request_id
  }

  CONVERSATION_AUDIT_LOG {
    uuid id PK
    uuid conversation_id FK
    uuid organization_id FK
    uuid project_id FK
    string action
    text metadata
    uuid performed_by
  }

  CONVERSATION_EXECUTION_REQUESTS {
    uuid id PK
    uuid conversation_id FK
    uuid client_request_id
    uuid execution_id FK
  }

  TOOLS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    string tool_key
    string executor_key
    string status
    boolean requires_approval
  }

  AGENT_TOOL_ASSIGNMENTS {
    uuid id PK
    uuid agent_id FK
    uuid tool_id FK
    boolean enabled
  }

  EXECUTION_TOOL_CALLS {
    uuid id PK
    uuid execution_id FK
    uuid tool_id FK
    string runtime_call_id
    int sequence_number
    string status
  }

  TOOL_AUDIT_LOG {
    uuid id PK
    uuid tool_id FK
    string action
    text metadata
  }

  KNOWLEDGE_BASES {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    string knowledge_key
    string status
    string embedding_provider_key
    string embedding_model
    int embedding_dimensions
  }

  KNOWLEDGE_DOCUMENTS {
    uuid id PK
    uuid knowledge_base_id FK
    string document_key
    string document_type
    string status
    string content_hash
  }

  KNOWLEDGE_DOCUMENT_CONTENT {
    uuid document_id PK
    text extracted_text
  }

  KNOWLEDGE_CHUNKS {
    uuid id PK
    uuid document_id FK
    int chunk_index
    text content
    string content_hash
  }

  KNOWLEDGE_EMBEDDINGS {
    uuid id PK
    uuid chunk_id FK
    string provider_key
    string model
    int dimensions
    text embedding
  }

  AGENT_KNOWLEDGE_ASSIGNMENTS {
    uuid id PK
    uuid agent_id FK
    uuid knowledge_base_id FK
    boolean enabled
  }

  KNOWLEDGE_RETRIEVAL_AUDIT {
    uuid id PK
    uuid knowledge_base_id FK
    string query_hash
    int returned_count
    bigint duration_ms
  }

  EXECUTION_KNOWLEDGE_SNAPSHOTS {
    uuid execution_id PK
    uuid organization_id FK
    uuid project_id FK
    text snapshot_json
    int citation_count
    int total_characters
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
- `conversation_messages (conversation_id, sequence_number)`
- `conversation_execution_requests (conversation_id, client_request_id)`
- `tools (organization_id, project_id, tool_key)`
- `agent_tool_assignments (agent_id, tool_id)`
- `execution_tool_calls (execution_id, runtime_call_id)`
- `execution_tool_calls (execution_id, sequence_number)`
- `knowledge_bases (project_id, knowledge_key)`
- `knowledge_documents (knowledge_base_id, document_key)`
- `knowledge_chunks (document_id, chunk_index)`
- `knowledge_embeddings (chunk_id, provider_key, model)`
- `agent_knowledge_assignments (agent_id, knowledge_base_id)`

Migrations: `V16__knowledge_base.sql`, `V17__knowledge_embeddings.sql`, `V18__knowledge_permissions_seed.sql`.
See [`018_KNOWLEDGE_BASE_AND_RAG.md`](018_KNOWLEDGE_BASE_AND_RAG.md).
