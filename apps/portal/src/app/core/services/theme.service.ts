import { Injectable, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';

import { ColorScheme } from '../models/ui';

const STORAGE_KEY = 'nova.portal.theme';

/**
 * Light/dark theme foundation using Material system color-scheme.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly schemeSignal = signal<ColorScheme>(this.readInitialScheme());

  readonly scheme = this.schemeSignal.asReadonly();

  constructor() {
    this.apply(this.schemeSignal());
  }

  setScheme(scheme: ColorScheme): void {
    this.schemeSignal.set(scheme);
    this.apply(scheme);
    localStorage.setItem(STORAGE_KEY, scheme);
  }

  toggleScheme(): void {
    this.setScheme(this.schemeSignal() === 'light' ? 'dark' : 'light');
  }

  private readInitialScheme(): ColorScheme {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === 'dark' ? 'dark' : 'light';
  }

  private apply(scheme: ColorScheme): void {
    this.document.body.style.colorScheme = scheme;
    this.document.body.dataset['theme'] = scheme;
  }
}
