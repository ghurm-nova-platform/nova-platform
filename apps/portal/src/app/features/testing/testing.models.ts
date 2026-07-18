export type TestType =
  | 'UNIT'
  | 'INTEGRATION'
  | 'API'
  | 'UI'
  | 'DATABASE'
  | 'SECURITY'
  | 'PERFORMANCE'
  | 'EDGE_CASE'
  | 'NEGATIVE';

export type TestPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface TestCase {
  id: string;
  generatedTestId: string;
  name: string;
  steps: string | null;
  expectedResult: string | null;
  priority: TestPriority;
}

export interface GeneratedTest {
  id: string;
  type: TestType;
  priority: TestPriority;
  title: string;
  description: string;
  artifactId: string | null;
  artifactPath: string | null;
  cases: TestCase[];
}

export interface TestingResult {
  id: string;
  taskId: string;
  runId: string;
  projectId: string;
  summary: string;
  coverageEstimate: number;
  generatedTests: GeneratedTest[];
  testCases: TestCase[];
  reviewedArtifacts: {
    artifactId: string;
    path: string;
    filename: string;
    language: string;
    sha256: string;
  }[];
  typeCounts: Record<string, number>;
  priorityCounts: Record<string, number>;
  tokensUsed: number | null;
  model: string | null;
  provider: string | null;
  generationTimeMs: number | null;
  createdAt: string;
  validated: boolean;
}

export function coverageTone(score: number): 'green' | 'yellow' | 'red' {
  if (score >= 90) {
    return 'green';
  }
  if (score >= 70) {
    return 'yellow';
  }
  return 'red';
}
