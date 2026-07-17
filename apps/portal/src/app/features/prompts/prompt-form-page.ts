import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { of, switchMap } from 'rxjs';

import {
  Prompt,
  PromptType,
  PromptValidateResponse,
  PromptVariableDataType,
  PromptVariableRequest,
  PromptVariableResponse,
  PROMPT_TYPES,
  PROMPT_VARIABLE_DATA_TYPES,
  detectPromptVariables,
  formatTagsInput,
  parseTagsInput,
} from './prompt.models';
import { PromptFormCanDeactivate } from './prompt-unsaved.guard';
import { PromptPermissionHelper } from './prompt-permission.helper';
import { PromptPreviewDialog } from './prompt-preview-dialog';
import { PromptService } from './prompt.service';

interface VariableFormValue {
  name: string;
  description: string;
  dataType: PromptVariableDataType;
  required: boolean;
  defaultValue: string;
  sampleValue: string;
}

@Component({
  selector: 'app-prompt-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatDialogModule,
  ],
  templateUrl: './prompt-form-page.html',
  styleUrl: './prompt-form-page.scss',
})
export class PromptFormPage implements OnInit, PromptFormCanDeactivate {
  @ViewChild('contentArea') contentArea?: ElementRef<HTMLTextAreaElement>;

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly promptsApi = inject(PromptService);
  private readonly dialog = inject(MatDialog);
  readonly permissions = inject(PromptPermissionHelper);

  readonly projectId = signal('');
  readonly promptId = signal<string | null>(null);
  readonly editMode = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly publishing = signal(false);
  readonly creatingDraft = signal(false);
  readonly validating = signal(false);
  readonly error = signal<string | null>(null);
  readonly conflict = signal(false);
  readonly needsDraft = signal(false);
  readonly draftVersionId = signal<string | null>(null);
  readonly detectedVariables = signal<string[]>([]);
  readonly validationResult = signal<PromptValidateResponse | null>(null);
  readonly types = PROMPT_TYPES;
  readonly dataTypes = PROMPT_VARIABLE_DATA_TYPES;
  private promptVersion = 0;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    promptType: this.fb.nonNullable.control<PromptType>('CHAT', Validators.required),
    tagsInput: [''],
    changeSummary: [''],
    content: ['', [Validators.required]],
    variables: this.fb.array<FormGroup>([]),
  });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    const promptId = this.route.snapshot.paramMap.get('promptId');
    this.promptId.set(promptId);
    this.editMode.set(!!promptId);

    this.form.controls.content.valueChanges.subscribe((content) => {
      this.detectedVariables.set(detectPromptVariables(content));
    });

    if (promptId) {
      if (!this.permissions.canUpdate()) {
        this.error.set('You are not allowed to edit prompts.');
        return;
      }
      this.loadPrompt(promptId);
    } else if (!this.permissions.canCreate()) {
      this.error.set('You are not allowed to create prompts.');
    }
  }

  get variablesArray(): FormArray<FormGroup> {
    return this.form.controls.variables;
  }

  contentLength(): number {
    return this.form.controls.content.value.length;
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  addVariable(seed?: PromptVariableRequest): void {
    this.variablesArray.push(this.createVariableGroup(seed));
    this.form.markAsDirty();
  }

  removeVariable(index: number): void {
    this.variablesArray.removeAt(index);
    this.form.markAsDirty();
  }

  insertVariable(name: string): void {
    const token = `{{${name}}}`;
    const contentControl = this.form.controls.content;
    const textarea = this.contentArea?.nativeElement;
    if (textarea) {
      const start = textarea.selectionStart ?? contentControl.value.length;
      const end = textarea.selectionEnd ?? start;
      const current = contentControl.value;
      contentControl.setValue(current.slice(0, start) + token + current.slice(end));
    } else {
      contentControl.setValue(contentControl.value + token);
    }
    contentControl.markAsDirty();
    this.detectedVariables.set(detectPromptVariables(contentControl.value));
  }

  validateContent(): void {
    if (!this.permissions.canPreview()) {
      this.error.set('You are not allowed to validate prompts.');
      return;
    }
    this.validating.set(true);
    this.validationResult.set(null);
    this.promptsApi
      .validate(this.projectId(), {
        content: this.form.controls.content.value.trim(),
        variables: this.collectVariables(),
      })
      .subscribe({
        next: (result) => {
          this.validationResult.set(result);
          this.validating.set(false);
        },
        error: () => {
          this.error.set('Unable to validate prompt content.');
          this.validating.set(false);
        },
      });
  }

  openPreview(): void {
    if (!this.permissions.canPreview()) {
      this.error.set('You are not allowed to preview prompts.');
      return;
    }
    this.dialog.open(PromptPreviewDialog, {
      width: '640px',
      data: {
        projectId: this.projectId(),
        content: this.form.controls.content.value.trim(),
        variables: this.collectVariables(),
      },
    });
  }

  createDraft(): void {
    const promptId = this.promptId();
    if (!promptId || this.creatingDraft()) {
      return;
    }
    this.creatingDraft.set(true);
    this.error.set(null);
    this.promptsApi.createVersion(this.projectId(), promptId, {}).subscribe({
      next: (version) => {
        this.draftVersionId.set(version.id);
        this.needsDraft.set(false);
        this.patchVersion(version.content, version.variables);
        this.creatingDraft.set(false);
        this.form.markAsPristine();
      },
      error: () => {
        this.error.set('Unable to create draft version.');
        this.creatingDraft.set(false);
      },
    });
  }

  publishDraft(): void {
    const promptId = this.promptId();
    const versionId = this.draftVersionId();
    if (!promptId || !versionId || !this.permissions.canPublish()) {
      return;
    }
    if (!window.confirm('Publish the current draft version?')) {
      return;
    }
    this.publishing.set(true);
    this.error.set(null);
    this.promptsApi.publishVersion(this.projectId(), promptId, versionId, {}).subscribe({
      next: () => {
        this.publishing.set(false);
        void this.router.navigate(['/projects', this.projectId(), 'prompts', promptId]);
      },
      error: (err: { error?: { message?: string } }) => {
        this.publishing.set(false);
        this.error.set(err.error?.message ?? 'Unable to publish draft.');
      },
    });
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.editMode()) {
      this.saveEdit();
      return;
    }
    this.saveCreate();
  }

  private saveCreate(): void {
    this.saving.set(true);
    this.error.set(null);
    const raw = this.form.getRawValue();
    this.promptsApi
      .create(this.projectId(), {
        name: raw.name.trim(),
        description: raw.description.trim() || null,
        promptType: raw.promptType,
        content: raw.content.trim(),
        changeSummary: raw.changeSummary.trim() || null,
        tags: parseTagsInput(raw.tagsInput),
        variables: this.collectVariables(),
      })
      .subscribe({
        next: (prompt) => {
          this.saving.set(false);
          this.form.markAsPristine();
          void this.router.navigate(['/projects', this.projectId(), 'prompts', prompt.id]);
        },
        error: (err: { status?: number; error?: { code?: string; message?: string } }) => {
          this.saving.set(false);
          this.handleSaveError(err);
        },
      });
  }

  private saveEdit(): void {
    const promptId = this.promptId();
    if (!promptId) {
      return;
    }
    if (this.needsDraft()) {
      this.error.set('Create a draft before editing prompt content.');
      return;
    }
    const versionId = this.draftVersionId();
    if (!versionId) {
      this.error.set('No draft version is available for content updates.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.conflict.set(false);
    const raw = this.form.getRawValue();
    const metadata$ = this.promptsApi.update(this.projectId(), promptId, {
      name: raw.name.trim(),
      description: raw.description.trim() || null,
      promptType: raw.promptType,
      tags: parseTagsInput(raw.tagsInput),
      version: this.promptVersion,
    });
    const content$ = this.promptsApi.updateVersion(this.projectId(), promptId, versionId, {
      content: raw.content.trim(),
      changeSummary: raw.changeSummary.trim() || null,
      variables: this.collectVariables(),
    });

    metadata$
      .pipe(switchMap((prompt) => content$.pipe(switchMap(() => of(prompt)))))
      .subscribe({
        next: (prompt) => {
          this.promptVersion = prompt.version;
          this.saving.set(false);
          this.form.markAsPristine();
          void this.router.navigate(['/projects', this.projectId(), 'prompts', prompt.id]);
        },
        error: (err: { status?: number; error?: { code?: string; message?: string } }) => {
          this.saving.set(false);
          this.handleSaveError(err);
        },
      });
  }

  private loadPrompt(promptId: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.promptsApi.get(this.projectId(), promptId).subscribe({
      next: (prompt) => {
        this.patchPrompt(prompt);
        const draftId = prompt.currentDraftVersionId;
        if (draftId) {
          this.draftVersionId.set(draftId);
          this.needsDraft.set(false);
          this.loadVersion(promptId, draftId);
          return;
        }
        if (prompt.publishedVersionId) {
          this.needsDraft.set(true);
          this.draftVersionId.set(null);
          this.loadVersion(promptId, prompt.publishedVersionId, true);
          return;
        }
        this.loading.set(false);
        this.error.set('Prompt has no editable version.');
      },
      error: () => {
        this.error.set('Unable to load prompt.');
        this.loading.set(false);
      },
    });
  }

  private loadVersion(promptId: string, versionId: string, readOnlyPublished = false): void {
    this.promptsApi.getVersion(this.projectId(), promptId, versionId).subscribe({
      next: (version) => {
        this.patchVersion(version.content, version.variables);
        if (readOnlyPublished) {
          this.form.controls.content.disable();
          this.form.controls.changeSummary.disable();
          this.variablesArray.controls.forEach((group) => group.disable());
        }
        this.loading.set(false);
        this.form.markAsPristine();
      },
      error: () => {
        this.error.set('Unable to load prompt version.');
        this.loading.set(false);
      },
    });
  }

  private patchPrompt(prompt: Prompt): void {
    this.promptVersion = prompt.version;
    this.form.patchValue({
      name: prompt.name,
      description: prompt.description ?? '',
      promptType: prompt.promptType,
      tagsInput: formatTagsInput(prompt.tags),
    });
  }

  private patchVersion(content: string, variables: PromptVariableResponse[]): void {
    this.variablesArray.clear();
    for (const variable of variables) {
      this.variablesArray.push(
        this.createVariableGroup({
          name: variable.name,
          description: variable.description,
          dataType: variable.dataType,
          required: variable.required,
          defaultValue: variable.defaultValue,
          sampleValue: variable.sampleValue,
        }),
      );
    }
    this.form.patchValue({ content });
    this.detectedVariables.set(detectPromptVariables(content));
  }

  private createVariableGroup(seed?: PromptVariableRequest): FormGroup {
    return this.fb.nonNullable.group({
      name: [seed?.name ?? '', [Validators.required, Validators.maxLength(100)]],
      description: [seed?.description ?? ''],
      dataType: this.fb.nonNullable.control<PromptVariableDataType>(seed?.dataType ?? 'STRING'),
      required: [seed?.required ?? false],
      defaultValue: [seed?.defaultValue ?? ''],
      sampleValue: [seed?.sampleValue ?? ''],
    });
  }

  private collectVariables(): PromptVariableRequest[] {
    const rows = this.variablesArray.getRawValue() as VariableFormValue[];
    return rows.map((variable) => ({
      name: variable.name.trim(),
      description: variable.description.trim() || null,
      dataType: variable.dataType,
      required: !!variable.required,
      defaultValue: variable.defaultValue.trim() || null,
      sampleValue: variable.sampleValue.trim() || null,
    }));
  }

  private handleSaveError(err: { status?: number; error?: { code?: string; message?: string } }): void {
    if (err.status === 409 && err.error?.code === 'OPTIMISTIC_LOCK_CONFLICT') {
      this.conflict.set(true);
      this.error.set('This prompt was modified elsewhere. Reload and try again.');
      return;
    }
    this.error.set(err.error?.message ?? 'Unable to save prompt.');
  }
}
