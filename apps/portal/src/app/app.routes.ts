import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './auth/guards/auth.guard';
import { promptUnsavedGuard } from './features/prompts/prompt-unsaved.guard';
import { Shell } from './layout/shell/shell';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/login/login-page').then((m) => m.LoginPage),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
      },
      {
        path: 'organizations',
        loadComponent: () =>
          import('./features/organizations/organizations-page').then((m) => m.OrganizationsPage),
      },
      {
        path: 'model-providers',
        loadComponent: () =>
          import('./features/model-gateway/model-providers-list-page').then((m) => m.ModelProvidersListPage),
      },
      {
        path: 'ai-models',
        loadComponent: () =>
          import('./features/model-gateway/catalog-models-list-page').then((m) => m.CatalogModelsListPage),
      },
      {
        path: 'ai-models/new',
        loadComponent: () =>
          import('./features/model-gateway/catalog-model-form-page').then((m) => m.CatalogModelFormPage),
      },
      {
        path: 'ai-models/:modelId/edit',
        loadComponent: () =>
          import('./features/model-gateway/catalog-model-form-page').then((m) => m.CatalogModelFormPage),
      },
      {
        path: 'ai-models/:modelId',
        loadComponent: () =>
          import('./features/model-gateway/catalog-model-detail-page').then((m) => m.CatalogModelDetailPage),
      },
      {
        path: 'provider-secrets',
        loadComponent: () =>
          import('./features/model-gateway/provider-secrets-list-page').then((m) => m.ProviderSecretsListPage),
      },
      {
        path: 'provider-secrets/new',
        loadComponent: () =>
          import('./features/model-gateway/provider-secret-form-page').then((m) => m.ProviderSecretFormPage),
      },
      {
        path: 'provider-secrets/:secretId',
        loadComponent: () =>
          import('./features/model-gateway/provider-secret-detail-page').then((m) => m.ProviderSecretDetailPage),
      },
      {
        path: 'planner',
        loadComponent: () =>
          import('./features/planner/planner-page').then((m) => m.PlannerPage),
      },
      {
        path: 'coding',
        loadComponent: () =>
          import('./features/coding/coding-page').then((m) => m.CodingPage),
      },
      {
        path: 'review',
        loadComponent: () =>
          import('./features/review/review-page').then((m) => m.ReviewPage),
      },
      {
        path: 'testing',
        loadComponent: () =>
          import('./features/testing/testing-page').then((m) => m.TestingPage),
      },
      {
        path: 'orchestration-runs',
        loadComponent: () =>
          import('./features/orchestration/orchestration-runs-list-page').then(
            (m) => m.OrchestrationRunsListPage,
          ),
      },
      {
        path: 'orchestration-runs/new',
        loadComponent: () =>
          import('./features/orchestration/orchestration-run-form-page').then(
            (m) => m.OrchestrationRunFormPage,
          ),
      },
      {
        path: 'orchestration-runs/:runId/edit',
        loadComponent: () =>
          import('./features/orchestration/orchestration-run-form-page').then(
            (m) => m.OrchestrationRunFormPage,
          ),
      },
      {
        path: 'orchestration-runs/:runId/graph',
        loadComponent: () =>
          import('./features/orchestration/orchestration-graph-builder-page').then(
            (m) => m.OrchestrationGraphBuilderPage,
          ),
      },
      {
        path: 'orchestration-runs/:runId',
        loadComponent: () =>
          import('./features/orchestration/orchestration-run-detail-page').then(
            (m) => m.OrchestrationRunDetailPage,
          ),
      },
      {
        path: 'model-providers/new',
        loadComponent: () =>
          import('./features/model-gateway/model-provider-form-page').then((m) => m.ModelProviderFormPage),
      },
      {
        path: 'model-providers/:providerId/models/new',
        loadComponent: () =>
          import('./features/model-gateway/provider-model-form-page').then((m) => m.ProviderModelFormPage),
      },
      {
        path: 'model-providers/:providerId/models/:modelId',
        loadComponent: () =>
          import('./features/model-gateway/provider-model-form-page').then((m) => m.ProviderModelFormPage),
      },
      {
        path: 'model-providers/:providerId/models',
        loadComponent: () =>
          import('./features/model-gateway/provider-models-list-page').then((m) => m.ProviderModelsListPage),
      },
      {
        path: 'model-providers/:providerId/edit',
        loadComponent: () =>
          import('./features/model-gateway/model-provider-form-page').then((m) => m.ModelProviderFormPage),
      },
      {
        path: 'model-providers/:providerId',
        loadComponent: () =>
          import('./features/model-gateway/model-provider-detail-page').then((m) => m.ModelProviderDetailPage),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./features/projects/projects-page').then((m) => m.ProjectsPage),
      },
      {
        path: 'projects/:projectId/agents',
        loadComponent: () =>
          import('./features/agents/agents-list-page').then((m) => m.AgentsListPage),
      },
      {
        path: 'projects/:projectId/agents/new',
        loadComponent: () =>
          import('./features/agents/agent-form-page').then((m) => m.AgentFormPage),
      },
      {
        path: 'projects/:projectId/agents/:agentId',
        loadComponent: () =>
          import('./features/agents/agent-detail-page').then((m) => m.AgentDetailPage),
      },
      {
        path: 'projects/:projectId/agents/:agentId/edit',
        loadComponent: () =>
          import('./features/agents/agent-form-page').then((m) => m.AgentFormPage),
      },
      {
        path: 'projects/:projectId/agents/:agentId/playground',
        loadComponent: () =>
          import('./features/execution/agent-playground-page').then((m) => m.AgentPlaygroundPage),
      },
      {
        path: 'projects/:projectId/knowledge-bases',
        loadComponent: () =>
          import('./features/knowledge/knowledge-bases-list-page').then((m) => m.KnowledgeBasesListPage),
      },
      {
        path: 'projects/:projectId/knowledge-bases/new',
        loadComponent: () =>
          import('./features/knowledge/knowledge-base-form-page').then((m) => m.KnowledgeBaseFormPage),
      },
      {
        path: 'projects/:projectId/knowledge-bases/:knowledgeBaseId/documents/:documentId',
        loadComponent: () =>
          import('./features/knowledge/knowledge-document-detail-page').then(
            (m) => m.KnowledgeDocumentDetailPage,
          ),
      },
      {
        path: 'projects/:projectId/knowledge-bases/:knowledgeBaseId/documents',
        loadComponent: () =>
          import('./features/knowledge/knowledge-documents-page').then((m) => m.KnowledgeDocumentsPage),
      },
      {
        path: 'projects/:projectId/knowledge-bases/:knowledgeBaseId',
        loadComponent: () =>
          import('./features/knowledge/knowledge-base-form-page').then((m) => m.KnowledgeBaseFormPage),
      },
      {
        path: 'projects/:projectId/agents/:agentId/knowledge-bases',
        loadComponent: () =>
          import('./features/knowledge/agent-knowledge-page').then((m) => m.AgentKnowledgePage),
      },
      {
        path: 'projects/:projectId/models',
        loadComponent: () =>
          import('./features/model-gateway/project-models-page').then((m) => m.ProjectModelsPage),
      },
      {
        path: 'projects/:projectId/agents/:agentId/models',
        loadComponent: () =>
          import('./features/model-gateway/agent-models-page').then((m) => m.AgentModelsPage),
      },
      {
        path: 'projects/:projectId/model-routing-policies/new',
        loadComponent: () =>
          import('./features/model-gateway/model-routing-policy-form-page').then(
            (m) => m.ModelRoutingPolicyFormPage,
          ),
      },
      {
        path: 'projects/:projectId/model-routing-policies/:policyId',
        loadComponent: () =>
          import('./features/model-gateway/model-routing-policy-form-page').then(
            (m) => m.ModelRoutingPolicyFormPage,
          ),
      },
      {
        path: 'projects/:projectId/model-routing-policies',
        loadComponent: () =>
          import('./features/model-gateway/model-routing-policies-list-page').then(
            (m) => m.ModelRoutingPoliciesListPage,
          ),
      },
      {
        path: 'projects/:projectId/model-usage',
        loadComponent: () =>
          import('./features/model-gateway/model-usage-dashboard-page').then((m) => m.ModelUsageDashboardPage),
      },
      {
        path: 'projects/:projectId/tools',
        loadComponent: () =>
          import('./features/tools/tools-list-page').then((m) => m.ToolsListPage),
      },
      {
        path: 'projects/:projectId/tools/new',
        loadComponent: () =>
          import('./features/tools/tool-form-page').then((m) => m.ToolFormPage),
      },
      {
        path: 'projects/:projectId/tools/:toolId',
        loadComponent: () =>
          import('./features/tools/tool-form-page').then((m) => m.ToolFormPage),
      },
      {
        path: 'projects/:projectId/agents/:agentId/tools',
        loadComponent: () =>
          import('./features/tools/agent-tools-page').then((m) => m.AgentToolsPage),
      },
      {
        path: 'projects/:projectId/executions/:executionId/tool-calls',
        loadComponent: () =>
          import('./features/tools/execution-tool-calls-page').then((m) => m.ExecutionToolCallsPage),
      },
      {
        path: 'projects/:projectId/conversations',
        loadComponent: () =>
          import('./features/conversations/conversations-list-page').then((m) => m.ConversationsListPage),
      },
      {
        path: 'projects/:projectId/conversations/new',
        loadComponent: () =>
          import('./features/conversations/conversation-form-page').then((m) => m.ConversationFormPage),
      },
      {
        path: 'projects/:projectId/conversations/:conversationId',
        loadComponent: () =>
          import('./features/conversations/conversation-detail-page').then((m) => m.ConversationDetailPage),
      },
      {
        path: 'projects/:projectId/agents/:agentId/conversations',
        loadComponent: () =>
          import('./features/conversations/conversations-list-page').then((m) => m.ConversationsListPage),
      },
      {
        path: 'projects/:projectId/prompts',
        loadComponent: () =>
          import('./features/prompts/prompts-list-page').then((m) => m.PromptsListPage),
      },
      {
        path: 'projects/:projectId/prompts/new',
        loadComponent: () =>
          import('./features/prompts/prompt-form-page').then((m) => m.PromptFormPage),
      },
      {
        path: 'projects/:projectId/prompts/:promptId/edit',
        canDeactivate: [promptUnsavedGuard],
        loadComponent: () =>
          import('./features/prompts/prompt-form-page').then((m) => m.PromptFormPage),
      },
      {
        path: 'projects/:projectId/prompts/:promptId/versions',
        loadComponent: () =>
          import('./features/prompts/prompt-versions-page').then((m) => m.PromptVersionsPage),
      },
      {
        path: 'projects/:projectId/prompts/:promptId/compare',
        loadComponent: () =>
          import('./features/prompts/prompt-compare-page').then((m) => m.PromptComparePage),
      },
      {
        path: 'projects/:projectId/prompts/:promptId',
        loadComponent: () =>
          import('./features/prompts/prompt-detail-page').then((m) => m.PromptDetailPage),
      },
      {
        path: 'agents',
        redirectTo: 'projects',
        pathMatch: 'full',
      },
      {
        path: 'prompts',
        redirectTo: 'projects',
        pathMatch: 'full',
      },
      {
        path: 'feedback',
        loadComponent: () =>
          import('./features/feedback/feedback-page').then((m) => m.FeedbackPage),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings-page').then((m) => m.SettingsPage),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
