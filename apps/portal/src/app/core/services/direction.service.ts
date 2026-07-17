import { Injectable, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';

import { AppLocale, TextDirection } from '../models/ui';

const STORAGE_KEY = 'nova.portal.locale';

/**
 * Manages English LTR and Arabic RTL document direction.
 */
@Injectable({ providedIn: 'root' })
export class DirectionService {
  private readonly document = inject(DOCUMENT);
  private readonly localeSignal = signal<AppLocale>(this.readInitialLocale());

  readonly locale = this.localeSignal.asReadonly();

  constructor() {
    this.apply(this.localeSignal());
  }

  direction(): TextDirection {
    return this.localeSignal() === 'ar' ? 'rtl' : 'ltr';
  }

  setLocale(locale: AppLocale): void {
    this.localeSignal.set(locale);
    this.apply(locale);
    localStorage.setItem(STORAGE_KEY, locale);
  }

  toggleLocale(): void {
    this.setLocale(this.localeSignal() === 'en' ? 'ar' : 'en');
  }

  private readInitialLocale(): AppLocale {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === 'ar' ? 'ar' : 'en';
  }

  private apply(locale: AppLocale): void {
    const dir: TextDirection = locale === 'ar' ? 'rtl' : 'ltr';
    this.document.documentElement.lang = locale;
    this.document.documentElement.dir = dir;
    this.document.body.dir = dir;
  }
}
