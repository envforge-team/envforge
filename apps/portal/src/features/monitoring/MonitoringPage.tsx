import './MonitoringPage.css'

import type {
  EventSeverity,
  HealthStatus,
  EventResponse,
  MetricResponse,
} from './monitoringTypes'

export type MonitoringPageProps = {
  snapshot: MetricResponse | null
  events: EventResponse[]
  isLoading?: boolean
  errorMessage?: string | null
}

function formatPercentage(value: number | null): string {
  if (value === null) {
    return 'Unavailable'
  }

  return `${value.toFixed(1)}%`
}

function formatRequestRate(value: number | null): string {
  if (value === null) {
    return 'Unavailable'
  }

  return `${value.toFixed(1)} req/s`
}

function formatBytes(value: number | null): string {
  if (value === null) {
    return 'Unavailable'
  }

  const mebibytes = value / 1024 / 1024

  if (mebibytes >= 1024) {
    return `${(mebibytes / 1024).toFixed(2)} GiB`
  }

  return `${mebibytes.toFixed(1)} MiB`
}

function formatDateTime(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'Invalid timestamp'
  }

  return date.toLocaleString()
}

function getStatusClass(status: HealthStatus): string {
  return `monitoring-status monitoring-status-${status.toLowerCase()}`
}

function getSeverityClass(severity: EventSeverity): string {
  return `monitoring-severity monitoring-severity-${severity.toLowerCase()}`
}

export function MonitoringPage({
  snapshot,
  events,
  isLoading = false,
  errorMessage = null,
}: MonitoringPageProps) {
  return (
    <div className="monitoring-page">
      <header className="monitoring-header">
        <div>
          <span className="monitoring-product-label">
            SELF-SERVICE AKS PLATFORM
          </span>
          <h1>EnvForge</h1>
        </div>

        <span className="monitoring-page-badge">
          Environment monitoring
        </span>
      </header>

      <main className="monitoring-content">
        <section className="monitoring-introduction">
          <p className="monitoring-eyebrow">Monitoring</p>
          <h2>Environment health</h2>
          <p>
            Review the latest health snapshot, workload metrics and
            operational events for an EnvForge sandbox.
          </p>
        </section>

        {isLoading && (
          <section
            className="monitoring-message"
            aria-live="polite"
          >
            Loading environment monitoring data...
          </section>
        )}

        {!isLoading && errorMessage && (
          <section
            className="monitoring-message monitoring-message-error"
            role="alert"
          >
            <strong>Monitoring data is unavailable</strong>
            <p>{errorMessage}</p>
          </section>
        )}

        {!isLoading && !errorMessage && snapshot === null && (
          <section className="monitoring-message">
            No health snapshot is available for this environment.
          </section>
        )}

        {!isLoading && !errorMessage && snapshot && (
          <>
            <section className="monitoring-overview">
              <div className="monitoring-environment">
                <div>
                  <span className={getStatusClass(snapshot.status)}>
                    {snapshot.status}
                  </span>

                  <h3>{snapshot.environmentName}</h3>
                  <p>{snapshot.namespace}</p>
                </div>

                <div className="monitoring-snapshot-time">
                  <span>Last snapshot</span>
                  <strong>
                    {formatDateTime(snapshot.capturedAt)}
                  </strong>
                </div>
              </div>

              <div className="monitoring-metrics-grid">
                <article className="monitoring-metric-card">
                  <span>CPU usage</span>
                  <strong>
                    {formatPercentage(snapshot.cpuUsagePercent)}
                  </strong>
                  <small>Current workload utilization</small>
                </article>

                <article className="monitoring-metric-card">
                  <span>Memory usage</span>
                  <strong>
                    {formatBytes(snapshot.memoryUsageBytes)}
                  </strong>
                  <small>Memory currently used by the workload</small>
                </article>

                <article className="monitoring-metric-card">
                  <span>Request rate</span>
                  <strong>
                    {formatRequestRate(
                      snapshot.requestRatePerSecond,
                    )}
                  </strong>
                  <small>Average incoming request rate</small>
                </article>

                <article className="monitoring-metric-card">
                  <span>Error rate</span>
                  <strong>
                    {formatPercentage(snapshot.errorRatePercent)}
                  </strong>
                  <small>Percentage of failed HTTP requests</small>
                </article>
              </div>
            </section>

            <section className="monitoring-events-panel">
              <div className="monitoring-events-heading">
                <div>
                  <p className="monitoring-eyebrow">Events</p>
                  <h3>Recent environment activity</h3>
                </div>

                <span>{events.length} events</span>
              </div>

              {events.length === 0 ? (
                <p className="monitoring-events-empty">
                  No environment events have been recorded.
                </p>
              ) : (
                <div className="monitoring-events-table-wrapper">
                  <table className="monitoring-events-table">
                    <thead>
                      <tr>
                        <th>Time</th>
                        <th>Severity</th>
                        <th>Event</th>
                        <th>Source</th>
                        <th>Message</th>
                      </tr>
                    </thead>

                    <tbody>
                      {events.map((event) => (
                        <tr key={event.id}>
                          <td>{formatDateTime(event.occurredAt)}</td>
                          <td>
                            <span
                              className={getSeverityClass(
                                event.severity,
                              )}
                            >
                              {event.severity}
                            </span>
                          </td>
                          <td>{event.eventType}</td>
                          <td>{event.source}</td>
                          <td>{event.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  )
}

export default MonitoringPage
