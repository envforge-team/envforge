import type {
  EventResponse,
  MetricResponse,
} from './monitoringTypes'

export type MonitoringApiClient = {
  fetchMetrics(
    environmentId: string,
  ): Promise<MetricResponse | null>

  fetchEvents(
    environmentId: string,
  ): Promise<EventResponse[]>
}

export const monitoringApiPaths = {
  metrics(environmentId: string): string {
    return `/api/environments/${encodeURIComponent(
      environmentId,
    )}/monitoring/metrics`
  },

  events(environmentId: string): string {
    return `/api/environments/${encodeURIComponent(
      environmentId,
    )}/monitoring/events`
  },
}
