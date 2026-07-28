import type {
  ApiError,
  CreateEnvironmentRequest,
  EnvironmentResponse,
} from './environmentTypes'

export class EnvironmentApiError extends Error {
  readonly details: ApiError

  constructor(details: ApiError) {
    super(details.message)
    this.name = 'EnvironmentApiError'
    this.details = details
  }
}

export async function createEnvironment(
  request: CreateEnvironmentRequest,
): Promise<EnvironmentResponse> {
  const response = await fetch('/api/environments', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    const error = (await response.json()) as ApiError
    throw new EnvironmentApiError(error)
  }

  return (await response.json()) as EnvironmentResponse
}