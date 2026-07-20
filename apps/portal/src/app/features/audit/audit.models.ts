export type AuditEntityType =
  | 'RELEASE'
  | 'DEPLOYMENT'
  | 'ROLLBACK'
  | 'ENVIRONMENT'
  | 'POLICY'
  | 'MERGE'
  | 'APPROVAL'
  | 'TASK'
  | 'REPOSITORY'
  | 'USER'
  | 'CONFIGURATION';

export type AuditAction =
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'ENABLE'
  | 'DISABLE'
  | 'ARCHIVE'
  | 'APPROVE'
  | 'REJECT'
  | 'MERGE'
  | 'VALIDATE'
  | 'OBSERVE'
  | 'LOGIN'
  | 'LOGOUT'
  | 'ACCESS';

export type AuditResult = 'SUCCESS' | 'FAILURE' | 'WARNING' | 'DENIED';
export type AuditSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type AuditSource =
  | 'PORTAL'
  | 'REST_API'
  | 'SYSTEM'
  | 'SCHEDULER'
  | 'MERGE_AGENT'
  | 'RELEASE_MANAGER'
  | 'DEPLOYMENT_OBSERVATION'
  | 'ROLLBACK_MANAGER'
  | 'RELEASE_POLICIES'
  | 'ENVIRONMENT_MANAGEMENT';

export interface AuditEvent {
  id: string;
  organizationId: string;
  projectId?: string;
  userId?: string;
  username?: string;
  sessionId?: string;
  entityType: AuditEntityType;
  entityId?: string;
  entityLabel?: string;
  action: AuditAction;
  result: AuditResult;
  severity: AuditSeverity;
  source: AuditSource;
  correlationId?: string;
  requestId?: string;
  ipAddress?: string;
  userAgent?: string;
  details?: Record<string, unknown>;
  createdAt: string;
}

export interface AuditSearchResponse {
  events: AuditEvent[];
  total: number;
  page: number;
  size: number;
}

export interface AuditHistoryResponse {
  entityType: AuditEntityType;
  entityId: string;
  entityLabel?: string;
  events: AuditEvent[];
}

export interface AuditSearchParams {
  from?: string;
  to?: string;
  projectId?: string;
  userId?: string;
  entityType?: AuditEntityType;
  entityId?: string;
  action?: AuditAction;
  severity?: AuditSeverity;
  result?: AuditResult;
  correlationId?: string;
  requestId?: string;
  page?: number;
  size?: number;
}
