import type { ApiError } from './environmentTypes'
import type { EnvironmentRuntimeResponse } from './environmentRuntimeTypes'

export class EnvironmentRuntimeApiError extends Error {
  readonly details: ApiError

  constructor(details: ApiError) {
    super(details.message)
    this.name = 'EnvironmentRuntimeApiError'
    this.details = details
  }
}

export async function getEnvironmentRuntime(
  environmentId: string,
  signal?: AbortSignal,
): Promise<EnvironmentRuntimeResponse> {
  const response = await fetch(
    `/api/environments/${encodeURIComponent(environmentId)}/runtime`,
    {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
      signal,
    },
  )

  if (!response.ok) {
    const error = (await response.json()) as ApiError
    throw new EnvironmentRuntimeApiError(error)
  }

  return (
    await response.json()
  ) as EnvironmentRuntimeResponse
}