import type {
  ApiError,
  AuditLogEntry,
  CurrentUserProfile,
  UpdateUserRoleRequest,
} from './identityTypes'

export class IdentityApiError extends Error {
  readonly details?: ApiError

  constructor(message: string, details?: ApiError) {
    super(message)
    this.name = 'IdentityApiError'
    this.details = details
  }
}

async function throwApiError(response: Response): Promise<never> {
  let details: ApiError | undefined
  try {
    details = (await response.json()) as ApiError
  } catch {
    details = undefined
  }
  throw new IdentityApiError(
    details?.message ?? `Request failed with status ${response.status}`,
    details,
  )
}

export async function fetchCurrentUser(): Promise<CurrentUserProfile> {
  const response = await fetch('/api/me')
  if (!response.ok) {
    await throwApiError(response)
  }
  return (await response.json()) as CurrentUserProfile
}

export async function fetchAuditEvents(): Promise<AuditLogEntry[]> {
  const response = await fetch('/api/audit')
  if (!response.ok) {
    await throwApiError(response)
  }
  return (await response.json()) as AuditLogEntry[]
}

export async function updateUserRole(
  userId: string,
  request: UpdateUserRoleRequest,
): Promise<CurrentUserProfile> {
  const response = await fetch(`/api/users/${userId}/role`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
  if (!response.ok) {
    await throwApiError(response)
  }
  return (await response.json()) as CurrentUserProfile
}
