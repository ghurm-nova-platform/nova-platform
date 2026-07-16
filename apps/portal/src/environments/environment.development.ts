export type AppLocale = 'en' | 'ar';
export type TextDirection = 'ltr' | 'rtl';
export type ColorScheme = 'light' | 'dark';

/**
 * Development build-time defaults.
 * Secrets are never stored here; API keys load from runtime configuration.
 */
export const environment = {
  production: false,
  platformApiUrl: 'http://localhost:8080',
  agentRuntimeUrl: 'http://localhost:8090',
};
