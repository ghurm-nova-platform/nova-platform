-- Optional published-prompt references for agents.
-- system_prompt remains required for backward compatibility and will be deprecated later.

ALTER TABLE agents
    ADD COLUMN prompt_id UUID;

ALTER TABLE agents
    ADD COLUMN prompt_version_id UUID;

ALTER TABLE agents
    ADD CONSTRAINT fk_agents_prompt
        FOREIGN KEY (prompt_id) REFERENCES prompts (id);

ALTER TABLE agents
    ADD CONSTRAINT fk_agents_prompt_version
        FOREIGN KEY (prompt_version_id) REFERENCES prompt_versions (id);

CREATE INDEX idx_agents_prompt_id ON agents (prompt_id);
CREATE INDEX idx_agents_prompt_version_id ON agents (prompt_version_id);
