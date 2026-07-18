-- Provider connection metadata for allowlisted OpenAI / Azure OpenAI endpoints.

ALTER TABLE ai_providers ADD COLUMN endpoint_profile VARCHAR(50);
ALTER TABLE ai_providers ADD COLUMN azure_resource_name VARCHAR(100);
ALTER TABLE ai_providers ADD COLUMN azure_api_version VARCHAR(50);
ALTER TABLE ai_providers ADD COLUMN last_connection_test_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ai_providers ADD COLUMN last_connection_test_status VARCHAR(30);
ALTER TABLE ai_providers ADD COLUMN last_connection_test_error_code VARCHAR(100);

ALTER TABLE ai_providers
    ADD CONSTRAINT ck_ai_providers_endpoint_profile
        CHECK (
            endpoint_profile IS NULL
            OR endpoint_profile IN ('OPENAI_PUBLIC', 'AZURE_OPENAI_RESOURCE')
        );

ALTER TABLE ai_providers
    ADD CONSTRAINT ck_ai_providers_azure_resource
        CHECK (
            azure_resource_name IS NULL
            OR azure_resource_name ~ '^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$'
        );

ALTER TABLE ai_providers
    ADD CONSTRAINT ck_ai_providers_connection_test_status
        CHECK (
            last_connection_test_status IS NULL
            OR last_connection_test_status IN ('NEVER', 'SUCCESS', 'FAILED')
        );

ALTER TABLE ai_providers
    ADD CONSTRAINT ck_ai_providers_openai_profile
        CHECK (
            provider_type <> 'OPENAI'
            OR endpoint_profile IS NULL
            OR endpoint_profile = 'OPENAI_PUBLIC'
        );

ALTER TABLE ai_providers
    ADD CONSTRAINT ck_ai_providers_azure_profile
        CHECK (
            provider_type <> 'AZURE_OPENAI'
            OR endpoint_profile IS NULL
            OR (
                endpoint_profile = 'AZURE_OPENAI_RESOURCE'
                AND azure_resource_name IS NOT NULL
                AND azure_api_version IS NOT NULL
            )
        );

UPDATE ai_providers
SET last_connection_test_status = 'NEVER'
WHERE last_connection_test_status IS NULL;

CREATE INDEX idx_ai_providers_endpoint_profile ON ai_providers (endpoint_profile);
CREATE INDEX idx_ai_providers_connection_test_status ON ai_providers (last_connection_test_status);
