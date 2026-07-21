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

  AI_PROVIDERS {
    uuid id PK
    uuid organization_id FK
    string provider_key
    string provider_type
    string adapter_key
    string credential_reference
    string endpoint_profile
    string azure_resource_name
    string azure_api_version
    string last_connection_test_status
    string last_model_sync_status
    string status
  }

  PROVIDER_SECRETS {
    uuid id PK
    uuid organization_id FK
    string secret_key
    string provider_type
    string status
    bytes ciphertext
    bytes nonce
    string algorithm
    string fingerprint_sha256
    string last4
  }

  AI_MODELS {
    uuid id PK
    uuid organization_id FK
    uuid provider_id FK
    string model_key
    string provider_model_id
    string model_type
    string status
    string source
    int context_window
    datetime last_synced_at
  }

  AI_MODEL_CAPABILITIES {
    uuid model_id FK
    string capability
    boolean enabled
  }

  AI_MODEL_ALIASES {
    uuid id PK
    uuid organization_id FK
    uuid model_id FK
    string alias
    string normalized_alias
  }

  AGENT_ORCHESTRATION_RUNS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    string status
    string execution_mode
    string failure_policy
    bigint event_sequence
  }

  AGENT_ORCHESTRATION_TASKS {
    uuid id PK
    uuid run_id FK
    string task_key
    string task_type
    string status
    string idempotency_key
  }

  AGENT_TASK_DEPENDENCIES {
    uuid predecessor_task_id FK
    uuid successor_task_id FK
    string dependency_type
  }

  AGENT_TASK_ATTEMPTS {
    uuid id PK
    uuid task_id FK
    int attempt_number
    string status
  }

  AGENT_ORCHESTRATION_EVENTS {
    uuid id PK
    uuid run_id FK
    bigint event_sequence
    string event_type
  }

  GENERATED_ARTIFACTS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid run_id FK
    uuid task_id FK
    string artifact_type
    string language
    string path
    string filename
    string sha256
  }

  REVIEW_RESULTS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid run_id FK
    uuid task_id FK
    string summary
    int score
    boolean approved
  }

  REVIEW_FINDINGS {
    uuid id PK
    uuid review_result_id FK
    string severity
    string category
    string title
  }

  REVIEWED_ARTIFACTS {
    uuid id PK
    uuid review_result_id FK
    uuid artifact_id FK
    string path
    string sha256
  }

  TESTING_RESULTS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid run_id FK
    uuid task_id FK
    string summary
    int coverage_estimate
  }

  GENERATED_TESTS {
    uuid id PK
    uuid testing_result_id FK
    string test_type
    string priority
    string title
  }

  GENERATED_TEST_CASES {
    uuid id PK
    uuid testing_result_id FK
    uuid generated_test_id FK
    string name
    string priority
  }

  TESTING_REVIEWED_ARTIFACTS {
    uuid id PK
    uuid testing_result_id FK
    uuid artifact_id FK
    string path
    string sha256
  }

  PATCH_RESULTS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid run_id FK
    uuid task_id FK
    string summary
    string status
    int files_changed
    int insertions
    int deletions
    int patch_size
  }

  GENERATED_PATCHES {
    uuid id PK
    uuid patch_result_id FK
    string path
    string change_type
    int insertions
    int deletions
  }

  PATCH_ARTIFACTS {
    uuid id PK
    uuid patch_result_id FK
    uuid artifact_id FK
    string path
    string sha256
  }

  GIT_OPERATIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid run_id FK
    uuid task_id FK
    uuid patch_result_id
    string status
    string branch_name
    string commit_hash
    string patch_hash
  }

  GIT_BRANCHES {
    uuid id PK
    uuid git_operation_id FK
    string branch_name
    string base_ref
  }

  GIT_COMMITS {
    uuid id PK
    uuid git_operation_id FK
    string commit_hash
    string message
  }

  PROJECT_REPOSITORY_CONFIGS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    string provider
    string repository_host
    string repository_owner
    string repository_name
    string remote_url
    string target_base_ref
    boolean enabled
  }

  PULL_REQUEST_OPERATIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid task_id FK
    uuid git_operation_id FK
    string status
    string provider
    string source_branch
    string target_branch
    string local_commit_hash
    string remote_commit_hash
    string patch_hash
    bigint pull_request_number
    string pull_request_url
    string error_code
  }

  REMOTE_PUSHES {
    uuid id PK
    uuid pull_request_operation_id FK
    string remote_name
    string source_branch
    string local_commit_hash
    string remote_commit_hash
    string status
  }

  PULL_REQUEST_RECORDS {
    uuid id PK
    uuid pull_request_operation_id FK
    string provider
    bigint pull_request_number
    string pull_request_url
    string title
    string source_branch
    string target_branch
    string state
  }

  CI_OBSERVATION_OPERATIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid task_id FK
    uuid pull_request_operation_id FK
    string status
    string provider
    string repository_owner
    string repository_name
    string source_branch
    string target_branch
    string commit_hash
    bigint pull_request_number
    string overall_status
    string failure_summary
    string retry_recommendation
    string error_code
  }

  CI_WORKFLOW_RUNS {
    uuid id PK
    uuid ci_observation_operation_id FK
    string external_workflow_id
    string workflow_name
    string external_run_id
    string run_url
    string status
    string conclusion
    bigint duration_ms
    string trigger_event
    string failure_reason
  }

  CI_JOBS {
    uuid id PK
    uuid ci_workflow_run_id FK
    string external_job_id
    string job_name
    string status
    string conclusion
    bigint duration_ms
    string failure_reason
  }

  CI_STEPS {
    uuid id PK
    uuid ci_job_id FK
    int step_number
    string step_name
    string status
    string conclusion
    bigint duration_ms
    string failure_reason
  }

  REPAIR_OPERATIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid task_id FK
    string status
    int attempt_number
    uuid prior_patch_result_id FK
    uuid new_patch_result_id FK
    string reason
    string summary
    double confidence
    string input_fingerprint
    string error_code
  }

  REPAIR_INPUTS {
    uuid id PK
    uuid repair_operation_id FK
    string source_type
    string source_ref
    int priority
    string detail
  }

  REPAIR_ACTIONS {
    uuid id PK
    uuid repair_operation_id FK
    string action_type
    string target_path
    string description
  }

  REPAIR_RESULTS {
    uuid id PK
    uuid repair_operation_id FK
    uuid patch_result_id FK
    string repaired_files_json
    string summary
    double confidence
  }

  APPROVAL_POLICIES {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    string name
    int version
    string status
    boolean is_default
    int required_human_approvals
    boolean require_ci_success
    boolean require_review_approved
    int minimum_review_score
    boolean require_testing_success
    int minimum_estimated_coverage
    int decision_validity_minutes
  }

  APPROVAL_POLICY_RULES {
    uuid id PK
    uuid approval_policy_id FK
    string rule_code
    string rule_type
    string operator
    string expected_value
    string severity
    boolean blocking
    int display_order
  }

  APPROVAL_GATE_OPERATIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid task_id FK
    uuid policy_id FK
    int policy_version
    string status
    string decision
    string error_code
  }

  APPROVAL_DECISIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid task_id FK
    uuid approval_gate_operation_id FK
    uuid policy_id FK
    string decision
    boolean eligible_for_merge
    int required_human_approvals
    int received_human_approvals
    int rejection_count
    string evidence_fingerprint
    string decision_fingerprint
    uuid patch_result_id FK
    string patch_hash
    uuid git_operation_id FK
    string commit_hash
    uuid pull_request_operation_id FK
    bigint pull_request_number
    string pull_request_url
    uuid ci_observation_operation_id FK
    string ci_overall_status
    uuid repair_operation_id FK
    string reason_summary
    timestamp valid_until
  }

  APPROVAL_EVIDENCE {
    uuid id PK
    uuid approval_decision_id FK
    string evidence_type
    uuid source_operation_id
    uuid source_result_id
    string source_hash
    string observed_status
    string observed_value
  }

  APPROVAL_REQUIREMENTS {
    uuid id PK
    uuid approval_decision_id FK
    string rule_code
    string result
    boolean blocking
    string severity
    string failure_reason
  }

  APPROVAL_HUMAN_ACTIONS {
    uuid id PK
    uuid approval_decision_id FK
    uuid actor_user_id FK
    string action
    string comment_text
    string evidence_fingerprint
    string idempotency_key
  }

  APPROVAL_DECISION_EVENTS {
    uuid id PK
    uuid approval_decision_id FK
    string event_type
    string detail
    uuid actor_user_id FK
  }

  MERGE_OPERATIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid task_id FK
    uuid approval_decision_id FK
    uuid pull_request_operation_id FK
    uuid git_operation_id FK
    uuid patch_result_id FK
    string status
    string merge_method
    string evidence_fingerprint
    string decision_fingerprint
    string expected_patch_hash
    string expected_commit_hash
    string expected_pr_head_sha
    bigint pull_request_number
    string repository_owner
    string repository_name
    string error_code
  }

  MERGE_VALIDATIONS {
    uuid id PK
    uuid merge_operation_id FK
    string check_code
    string expected_value
    string actual_value
    string result
    string failure_reason
  }

  MERGE_RESULTS {
    uuid id PK
    uuid merge_operation_id FK
    string merge_method
    string merged_commit
    bigint pull_request_number
    string pull_request_url
    string provider
    uuid merged_by_user_id FK
  }

  MERGE_EVENTS {
    uuid id PK
    uuid merge_operation_id FK
    string event_type
    string detail
  }

  RELEASE_OPERATIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    bigint release_number
    string semantic_version
    string release_name
    string status
    string content_fingerprint
    string manifest_hash
    uuid created_by FK
  }

  RELEASE_VERSIONS {
    uuid id PK
    uuid release_operation_id FK
    string semantic_version
    string version_strategy
    string bump_type
    int major_version
    int minor_version
    int patch_version
  }

  RELEASE_CONTENTS {
    uuid id PK
    uuid release_operation_id FK
    string content_type
    uuid reference_id
    string commit_sha
  }

  RELEASE_ARTIFACTS {
    uuid id PK
    uuid release_operation_id FK
    string artifact_type
    string artifact_uri
    string artifact_hash
  }

  RELEASE_EVENTS {
    uuid id PK
    uuid release_operation_id FK
    string event_type
    string detail
  }

  DEPLOYMENT_ENVIRONMENTS {
    uuid id PK
    string code
    string name
    string environment_type
  }

  DEPLOYMENT_OPERATIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid release_operation_id FK
    uuid environment_id FK
    string semantic_version
    string status
    string health
    string deployment_provider
    string deployment_hash
  }

  DEPLOYMENT_EVENTS {
    uuid id PK
    uuid deployment_operation_id FK
    string event_type
    string detail
  }

  DEPLOYMENT_HEALTH {
    uuid id PK
    uuid deployment_operation_id FK
    string health
    string message
  }

  DEPLOYMENT_ARTIFACTS {
    uuid id PK
    uuid deployment_operation_id FK
    string artifact_type
    string artifact_uri
  }

  ROLLBACK_OPERATIONS {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    uuid release_operation_id FK
    uuid deployment_operation_id FK
    uuid target_release_operation_id FK
    string current_version
    string target_version
    string environment_code
    string status
    string strategy
    string rollback_plan_hash
  }

  ROLLBACK_PLANS {
    uuid id PK
    uuid rollback_operation_id FK
    string strategy
    string risk_level
    string validation_result
    boolean immutable
  }

  ROLLBACK_TARGETS {
    uuid id PK
    uuid rollback_operation_id FK
    uuid target_release_operation_id FK
    string target_version
  }

  ROLLBACK_EVENTS {
    uuid id PK
    uuid rollback_operation_id FK
    string event_type
    string detail
  }

  ROLLBACK_VALIDATIONS {
    uuid id PK
    uuid rollback_operation_id FK
    string check_code
    boolean passed
  }

  RELEASE_POLICIES {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    string policy_name
    string policy_type
    string status
    int priority
    string evaluation_mode
    string policy_fingerprint
  }

  POLICY_VERSIONS {
    uuid id PK
    uuid policy_id FK
    int version_number
    string policy_type
    string config_json
  }

  POLICY_EVALUATIONS {
    uuid id PK
    uuid policy_id FK
    uuid policy_version_id FK
    uuid release_operation_id FK
    string decision
    string evaluation_hash
  }

  POLICY_EVIDENCE {
    uuid id PK
    uuid policy_evaluation_id FK
    string evidence_key
    boolean passed
  }

  POLICY_EVENTS {
    uuid id PK
    uuid policy_id FK
    string event_type
    string detail
  }

  PLANNER_TEMPLATES {
    uuid id PK
    uuid organization_id FK
    uuid project_id FK
    string name
    string template_type
    text system_prompt
    boolean enabled
  }

  PROJECT_MODELS {
    uuid id PK
    uuid project_id FK
    uuid model_id FK
    boolean enabled
    boolean is_default
  }

  AGENT_MODEL_ASSIGNMENTS {
    uuid id PK
    uuid agent_id FK
    uuid model_id FK
    string assignment_role
    int priority
  }

  MODEL_ROUTING_POLICIES {
    uuid id PK
    uuid project_id FK
    uuid agent_id FK
    string strategy
    string status
  }

  MODEL_INVOCATIONS {
    uuid id PK
    uuid execution_id FK
    uuid provider_id FK
    uuid model_id FK
    int attempt_number
    string status
  }

  MODEL_USAGE_DAILY {
    uuid id PK
    uuid project_id FK
    uuid model_id FK
    date usage_date
    bigint request_count
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
- `provider_secrets (organization_id, secret_key)`

Migrations: `V16__knowledge_base.sql`, `V17__knowledge_embeddings.sql`, `V18__knowledge_permissions_seed.sql`,
`V19__execution_knowledge_snapshot.sql`, `V20__ai_model_gateway.sql`, `V21__model_routing_and_usage.sql`,
`V22__model_gateway_permissions.sql`, `V23__provider_secret_vault.sql`,
`V24__provider_connection_metadata.sql`, `V25__provider_secret_permissions.sql`,
`V34__planner_templates.sql`, `V35__generated_artifacts.sql`, `V36__review_findings.sql`,
`V37__testing_results.sql`, `V38__patch_results.sql`, `V39__git_operations.sql`, `V40__pull_request_operations.sql`, `V41__ci_observation.sql`, `V42__repair_agent.sql`, `V43__approval_gate.sql`, `V44__merge_agent.sql`, `V45__release_manager.sql`, `V46__deployment_observation.sql`, `V47__rollback_manager.sql`, `V48__release_policies.sql`, `V49__environment_management.sql`, `V50__audit_center.sql`, `V51__audit_database_immutability` (Java migration), `V52__deployment_execution.sql`, `V54__dashboard_permissions.sql`.
See [`018_KNOWLEDGE_BASE_AND_RAG.md`](018_KNOWLEDGE_BASE_AND_RAG.md),
[`019_AI_MODEL_GATEWAY.md`](019_AI_MODEL_GATEWAY.md),
[`020_SECURE_PROVIDER_INTEGRATION.md`](020_SECURE_PROVIDER_INTEGRATION.md),
[`023_PLANNER_AGENT.md`](023_PLANNER_AGENT.md),
[`024_CODING_AGENT.md`](024_CODING_AGENT.md),
[`026_REVIEW_AGENT.md`](026_REVIEW_AGENT.md),
[`027_TESTING_AGENT.md`](027_TESTING_AGENT.md),
[`028_PATCH_AGENT.md`](028_PATCH_AGENT.md), and
[`029_GIT_INTEGRATION_AGENT.md`](029_GIT_INTEGRATION_AGENT.md),
[`030_PULL_REQUEST_AGENT.md`](030_PULL_REQUEST_AGENT.md),
[`031_CI_OBSERVATION_AGENT.md`](031_CI_OBSERVATION_AGENT.md),
[`032_REPAIR_AGENT.md`](032_REPAIR_AGENT.md),
[`033_APPROVAL_GATE.md`](033_APPROVAL_GATE.md),
[`034_MERGE_AGENT.md`](034_MERGE_AGENT.md), [`035_RELEASE_MANAGER.md`](035_RELEASE_MANAGER.md), [`036_DEPLOYMENT_OBSERVATION.md`](036_DEPLOYMENT_OBSERVATION.md), [`037_ROLLBACK_MANAGER.md`](037_ROLLBACK_MANAGER.md), [`038_RELEASE_POLICIES.md`](038_RELEASE_POLICIES.md), [`039_ENVIRONMENT_MANAGEMENT.md`](039_ENVIRONMENT_MANAGEMENT.md), [`040_AUDIT_CENTER.md`](040_AUDIT_CENTER.md).
