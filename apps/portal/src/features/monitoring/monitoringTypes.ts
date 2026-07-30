export type HealthStatus =
  | 'HEALTHY'
  | 'DEGRADED'
  | 'UNHEALTHY'
  | 'UNKNOWN'

export type EventSeverity =
  | 'INFO'
  | 'WARNING'
  | 'ERROR'

export type EnvironmentEventType =
  | 'CREATED'
  | 'DEPLOYMENT_STARTED'
  | 'DEPLOYMENT_SUCCEEDED'
  | 'DEPLOYMENT_FAILED'
  | 'POD_RESTARTED'
  | 'HEALTH_DEGRADED'
  | 'HEALTH_RECOVERED'
  | 'EXPIRED'
  | 'DELETED'

export type MetricResponse = {
  environmentId: string
  environmentName: string
  namespace: string
  status: HealthStatus
  cpuUsagePercent: number | null
  memoryUsageBytes: number | null
  requestRatePerSecond: number | null
  errorRatePercent: number | null
  capturedAt: string
}

export type EventResponse = {
  id: string
  environmentId: string
  eventType: EnvironmentEventType
  severity: EventSeverity
  source: string
  message: string
  occurredAt: string
}
