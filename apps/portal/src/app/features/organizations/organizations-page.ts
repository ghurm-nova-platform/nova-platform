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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Organization } from '../../core/models/catalog';
import { OrganizationDialog } from './organization-dialog';
import { OrganizationService } from './organization.service';

@Component({
  selector: 'app-organizations-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
  ],
  templateUrl: './organizations-page.html',
  styleUrl: './organizations-page.scss',
})
export class OrganizationsPage implements OnInit {
  private readonly organizationsApi = inject(OrganizationService);
  private readonly dialog = inject(MatDialog);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly displayedColumns = ['name', 'slug', 'updatedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<Organization[]>([]);
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
    this.organizationsApi
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
          this.error.set('Unable to load organizations.');
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
      .open(OrganizationDialog, { data: { mode: 'create' }, width: '480px' })
      .afterClosed()
      .subscribe((result) => {
        if (!result) {
          return;
        }
        this.organizationsApi.create(result).subscribe({
          next: () => this.load(),
          error: () => this.error.set('Unable to create organization.'),
        });
      });
  }

  edit(organization: Organization): void {
    this.dialog
      .open(OrganizationDialog, {
        data: { mode: 'edit', organization },
        width: '480px',
      })
      .afterClosed()
      .subscribe((result) => {
        if (!result) {
          return;
        }
        this.organizationsApi.update(organization.id, result).subscribe({
          next: () => this.load(),
          error: () => this.error.set('Unable to update organization.'),
        });
      });
  }

  remove(organization: Organization): void {
    const confirmed = window.confirm(`Delete organization "${organization.name}"?`);
    if (!confirmed) {
      return;
    }
    this.organizationsApi.remove(organization.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to delete organization.'),
    });
  }
}
