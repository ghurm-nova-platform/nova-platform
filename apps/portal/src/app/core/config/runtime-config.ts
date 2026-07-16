/**
 * Runtime configuration loaded from `/runtime-config.json`.
 * API keys must be supplied here (or via deployment mounts), never hardcoded.
 */
export interface RuntimeConfig {
  platformApiUrl: string;
  agentRuntimeUrl: string;
  /** Optional internal API key; empty means unauthenticated local calls. */
  apiKey: string;
}

export const DEFAULT_RUNTIME_CONFIG: RuntimeConfig = {
  platformApiUrl: 'http://localhost:8080',
  agentRuntimeUrl: 'http://localhost:8090',
  apiKey: '',
};
