import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { ProjectService } from '../projects/project.service';
import { KnowledgeEnginePage, paginateItems } from './knowledge-engine-page';
import { KnowledgeEngineService } from './knowledge-engine.service';
import { DocumentDetail, DocumentSummary, SearchResult } from './knowledge-engine.models';

describe('KnowledgeComponentTest', () => {
  let fixture: ComponentFixture<KnowledgeEnginePage>;

  const document: DocumentSummary = {
    id: 'doc-1',
    organizationId: 'org-1',
    projectId: 'proj-1',
    title: 'Architecture overview',
    summary: 'System design notes',
    contentFormat: 'MARKDOWN',
    knowledgeType: 'DOCUMENTATION',
    category: 'Architecture',
    status: 'ACTIVE',
    visibility: 'ORGANIZATION',
    authorId: 'user-1',
    version: 1,
    tags: ['architecture'],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  const detail: DocumentDetail = {
    ...document,
    content: '# Overview\n\nInitial **architecture** notes.',
    relations: [
      {
        id: 'rel-1',
        relationType: 'REFERENCES',
        targetDocumentId: 'doc-2',
        targetRefId: null,
        targetRefType: null,
        createdAt: new Date().toISOString(),
      },
    ],
    attachments: [],
  };

  const searchResult: SearchResult = {
    id: 'doc-1',
    title: 'Architecture overview',
    summary: 'System design notes',
    knowledgeType: 'DOCUMENTATION',
    category: 'Architecture',
    visibility: 'ORGANIZATION',
    projectId: 'proj-1',
    authorId: 'user-1',
    tags: ['architecture'],
    matchedSnippet: 'architecture notes',
    updatedAt: new Date().toISOString(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KnowledgeEnginePage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        {
          provide: UserSessionService,
          useValue: {
            user: signal({
              roles: ['ORG_ADMIN'],
              permissions: ['KNOWLEDGE_READ', 'KNOWLEDGE_WRITE', 'KNOWLEDGE_ADMIN'],
            }),
          },
        },
        {
          provide: KnowledgeEngineService,
          useValue: {
            config: () => of({ enabled: true, cacheEnabled: true, cacheTtlSeconds: 60, chunkSize: 500, chunkOverlap: 50 }),
            list: () => of([document]),
            get: () => of(detail),
            search: () => of([searchResult]),
            categories: () => of(['Architecture', 'Backend']),
            tags: () => of(['architecture', 'backend']),
            listByProject: () => of([document]),
            relations: () => of(detail.relations),
          },
        },
        {
          provide: ProjectService,
          useValue: {
            list: () =>
              of({
                content: [
                  {
                    id: 'proj-1',
                    organizationId: 'org-1',
                    name: 'Nova Platform',
                    description: null,
                    status: 'ACTIVE',
                    visibility: 'INTERNAL',
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString(),
                    createdBy: 'user-1',
                    updatedBy: 'user-1',
                  },
                ],
                totalElements: 1,
                totalPages: 1,
                size: 100,
                number: 0,
              }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(KnowledgeEnginePage);
    fixture.detectChanges();
  });

  it('renders knowledge engine heading', () => {
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Knowledge & Memory Engine');
  });

  it('renders selected document data', () => {
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Architecture overview');
    expect(element.textContent).toContain('System design notes');
  });

  it('paginates search results client-side', () => {
    const items = Array.from({ length: 12 }, (_, index) => ({ id: String(index) }));
    expect(paginateItems(items, 0, 5).length).toBe(5);
    expect(paginateItems(items, 1, 5).length).toBe(5);
    expect(paginateItems(items, 2, 5).length).toBe(2);
  });
});
