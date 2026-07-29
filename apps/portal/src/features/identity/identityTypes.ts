export type UserRole = 'USER' | 'OPERATOR' | 'ADMIN'

export type CurrentUserProfile = {
  id: string
  displayName: string
  email: string
  role: UserRole
  createdAt: string
}

export type UpdateUserRoleRequest = {
  role: UserRole
}

export type AuditResult = 'SUCCESS' | 'FAILURE'

export type AuditLogEntry = {
  id: string
  actor: string
  action: string
  resourceType: string
  resourceId?: string
  result: AuditResult
  createdAt: string
}

export type ApiError = {
  status: number
  error: string
  message: string
  path?: string
  timestamp: string
}
