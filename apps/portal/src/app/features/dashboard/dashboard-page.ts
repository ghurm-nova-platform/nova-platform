import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EChartsOption } from 'echarts';
import { NgxEchartsDirective, provideEchartsCore } from 'ngx-echarts';
import { interval, switchMap, startWith } from 'rxjs';
import * as echarts from 'echarts/core';
import { BarChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { DashboardPermissionHelper } from './dashboard-permission.helper';
import { DashboardService } from './dashboard.service';
import { DashboardExportFormat, DashboardSnapshot } from './dashboard.models';

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer]);

@Component({
  selector: 'app-dashboard-page',
  imports: [
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    NgxEchartsDirective,
  ],
  providers: [provideEchartsCore({ echarts })],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage implements OnInit {
  private readonly dashboardApi = inject(DashboardService);
  private readonly runtimeConfig = inject(RuntimeConfigService);
  private readonly destroyRef = inject(DestroyRef);
  readonly permissions = inject(DashboardPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly snapshot = signal<DashboardSnapshot | null>(null);
  readonly refreshRateSeconds = signal(30);

  readonly pipelineChartOptions = computed<EChartsOption>(() => {
    const data = this.snapshot();
    if (!data) {
      return {};
    }
    return {
      tooltip: { trigger: 'axis' },
      legend: { data: ['Current', 'Waiting', 'Failed', 'Success'] },
      xAxis: {
        type: 'category',
        data: data.pipeline.stages.map((stage) => stage.label),
        axisLabel: { rotate: 35, interval: 0 },
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: 'Current',
          type: 'bar',
          stack: 'pipeline',
          data: data.pipeline.stages.map((stage) => stage.current),
        },
        {
          name: 'Waiting',
          type: 'bar',
          stack: 'pipeline',
          data: data.pipeline.stages.map((stage) => stage.waiting),
        },
        {
          name: 'Failed',
          type: 'bar',
          stack: 'pipeline',
          data: data.pipeline.stages.map((stage) => stage.failed),
        },
        {
          name: 'Success',
          type: 'bar',
          stack: 'pipeline',
          data: data.pipeline.stages.map((stage) => stage.success),
        },
      ],
    };
  });

  readonly environmentChartOptions = computed<EChartsOption>(() => {
    const data = this.snapshot();
    if (!data) {
      return {};
    }
    return {
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'bar',
          data: data.environments.buckets.map((bucket) => ({
            name: bucket.bucket,
            value: bucket.environmentCount,
          })),
        },
      ],
      xAxis: {
        type: 'category',
        data: data.environments.buckets.map((bucket) => bucket.bucket),
      },
      yAxis: { type: 'value' },
    };
  });

  ngOnInit(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
      return;
    }
    this.dashboardApi.getConfig().subscribe({
      next: (config) => this.refreshRateSeconds.set(config.refreshRateSeconds || 30),
      error: () => this.refreshRateSeconds.set(30),
    });
    interval(Math.max(this.refreshRateSeconds(), 5) * 1000)
      .pipe(
        startWith(0),
        switchMap(() => {
          this.loading.set(true);
          return this.dashboardApi.getSnapshot();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (snapshot) => {
          this.snapshot.set(snapshot);
          this.loading.set(false);
          this.error.set(null);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err?.error?.message ?? 'Failed to load dashboard');
        },
      });
  }

  refresh(): void {
    if (!this.permissions.canAdmin()) {
      return;
    }
    this.dashboardApi.refresh().subscribe({
      next: () => this.loadSnapshot(),
      error: (err) => this.error.set(err?.error?.message ?? 'Failed to refresh dashboard cache'),
    });
  }

  export(format: DashboardExportFormat): void {
    const base = this.runtimeConfig.platformApiUrl().replace(/\/$/, '');
    const path = this.dashboardApi.exportUrl(format, 'overview');
    window.open(`${base}${path}`, '_blank', 'noopener,noreferrer');
  }

  private loadSnapshot(): void {
    this.loading.set(true);
    this.dashboardApi.getSnapshot().subscribe({
      next: (snapshot) => {
        this.snapshot.set(snapshot);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load dashboard');
      },
    });
  }
}
