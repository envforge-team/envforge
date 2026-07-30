import type { DeploymentResponse } from './deploymentTypes';

const mockDeployments: DeploymentResponse[] = [
  {
    id: 1, environmentId: 1, requestedVersion: '1.3.0', imageTag: 'acr.io/api:1.3.0',
    status: 'SUCCESS', triggeredBy: 'raoul',
    startedAt: '2026-07-20T10:00:00Z', finishedAt: '2026-07-20T10:03:00Z', failureReason: null,
  },
];

export function fetchDeploymentHistory(environmentId: number): Promise<DeploymentResponse[]> {
  return new Promise((resolve) =>
    setTimeout(() => resolve(mockDeployments.filter((d) => d.environmentId === environmentId)), 400)
  );
}

export function updateEnvironment(environmentId: number, version: string): Promise<DeploymentResponse> {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (!/^\d+\.\d+\.\d+$/.test(version)) {
        reject(new Error('Invalid version format'));
        return;
      }
      const newDeployment: DeploymentResponse = {
        id: mockDeployments.length + 1,
        environmentId,
        requestedVersion: version,
        imageTag: `acr.io/api:${version}`,
        status: 'IN_PROGRESS',
        triggeredBy: 'raoul',
        startedAt: new Date().toISOString(),
        finishedAt: null,
        failureReason: null,
      };
      mockDeployments.push(newDeployment);
      resolve(newDeployment);
    }, 600);
  });
}