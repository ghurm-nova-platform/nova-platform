import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { DEFAULT_RUNTIME_CONFIG, RuntimeConfig } from './runtime-config';

/**
 * Loads deployment-time browser configuration.
 * Does not load or expose internal API keys.
 */
@Injectable({ providedIn: 'root' })
export class RuntimeConfigService {
  private readonly http = inject(HttpClient);
  private readonly configSignal = signal<RuntimeConfig>({
    ...DEFAULT_RUNTIME_CONFIG,
    platformApiUrl: environment.platformApiUrl,
  });

  readonly config = this.configSignal.asReadonly();

  async load(): Promise<void> {
    try {
      const remote = await firstValueFrom(this.http.get<Partial<RuntimeConfig>>('/runtime-config.json'));
      this.configSignal.set({
        platformApiUrl: remote.platformApiUrl ?? environment.platformApiUrl,
      });
    } catch {
      this.configSignal.set({
        platformApiUrl: environment.platformApiUrl,
      });
    }
  }

  platformApiUrl(): string {
    return this.configSignal().platformApiUrl;
  }
}
