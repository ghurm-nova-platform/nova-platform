import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { EnvironmentPage } from './environment-page';
import { EnvironmentPermissionHelper } from './environment-permission.helper';
import { EnvironmentService } from './environment.service';
import { ManagedEnvironment } from './environment.models';

describe('EnvironmentPage', () => {
  const environment: ManagedEnvironment = {
    id: 'e1',
    organizationId: '11111111-1111-1111-1111-111111111111',
    projectId: '55555555-5555-5555-5555-555555555501',
    code: 'STAGING_55555555',
    name: 'Staging',
    description: 'test',
    environmentType: 'STAGING',
    status: 'ACTIVE',
    active: true,
    region: 'us-east-1',
    provider: 'kubernetes',
    clusterName: 'cluster-a',
    namespaceName: 'app',
    cloudProvider: null,
    platform: null,
    ownerName: 'team',
    businessUnit: null,
    costCenter: null,
    tags: {},
    createdBy: null,
    createdAt: '2026-07-20T00:00:00Z',
    updatedAt: '2026-07-20T00:01:00Z',
    labels: [{ key: 'team', value: 'platform', createdAt: '2026-07-20T00:00:00Z' }],
    variables: [],
    timeline: [{ eventType: 'CREATED', at: '2026-07-20T00:00:00Z', detail: 'created' }],
    history: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvironmentPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['ENVIRONMENT_RUN', 'ENVIRONMENT_READ'],
            }),
          },
        },
        {
          provide: EnvironmentService,
          useValue: {
            list: () => of([environment]),
            history: () => of(environment),
            create: () => of(environment),
          },
        },
        EnvironmentPermissionHelper,
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(EnvironmentPage);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('loads list when permitted', () => {
    const fixture = TestBed.createComponent(EnvironmentPage);
    const component = fixture.componentInstance;
    component.form.controls.projectId.setValue(environment.projectId);
    component.loadList();
    expect(component.environments().length).toBe(1);
  });
});
