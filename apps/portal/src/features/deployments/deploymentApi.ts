import type { ApiError } from '../environments/environmentTypes'
import type { DeploymentResponse, UpdateEnvironmentRequest } from './deploymentTypes'

export class DeploymentApiError extends Error {
  readonly details: ApiError

  constructor(details: ApiError) {
    super(details.message)
    this.name = 'DeploymentApiError'
    this.details = details
  }
}

export async function fetchDeploymentHistory(
  environmentId: string,
): Promise<DeploymentResponse[]> {
  const response = await fetch(`/api/environments/${environmentId}/deployments`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    const error = (await response.json()) as ApiError
    throw new DeploymentApiError(error)
  }

  return (await response.json()) as DeploymentResponse[]
}

export async function updateEnvironment(
  environmentId: string,
  version: string,
): Promise<DeploymentResponse> {
  const body: UpdateEnvironmentRequest = { version }

  const response = await fetch(`/api/environments/${environmentId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const error = (await response.json()) as ApiError
    throw new DeploymentApiError(error)
  }

  return (await response.json()) as DeploymentResponse
}
