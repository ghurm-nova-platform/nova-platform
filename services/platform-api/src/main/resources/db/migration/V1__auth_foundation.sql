CREATE TABLE organizations (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    email           VARCHAR(320) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_users_org_email UNIQUE (organization_id, email)
);

CREATE TABLE roles (
    id              UUID PRIMARY KEY,
    code            VARCHAR(100) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_roles_code UNIQUE (code)
);

CREATE TABLE permissions (
    id              UUID PRIMARY KEY,
    code            VARCHAR(150) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

CREATE TABLE user_roles (
    user_id         UUID NOT NULL,
    role_id         UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE role_permissions (
    role_id         UUID NOT NULL,
    permission_id   UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    token_hash      VARCHAR(128) NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_users_organization_id ON users (organization_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
