import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/common';

import { DirectionService } from './direction.service';

describe('DirectionService', () => {
  let service: DirectionService;
  let documentRef: Document;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [DirectionService],
    });
    documentRef = TestBed.inject(DOCUMENT);
    service = TestBed.inject(DirectionService);
  });

  it('defaults to English LTR', () => {
    expect(service.locale()).toBe('en');
    expect(service.direction()).toBe('ltr');
    expect(documentRef.documentElement.dir).toBe('ltr');
    expect(documentRef.documentElement.lang).toBe('en');
  });

  it('switches to Arabic RTL and back to English LTR', () => {
    service.setLocale('ar');
    expect(service.locale()).toBe('ar');
    expect(service.direction()).toBe('rtl');
    expect(documentRef.documentElement.dir).toBe('rtl');
    expect(documentRef.documentElement.lang).toBe('ar');

    service.toggleLocale();
    expect(service.locale()).toBe('en');
    expect(service.direction()).toBe('ltr');
    expect(documentRef.documentElement.dir).toBe('ltr');
  });
});
