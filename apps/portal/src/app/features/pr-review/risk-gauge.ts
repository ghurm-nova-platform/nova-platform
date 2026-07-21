import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-risk-gauge',
  templateUrl: './risk-gauge.html',
  styleUrl: './risk-gauge.scss',
})
export class RiskGaugeComponent {
  readonly riskScore = input(0);
  readonly overallScore = input(0);

  readonly riskLevel = computed(() => {
    const score = this.riskScore();
    if (score >= 75) {
      return 'high';
    }
    if (score >= 40) {
      return 'medium';
    }
    return 'low';
  });

  readonly gaugeOffset = computed(() => {
    const clamped = Math.max(0, Math.min(100, this.riskScore()));
    const circumference = 251.2;
    return circumference - (circumference * clamped) / 100;
  });
}
