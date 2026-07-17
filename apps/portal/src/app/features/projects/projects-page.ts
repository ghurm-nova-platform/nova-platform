import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Project, ProjectStatus } from '../../core/models/catalog';
import { ProjectDialog } from './project-dialog';
import { ProjectService } from './project.service';

@Component({
  selector: 'app-projects-page',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDialogModule,
  ],
  templateUrl: './projects-page.html',
  styleUrl: './projects-page.scss',
})
export class ProjectsPage implements OnInit {
  private readonly projectsApi = inject(ProjectService);
  private readonly dialog = inject(MatDialog);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly displayedColumns = ['name', 'status', 'visibility', 'updatedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<Project[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('name,asc');

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(debounceTime(250), distinctUntilChanged()).subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.projectsApi
      .list({
        search: this.searchControl.value.trim() || undefined,
        page: this.pageIndex(),
        size: this.pageSize(),
        sort: this.sort(),
      })
      .subscribe({
        next: (page) => {
          this.rows.set(page.content);
          this.total.set(page.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Unable to load projects.');
          this.loading.set(false);
        },
      });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  onSort(sort: Sort): void {
    if (!sort.active || !sort.direction) {
      this.sort.set('name,asc');
    } else {
      this.sort.set(`${sort.active},${sort.direction}`);
    }
    this.load();
  }

  create(): void {
    this.dialog
      .open(ProjectDialog, { data: { mode: 'create' }, width: '520px' })
      .afterClosed()
      .subscribe((result) => {
        if (!result) {
          return;
        }
        this.projectsApi.create(result).subscribe({
          next: () => this.load(),
          error: () => this.error.set('Unable to create project.'),
        });
      });
  }

  edit(project: Project): void {
    this.dialog
      .open(ProjectDialog, { data: { mode: 'edit', project }, width: '520px' })
      .afterClosed()
      .subscribe((result) => {
        if (!result) {
          return;
        }
        this.projectsApi.update(project.id, result).subscribe({
          next: () => this.load(),
          error: () => this.error.set('Unable to update project.'),
        });
      });
  }

  archive(project: Project): void {
    const confirmed = window.confirm(`Archive project "${project.name}"?`);
    if (!confirmed) {
      return;
    }
    this.projectsApi.remove(project.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to archive project.'),
    });
  }

  statusClass(status: ProjectStatus): string {
    return `status status--${status.toLowerCase()}`;
  }
}
