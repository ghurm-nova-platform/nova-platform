-- Provider secret vault: encrypted credentials (AES-256-GCM ciphertext only).

CREATE TABLE provider_secrets (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    secret_key VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    provider_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    ciphertext BYTEA NOT NULL,
    nonce BYTEA NOT NULL,
    key_version INTEGER NOT NULL DEFAULT 1,
    algorithm VARCHAR(50) NOT NULL DEFAULT 'AES-256-GCM',
    fingerprint_sha256 VARCHAR(64) NOT NULL,
    last4 VARCHAR(4),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    rotated_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_provider_secrets_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_provider_secrets_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_provider_secrets_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_provider_secrets_org_key UNIQUE (organization_id, secret_key),
    CONSTRAINT ck_provider_secrets_key_format
        CHECK (secret_key ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_provider_secrets_status
        CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'ARCHIVED')),
    CONSTRAINT ck_provider_secrets_provider_type
        CHECK (provider_type IN (
            'DETERMINISTIC_LOCAL',
            'OPENAI',
            'AZURE_OPENAI',
            'ANTHROPIC',
            'GOOGLE_GEMINI',
            'AWS_BEDROCK',
            'CUSTOM_MANAGED'
        )),
    CONSTRAINT ck_provider_secrets_algorithm
        CHECK (algorithm = 'AES-256-GCM'),
    CONSTRAINT ck_provider_secrets_key_version
        CHECK (key_version >= 1),
    CONSTRAINT ck_provider_secrets_fingerprint
        CHECK (LENGTH(fingerprint_sha256) = 64),
    CONSTRAINT ck_provider_secrets_last4
        CHECK (last4 IS NULL OR last4 ~ '^[A-Za-z0-9]{4}$'),
    CONSTRAINT ck_provider_secrets_deterministic_forbidden
        CHECK (provider_type <> 'DETERMINISTIC_LOCAL')
);

CREATE INDEX idx_provider_secrets_organization_id ON provider_secrets (organization_id);
CREATE INDEX idx_provider_secrets_status ON provider_secrets (status);
CREATE INDEX idx_provider_secrets_provider_type ON provider_secrets (provider_type);
CREATE INDEX idx_provider_secrets_created_at ON provider_secrets (created_at);
