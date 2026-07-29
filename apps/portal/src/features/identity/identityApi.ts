import type {
  AuditLogEntry,
  CurrentUserProfile,
  UpdateUserRoleRequest,
} from './identityTypes'

export class IdentityApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'IdentityApiError'
  }
}

// NOTE: /api/me, /api/users/{id}/role and /api/audit are not implemented
// on the backend yet (planned for Săptămâna 3, Zi 12-13: Authorization/
// AuditService + endpoints). Until then these functions return simulated
// data so the UI can be built and exercised end-to-end. Swap the bodies
// for real `fetch` calls once the endpoints exist — keep the same
// function signatures so the pages don't need to change.

const simulatedCurrentUser: CurrentUserProfile = {
  id: '00000000-0000-0000-0000-000000000001',
  displayName: 'Silvius Lombrea',
  email: 'silvius.lombrea@gmail.com',
  role: 'ADMIN',
  createdAt: '2026-07-01T09:00:00Z',
}

const simulatedAuditEvents: AuditLogEntry[] = [
  {
    id: 'a1',
    actor: 'silvius.lombrea@gmail.com',
    action: 'ENVIRONMENT_UPDATE',
    resourceType: 'ENVIRONMENT',
    resourceId: 'api-demo-bogdan',
    result: 'SUCCESS',
    createdAt: '2026-07-29T18:32:00Z',
  },
  {
    id: 'a2',
    actor: 'silvius.lombrea@gmail.com',
    action: 'ROLE_ASSIGNED',
    resourceType: 'USER',
    resourceId: 'colleague@envforge.dev',
    result: 'SUCCESS',
    createdAt: '2026-07-28T14:10:00Z',
  },
]

export async function fetchCurrentUser(): Promise<CurrentUserProfile> {
  return simulatedCurrentUser
}

export async function fetchAuditEvents(): Promise<AuditLogEntry[]> {
  return simulatedAuditEvents
}

export async function updateUserRole(
  userId: string,
  request: UpdateUserRoleRequest,
): Promise<CurrentUserProfile> {
  return { ...simulatedCurrentUser, id: userId, role: request.role }
}
