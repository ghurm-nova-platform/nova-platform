-- Planner templates and planner permissions (Sprint 2 Phase 2).

CREATE TABLE planner_templates (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    template_type VARCHAR(50) NOT NULL,
    system_prompt TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_planner_templates_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_planner_templates_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT chk_planner_templates_type CHECK (template_type IN (
        'DEFAULT', 'SOFTWARE', 'RESEARCH', 'OPERATIONS', 'CUSTOM'
    )),
    CONSTRAINT uq_planner_templates_org_name UNIQUE (organization_id, name)
);

CREATE INDEX idx_planner_templates_org ON planner_templates (organization_id);
CREATE INDEX idx_planner_templates_org_project ON planner_templates (organization_id, project_id);
CREATE INDEX idx_planner_templates_enabled ON planner_templates (organization_id, enabled);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331041', 'PLANNER_PLAN', 'Generate execution plans', 'Invoke the planner agent to produce execution plans', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331042', 'PLANNER_IMPORT', 'Import planner plans', 'Import planner output into draft orchestration runs', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331043', 'PLANNER_TEMPLATE_READ', 'Read planner templates', 'View planner templates', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331044', 'PLANNER_TEMPLATE_MANAGE', 'Manage planner templates', 'Create and update planner templates', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    -- ORG_ADMIN: all
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331041'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331042'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331043'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331044'),
    -- PROJECT_ADMIN: plan/import/read templates
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331041'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331042'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331043'),
    -- USER + ORG_MEMBER: plan + read templates
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331041'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331043'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331041'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331043');

-- Default org-level planning template (Nova demo org).
INSERT INTO planner_templates (
    id, organization_id, project_id, name, description, template_type, system_prompt, enabled, created_at, updated_at
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa301',
    '11111111-1111-1111-1111-111111111111',
    NULL,
    'Default software planner',
    'Default template for software delivery planning',
    'SOFTWARE',
    'You are the Nova Planner Agent. Produce a complete execution plan as JSON only. Never execute tools, shell, git, browser, or MCP. Break the objective into tasks with agentRole values from the allowed role vocabulary. Use DEPENDENCY_GRAPH or SEQUENTIAL execution modes. Return only valid JSON matching the schema.',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
