export type CollaborationSessionStatus =
  | 'CREATED'
  | 'STARTING'
  | 'ACTIVE'
  | 'WAITING'
  | 'BLOCKED'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export type CollaborationParticipantRole =
  | 'PLANNER'
  | 'CODING'
  | 'REVIEW'
  | 'TESTING'
  | 'REPAIR'
  | 'CI'
  | 'MERGE'
  | 'RELEASE'
  | 'DEPLOYMENT'
  | 'ROLLBACK'
  | 'HUMAN_REVIEWER';

export type CollaborationParticipantStatus =
  | 'IDLE'
  | 'ACTIVE'
  | 'WAITING'
  | 'BLOCKED'
  | 'COMPLETED'
  | 'FAILED';

export type CollaborationTaskStatus =
  | 'PENDING'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'BLOCKED'
  | 'COMPLETED'
  | 'REJECTED'
  | 'CANCELLED';

export type CollaborationMessageType =
  | 'TASK'
  | 'QUESTION'
  | 'ANSWER'
  | 'WARNING'
  | 'ERROR'
  | 'INFO'
  | 'APPROVAL_REQUEST'
  | 'DECISION';

export type CollaborationDecisionType =
  | 'APPROVE'
  | 'REJECT'
  | 'RESOLVE_CONFLICT'
  | 'PAUSE'
  | 'RESUME'
  | 'CANCEL'
  | 'REQUEST_REVIEW'
  | 'REQUEST_APPROVAL'
  | 'REQUEST_CLARIFICATION';

export type CollaborationTimelineEventType =
  | 'CREATED'
  | 'STARTED'
  | 'TASK_ASSIGNED'
  | 'TASK_STARTED'
  | 'TASK_COMPLETED'
  | 'TASK_BLOCKED'
  | 'TASK_RESUMED'
  | 'MESSAGE_SENT'
  | 'DECISION'
  | 'APPROVAL'
  | 'CONFLICT'
  | 'PAUSED'
  | 'RESUMED'
  | 'CANCELLED'
  | 'COMPLETED'
  | 'FAILED';

export type TaskAction =
  | 'ASSIGN'
  | 'COMPLETE'
  | 'REJECT'
  | 'REASSIGN'
  | 'BLOCK'
  | 'RESUME'
  | 'CANCEL';

export interface SharedContext {
  projectId: string | null;
  repositoryId: string | null;
  branch: string | null;
  releaseId: string | null;
  environmentId: string | null;
  executionId: string | null;
  auditEventIds: string[] | null;
}

export interface SessionSummary {
  id: string;
  organizationId: string;
  projectId: string;
  orchestrationRunId: string | null;
  name: string;
  status: CollaborationSessionStatus;
  conflictDetected: boolean;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ParticipantView {
  id: string;
  participantRole: CollaborationParticipantRole;
  status: CollaborationParticipantStatus;
  currentTaskId: string | null;
  progressPercent: number;
  parallelGroup: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface TaskView {
  id: string;
  taskKey: string;
  title: string;
  status: CollaborationTaskStatus;
  participantId: string | null;
  dependsOnTaskId: string | null;
  blockedByTaskId: string | null;
  completedByParticipantId: string | null;
  artifactRef: string | null;
  parallelGroup: string | null;
  assignedAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface MessageView {
  id: string;
  senderRole: CollaborationParticipantRole;
  messageType: CollaborationMessageType;
  content: string;
  taskId: string | null;
  createdAt: string;
}

export interface DecisionView {
  id: string;
  decisionType: CollaborationDecisionType;
  summary: string;
  details: Record<string, unknown> | null;
  decidedBy: string | null;
  taskId: string | null;
  createdAt: string;
}

export interface TimelineEventView {
  id: string;
  eventType: CollaborationTimelineEventType;
  summary: string;
  actorRole: CollaborationParticipantRole | null;
  taskId: string | null;
  messageId: string | null;
  decisionId: string | null;
  details: Record<string, unknown> | null;
  createdAt: string;
}

export interface SessionDetail {
  id: string;
  organizationId: string;
  projectId: string;
  orchestrationRunId: string | null;
  name: string;
  status: CollaborationSessionStatus;
  sharedContext: SharedContext | null;
  parallelGroup: string | null;
  conflictDetected: boolean;
  conflictDetails: Record<string, unknown> | null;
  createdBy: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  participants: ParticipantView[];
  tasks: TaskView[];
  messages: MessageView[];
  decisions: DecisionView[];
  timeline: TimelineEventView[];
}

export interface CollaborationConfigResponse {
  enabled: boolean;
  pollingSeconds: number;
  maxMessages: number;
}

export interface InitialTaskRequest {
  taskKey: string;
  title: string;
  dependsOnTaskId?: string | null;
  parallelGroup?: string | null;
  artifactRef?: string | null;
}

export interface CreateSessionRequest {
  projectId: string;
  orchestrationRunId?: string | null;
  name: string;
  sharedContext?: SharedContext | null;
  participantRoles?: CollaborationParticipantRole[];
  initialTasks?: InitialTaskRequest[];
}

export interface AssignTaskRequest {
  taskId: string;
  action?: TaskAction;
  participantId?: string | null;
  blockedByTaskId?: string | null;
  reassignToParticipantId?: string | null;
  artifactRef?: string | null;
  parallelGroup?: string | null;
  reason?: string | null;
}

export interface SendMessageRequest {
  senderRole: CollaborationParticipantRole;
  messageType: CollaborationMessageType;
  content: string;
  taskId?: string | null;
}

export interface RecordDecisionRequest {
  decisionType: CollaborationDecisionType;
  summary: string;
  details?: Record<string, unknown> | null;
  taskId?: string | null;
}
