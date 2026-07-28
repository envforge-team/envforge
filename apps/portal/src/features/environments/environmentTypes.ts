export type EnvironmentTemplate =
  | 'STATIC_WEB'
  | 'RELIABILITY_API'

export type ResourceProfile =
  | 'SMALL'
  | 'MEDIUM'
  | 'LARGE'

export type EnvironmentStatus =
  | 'REQUESTED'
  | 'PROVISIONING'
  | 'DEPLOYING'
  | 'READY'
  | 'DEGRADED'
  | 'FAILED'
  | 'DELETING'
  | 'DELETED'
  | 'EXPIRED'

export type CreateEnvironmentRequest = {
  name: string
  template: EnvironmentTemplate
  imageVersion: string
  replicas: number
  resourceProfile: ResourceProfile
  lifetimeHours: number
  monitoringEnabled: boolean
}

export type EnvironmentResponse = {
  id: string
  name: string
  namespace: string
  template: EnvironmentTemplate
  imageVersion: string
  replicas: number
  resourceProfile: ResourceProfile
  status: EnvironmentStatus
  monitoringEnabled: boolean
  createdBy: string
  createdAt: string
  expiresAt: string
  updatedAt: string
}

export type ApiError = {
  status: number
  error: string
  message: string
  path?: string
  timestamp: string
}