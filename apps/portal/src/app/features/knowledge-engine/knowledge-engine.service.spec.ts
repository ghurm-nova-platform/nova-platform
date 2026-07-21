import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { KnowledgeEngineService } from './knowledge-engine.service';

describe('KnowledgeEngineService', () => {
  it('wraps knowledge engine API endpoints', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['get', 'post', 'put', 'delete']);
    api.get.and.returnValue(of([]));
    api.post.and.returnValue(of({ id: 'doc-1' }));
    api.put.and.returnValue(of({ id: 'doc-1' }));
    api.delete.and.returnValue(of(undefined));

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        RuntimeConfigService,
        KnowledgeEngineService,
        { provide: ApiClient, useValue: api },
      ],
    });
    const service = TestBed.inject(KnowledgeEngineService);

    service.config().subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/knowledge/config');

    service.list().subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/knowledge');

    service.list('proj-1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/knowledge?projectId=proj-1');

    service.search({ q: 'architecture', category: 'Architecture' }).subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/knowledge/search', {
      q: 'architecture',
      category: 'Architecture',
    });

    service.memory({ projectId: 'proj-1', limit: 10, types: ['DOCUMENTATION'] }).subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/knowledge/memory', {
      projectId: 'proj-1',
      limit: '10',
      types: 'DOCUMENTATION',
    });

    service.categories().subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/knowledge/categories');

    service.tags().subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/knowledge/tags');

    service.listByProject('proj-1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/knowledge/project/proj-1');

    service.get('doc-1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/knowledge/doc-1');

    service.relations('doc-1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/knowledge/doc-1/relations');

    service
      .create({
        title: 'New doc',
        content: 'Body',
        contentFormat: 'MARKDOWN',
        knowledgeType: 'DOCUMENTATION',
        category: 'General',
      })
      .subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/knowledge', {
      title: 'New doc',
      content: 'Body',
      contentFormat: 'MARKDOWN',
      knowledgeType: 'DOCUMENTATION',
      category: 'General',
    });

    service
      .importDocument({
        title: 'Imported',
        content: '# Hello',
        importFormat: 'markdown',
      })
      .subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/knowledge/import', {
      title: 'Imported',
      content: '# Hello',
      importFormat: 'markdown',
    });

    service.archive('doc-1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/knowledge/doc-1/archive', {});

    service.restore('doc-1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/knowledge/doc-1/restore', {});

    service
      .relate('doc-1', { relationType: 'REFERENCES', targetDocumentId: 'doc-2' })
      .subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/knowledge/doc-1/relate', {
      relationType: 'REFERENCES',
      targetDocumentId: 'doc-2',
    });

    service.update('doc-1', { title: 'Updated' }).subscribe();
    expect(api.put).toHaveBeenCalledWith('/api/knowledge/doc-1', { title: 'Updated' });

    service.delete('doc-1').subscribe();
    expect(api.delete).toHaveBeenCalledWith('/api/knowledge/doc-1');
  });
});
