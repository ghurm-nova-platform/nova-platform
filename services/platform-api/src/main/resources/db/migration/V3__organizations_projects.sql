-- Projects + organization audit columns and name uniqueness.

ALTER TABLE organizations ADD COLUMN created_by UUID;
ALTER TABLE organizations ADD COLUMN updated_by UUID;

ALTER TABLE organizations ADD CONSTRAINT uq_organizations_name UNIQUE (name);

UPDATE organizations
SET created_by = '44444444-4444-4444-4444-444444444401',
    updated_by = '44444444-4444-4444-4444-444444444401'
WHERE created_by IS NULL;

ALTER TABLE organizations ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE organizations ALTER COLUMN updated_by SET NOT NULL;

CREATE TABLE projects (
    id               UUID PRIMARY KEY,
    organization_id  UUID NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(2000),
    status           VARCHAR(32) NOT NULL,
    visibility       VARCHAR(32) NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       UUID NOT NULL,
    updated_by       UUID NOT NULL,
    CONSTRAINT fk_projects_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT uq_projects_org_name UNIQUE (organization_id, name),
    CONSTRAINT ck_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DRAFT')),
    CONSTRAINT ck_projects_visibility CHECK (visibility IN ('PRIVATE', 'INTERNAL', 'PUBLIC'))
);

CREATE INDEX idx_projects_organization_id ON projects (organization_id);
CREATE INDEX idx_projects_status ON projects (status);
CREATE INDEX idx_projects_name ON projects (name);
