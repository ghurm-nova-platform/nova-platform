import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { RiskGaugeComponent } from './risk-gauge';

describe('RiskGaugeTest', () => {
  let fixture: ComponentFixture<RiskGaugeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RiskGaugeComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(RiskGaugeComponent);
    fixture.componentRef.setInput('riskScore', 72);
    fixture.componentRef.setInput('overallScore', 84);
    fixture.detectChanges();
  });

  it('renders risk and overall scores', () => {
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('72');
    expect(element.textContent).toContain('84');
  });

  it('classifies high risk levels', () => {
    fixture.componentRef.setInput('riskScore', 80);
    fixture.detectChanges();
    expect(fixture.componentInstance.riskLevel()).toBe('high');
  });

  it('computes gauge stroke offset from risk score', () => {
    fixture.componentRef.setInput('riskScore', 50);
    fixture.detectChanges();
    expect(fixture.componentInstance.gaugeOffset()).toBeCloseTo(125.6, 1);
  });
});
