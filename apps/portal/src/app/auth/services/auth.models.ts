export interface AuthUser {
  userId: string;
  organizationId: string;
  email: string;
  displayName: string;
  roles: string[];
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}
