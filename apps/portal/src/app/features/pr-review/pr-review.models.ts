export type ReviewRunStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export type ReviewResult =
  | 'APPROVED'
  | 'APPROVED_WITH_SUGGESTIONS'
  | 'REQUEST_CHANGES'
  | 'REJECTED'
  | null;

export type ReviewSeverity = 'INFO' | 'SUGGESTION' | 'WARNING' | 'ERROR' | 'BLOCKER';

export type ReviewCategory =
  | 'Architecture'
  | 'Security'
  | 'Performance'
  | 'CodeQuality'
  | 'Testing'
  | 'Maintainability'
  | 'Documentation'
  | 'Database'
  | 'ApiDesign'
  | 'Frontend'
  | 'Backend'
  | 'Infrastructure';

export interface PrReviewConfigResponse {
  enabled: boolean;
  maxDiffCharacters: number;
  defaultLimit: number;
  maxFindings: number;
  maxRecommendations: number;
  parallelAnalysis: boolean;
  exportEnabled: boolean;
  cacheEnabled: boolean;
  cacheTtlSeconds: number;
}

export interface ReviewRunSummary {
  id: string;
  organizationId: string;
  projectId: string;
  pullRequestOperationId?: string | null;
  pullRequestNumber?: number | null;
  pullRequestTitle?: string | null;
  repositoryRef?: string | null;
  sourceBranch?: string | null;
  targetBranch?: string | null;
  commitSha?: string | null;
  status: ReviewRunStatus;
  result?: ReviewResult;
  overallScore: number;
  riskScore: number;
  architectureScore: number;
  securityScore: number;
  performanceScore: number;
  qualityScore: number;
  testingScore: number;
  documentationScore: number;
  summary?: string | null;
  createdBy: string;
  startedAt?: string | null;
  completedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FindingView {
  id: string;
  reviewRunId: string;
  category: ReviewCategory | string;
  severity: ReviewSeverity | string;
  title: string;
  description: string;
  recommendation: string;
  filePath?: string | null;
  lineHint?: number | null;
  ruleCode?: string | null;
  evidenceExcerpt?: string | null;
  references: string[];
  knowledgeDocumentIds: string[];
  createdAt: string;
}

export interface RecommendationView {
  id: string;
  reviewRunId: string;
  findingId?: string | null;
  priority: string;
  title: string;
  description: string;
  knowledgeDocumentIds: string[];
  createdAt: string;
}

export interface ReviewRunDetail extends ReviewRunSummary {
  changedFiles: string[];
  diffExcerpt?: string | null;
  findings: FindingView[];
  recommendations: RecommendationView[];
}

export interface RiskScoreView {
  reviewRunId: string;
  overallScore: number;
  riskScore: number;
  result?: ReviewResult;
  categoryScores: Record<string, number>;
}

export interface KnowledgeReferenceView {
  findingId: string;
  findingTitle: string;
  category: ReviewCategory | string;
  knowledgeDocumentIds: string[];
}

export interface RunRequest {
  projectId: string;
  pullRequestOperationId?: string | null;
  pullRequestNumber?: number | null;
  pullRequestTitle?: string | null;
  repositoryRef?: string | null;
  sourceBranch?: string | null;
  targetBranch?: string | null;
  commitSha?: string | null;
  changedFiles?: string[] | null;
  diffContent: string;
}
