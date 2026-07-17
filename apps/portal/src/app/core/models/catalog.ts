export interface Organization {
  id: string;
  name: string;
  slug: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

export interface OrganizationRequest {
  name: string;
  slug?: string;
}

export type ProjectStatus = 'ACTIVE' | 'ARCHIVED' | 'DRAFT';
export type ProjectVisibility = 'PRIVATE' | 'INTERNAL' | 'PUBLIC';

export interface Project {
  id: string;
  organizationId: string;
  name: string;
  description: string | null;
  status: ProjectStatus;
  visibility: ProjectVisibility;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

export interface ProjectRequest {
  name: string;
  description?: string | null;
  status: ProjectStatus;
  visibility: ProjectVisibility;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
