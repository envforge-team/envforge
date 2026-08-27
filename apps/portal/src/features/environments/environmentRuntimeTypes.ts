export type EnvironmentRuntimeResponse = {
  environmentId: string
  environmentName: string
  namespace: string
  namespaceExists: boolean
  helmRelease: string
  helmStatus: string
  deploymentName: string | null
  desiredReplicas: number | null
  readyReplicas: number | null
  serviceName: string | null
  observedAt: string
}