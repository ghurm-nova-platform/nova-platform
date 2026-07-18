export type ReviewSeverity = 'INFO' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type ReviewCategory =
  | 'CORRECTNESS'
  | 'ARCHITECTURE'
  | 'MAINTAINABILITY'
  | 'READABILITY'
  | 'SECURITY'
  | 'PERFORMANCE'
  | 'CONCURRENCY'
  | 'VALIDATION'
  | 'ERROR_HANDLING'
  | 'DOCUMENTATION'
  | 'NAMING'
  | 'TESTING'
  | 'BEST_PRACTICES';

export interface ReviewFinding {
  id: string;
  severity: ReviewSeverity;
  category: ReviewCategory;
  title: string;
  description: string;
  recommendation: string;
  artifactId: string | null;
  artifactPath: string | null;
}

export interface ArtifactReview {
  artifactId: string;
  path: string;
  filename: string;
  language: string;
  sha256: string;
}

export interface ReviewResult {
  id: string;
  taskId: string;
  runId: string;
  projectId: string;
  summary: string;
  score: number;
  approved: boolean;
  findings: ReviewFinding[];
  reviewedArtifacts: ArtifactReview[];
  severityCounts: Record<string, number>;
  tokensUsed: number | null;
  model: string | null;
  provider: string | null;
  reviewTimeMs: number | null;
  createdAt: string;
  validated: boolean;
}

export function scoreTone(score: number): 'green' | 'yellow' | 'red' {
  if (score >= 90) {
    return 'green';
  }
  if (score >= 70) {
    return 'yellow';
  }
  return 'red';
}
