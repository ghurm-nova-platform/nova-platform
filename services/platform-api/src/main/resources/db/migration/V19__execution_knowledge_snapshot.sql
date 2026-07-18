-- Bounded execution knowledge snapshots for tool-approval continuation.
-- Stores citation metadata and bounded chunk content only (no embeddings).

CREATE TABLE execution_knowledge_snapshots (
    execution_id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    snapshot_json TEXT NOT NULL,
    citation_count INTEGER NOT NULL,
    total_characters INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_eks_execution FOREIGN KEY (execution_id) REFERENCES agent_executions (id) ON DELETE CASCADE,
    CONSTRAINT fk_eks_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_eks_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT chk_eks_citation_count CHECK (citation_count >= 0),
    CONSTRAINT chk_eks_total_characters CHECK (total_characters >= 0)
);

CREATE INDEX idx_eks_organization_id ON execution_knowledge_snapshots (organization_id);
CREATE INDEX idx_eks_project_id ON execution_knowledge_snapshots (project_id);
