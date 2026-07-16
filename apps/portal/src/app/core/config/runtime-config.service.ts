import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { DEFAULT_RUNTIME_CONFIG, RuntimeConfig } from './runtime-config';

/**
 * Loads deployment-time configuration, including the API-key placeholder.
 */
@Injectable({ providedIn: 'root' })
export class RuntimeConfigService {
  private readonly http = inject(HttpClient);
  private readonly configSignal = signal<RuntimeConfig>({
    ...DEFAULT_RUNTIME_CONFIG,
    platformApiUrl: environment.platformApiUrl,
    agentRuntimeUrl: environment.agentRuntimeUrl,
  });

  readonly config = this.configSignal.asReadonly();

  async load(): Promise<void> {
    try {
      const remote = await firstValueFrom(this.http.get<Partial<RuntimeConfig>>('/runtime-config.json'));
      this.configSignal.set({
        platformApiUrl: remote.platformApiUrl ?? environment.platformApiUrl,
        agentRuntimeUrl: remote.agentRuntimeUrl ?? environment.agentRuntimeUrl,
        apiKey: remote.apiKey ?? '',
      });
    } catch {
      // Keep build-time defaults when runtime config is unavailable.
      this.configSignal.set({
        platformApiUrl: environment.platformApiUrl,
        agentRuntimeUrl: environment.agentRuntimeUrl,
        apiKey: '',
      });
    }
  }

  apiKey(): string {
    return this.configSignal().apiKey;
  }

  platformApiUrl(): string {
    return this.configSignal().platformApiUrl;
  }

  agentRuntimeUrl(): string {
    return this.configSignal().agentRuntimeUrl;
  }
}
