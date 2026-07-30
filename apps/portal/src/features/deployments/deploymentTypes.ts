export type DeploymentStatus = 'PENDING' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILED' | 'ROLLED_BACK';

export interface DeploymentResponse {
  id: number;
  environmentId: number;
  requestedVersion: string;
  imageTag: string;
  status: DeploymentStatus;
  triggeredBy: string;
  startedAt: string;
  finishedAt: string | null;
  failureReason: string | null;
}

export interface UpdateEnvironmentRequest {
  version: string;
}