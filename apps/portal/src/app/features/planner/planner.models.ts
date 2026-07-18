export type PlannerComplexity = 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH';
export type PlannerRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH';
export type PlannerTaskType = 'AGENT_TURN' | 'HUMAN_APPROVAL' | 'TRANSFORM' | 'AGGREGATION';
export type PlannerDependencyType = 'SUCCESS' | 'COMPLETION';
export type PlannerExecutionMode = 'SEQUENTIAL' | 'DEPENDENCY_GRAPH';
export type PlannerFailurePolicy = 'FAIL_FAST' | 'CONTINUE_INDEPENDENT' | 'BEST_EFFORT';

export interface ExecutionTaskDefinition {
  taskKey: string;
  displayName: string;
  description?: string | null;
  taskType: PlannerTaskType;
  agentRole: string;
  classification?: string | null;
  priority?: number | null;
  sequenceOrder?: number | null;
  assignedAgentId?: string | null;
  modelReference?: string | null;
  inputJson?: string | null;
}

export interface ExecutionDependency {
  from: string;
  to: string;
  type: PlannerDependencyType;
}

export interface ExecutionEstimate {
  complexity: PlannerComplexity;
  riskLevel: PlannerRiskLevel;
  estimatedTokens: number;
  estimatedDurationSeconds: number;
  estimatedCostUsd: number;
  notes?: string | null;
}

export interface ExecutionPlan {
  objective: string;
  executionMode: PlannerExecutionMode;
  failurePolicy: PlannerFailurePolicy;
  maxParallelTasks?: number | null;
  maximumDurationMs?: number | null;
  estimatedComplexity?: PlannerComplexity | null;
  estimatedTokens?: number | null;
  estimatedDurationSeconds?: number | null;
  estimatedCostUsd?: number | null;
  riskLevel?: PlannerRiskLevel | null;
  tasks: ExecutionTaskDefinition[];
  dependencies: ExecutionDependency[];
  metadata?: Record<string, unknown> | null;
}

export interface PlannerResponse {
  plan: ExecutionPlan;
  estimate: ExecutionEstimate;
  rawPlannerOutput?: string | null;
  templateId?: string | null;
  validated: boolean;
}

export interface PlanAndCreateResponse {
  planner: PlannerResponse;
  draftRun: {
    id: string;
    status: string;
    name: string;
    projectId: string;
  };
}

export interface PlannerRequest {
  projectId: string;
  objective: string;
  runName?: string | null;
  templateId?: string | null;
  plannerAgentId?: string | null;
  metadataJson?: string | null;
}

export interface PlannerTemplate {
  id: string;
  organizationId: string;
  projectId?: string | null;
  name: string;
  description?: string | null;
  templateType: string;
  enabled: boolean;
}

export const TASK_TYPE_COLORS: Record<PlannerTaskType, string> = {
  AGENT_TURN: '#2563eb',
  HUMAN_APPROVAL: '#b45309',
  TRANSFORM: '#7c3aed',
  AGGREGATION: '#0f766e',
};
