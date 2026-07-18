import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { By } from '@angular/platform-browser';

import { Shell } from './shell';
import { DirectionService } from '../../core/services/direction.service';
import { ThemeService } from '../../core/services/theme.service';

describe('Shell', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideAnimationsAsync(),
        DirectionService,
        ThemeService,
      ],
    }).compileComponents();
  });

  it('renders navigation items for the administration shell', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Dashboard');
    expect(text).toContain('Organizations');
    expect(text).toContain('Projects');
    expect(text).toContain('Model providers');
    expect(text).toContain('AI Models');
    expect(text).toContain('Provider secrets');
    expect(text).toContain('AI Planner');
    expect(text).toContain('Coding Agent');
    expect(text).toContain('Review Agent');
    expect(text).toContain('Testing Agent');
    expect(text).toContain('AI Orchestration');
    expect(text).toContain('Agents');
    expect(text).toContain('Feedback');
    expect(text).toContain('Settings');
  });

  it('toggles the sidenav open state', () => {
    const fixture = TestBed.createComponent(Shell);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component.navOpen()).toBeTrue();
    component.toggleNav();
    expect(component.navOpen()).toBeFalse();
  });

  it('exposes a user menu trigger', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    const trigger = fixture.debugElement.query(By.css('button[aria-label="User menu"]'));
    expect(trigger).toBeTruthy();
  });
});
