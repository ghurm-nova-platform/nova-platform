/**
 * Runtime configuration loaded from `/runtime-config.json`.
 *
 * Browser clients must never receive internal service credentials.
 * Authentication will use a future user session or OAuth/OIDC access token.
 */
export interface RuntimeConfig {
  /** Base URL of the Platform API / BFF. */
  platformApiUrl: string;
}

export const DEFAULT_RUNTIME_CONFIG: RuntimeConfig = {
  platformApiUrl: 'http://localhost:8080',
};
