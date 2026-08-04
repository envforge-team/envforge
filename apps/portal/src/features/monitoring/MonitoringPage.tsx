import './MonitoringPage.css'

import { MetricChart } from './MetricChart'

import type {
  EventResponse,
  EventSeverity,
  HealthStatus,
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

  if (value < 1024) {
    return `${value.toFixed(0)} B`
  }

  const kibibytes = value / 1024

  if (kibibytes < 1024) {
    return `${kibibytes.toFixed(1)} KiB`
  }

  const mebibytes = kibibytes / 1024

  if (mebibytes < 1024) {
    return `${mebibytes.toFixed(1)} MiB`
  }

  return `${(mebibytes / 1024).toFixed(2)} GiB`
}

function formatMemoryScale(mebibytes: number): string {
  if (mebibytes >= 1024) {
    return `${(mebibytes / 1024).toFixed(1)} GiB`
  }

  return `${mebibytes.toFixed(0)} MiB`
}

function formatRequestRateScale(value: number): string {
  if (value < 10) {
    return `${value.toFixed(1)} req/s`
  }

  return `${value.toFixed(0)} req/s`
}

function formatDateTime(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'Invalid timestamp'
  }

  return date.toLocaleString()
}

function getNiceMaximum(value: number | null): number {
  if (
    value === null ||
    !Number.isFinite(value) ||
    value <= 0
  ) {
    return 1
  }

  const exponent = Math.floor(Math.log10(value))
  const magnitude = 10 ** exponent
  const normalizedValue = value / magnitude

  let roundedValue: number

  if (normalizedValue <= 1) {
    roundedValue = 1
  } else if (normalizedValue <= 2) {
    roundedValue = 2
  } else if (normalizedValue <= 5) {
    roundedValue = 5
  } else {
    roundedValue = 10
  }

  return roundedValue * magnitude
}

function getStatusClass(status: HealthStatus): string {
  return `monitoring-status monitoring-status-${status.toLowerCase()}`
}

function getSeverityClass(
  severity: EventSeverity,
): string {
  return `monitoring-severity monitoring-severity-${severity.toLowerCase()}`
}

export function MonitoringPage({
  snapshot,
  events,
  isLoading = false,
  errorMessage = null,
}: MonitoringPageProps) {
  const memoryUsageMebibytes =
    snapshot?.memoryUsageBytes === null ||
    snapshot?.memoryUsageBytes === undefined
      ? null
      : snapshot.memoryUsageBytes / 1024 / 1024

  const memoryMaximum = getNiceMaximum(
    memoryUsageMebibytes,
  )

  const requestRateMaximum = getNiceMaximum(
    snapshot?.requestRatePerSecond ?? null,
  )

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
          <p className="monitoring-eyebrow">
            Monitoring
          </p>

          <h2>Environment health</h2>

          <p>
            Review the latest health snapshot, workload
            metrics and operational events for an EnvForge
            sandbox.
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
            <strong>
              Monitoring data is unavailable
            </strong>

            <p>{errorMessage}</p>
          </section>
        )}

        {!isLoading &&
          !errorMessage &&
          snapshot === null && (
            <section className="monitoring-message">
              No health snapshot is available for this
              environment.
            </section>
          )}

        {!isLoading && !errorMessage && snapshot && (
          <section className="monitoring-overview">
            <div className="monitoring-environment">
              <div>
                <span
                  className={getStatusClass(
                    snapshot.status,
                  )}
                >
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
              <MetricChart
                label="CPU usage"
                value={snapshot.cpuUsagePercent}
                formattedValue={formatPercentage(
                  snapshot.cpuUsagePercent,
                )}
                description="Current workload utilization"
                maximum={100}
                maximumLabel="100%"
              />

              <MetricChart
                label="Memory usage"
                value={memoryUsageMebibytes}
                formattedValue={formatBytes(
                  snapshot.memoryUsageBytes,
                )}
                description="Memory currently used by the workload"
                maximum={memoryMaximum}
                maximumLabel={formatMemoryScale(
                  memoryMaximum,
                )}
              />

              <MetricChart
                label="Request rate"
                value={snapshot.requestRatePerSecond}
                formattedValue={formatRequestRate(
                  snapshot.requestRatePerSecond,
                )}
                description="Average incoming request rate"
                maximum={requestRateMaximum}
                maximumLabel={formatRequestRateScale(
                  requestRateMaximum,
                )}
              />

              <MetricChart
                label="Error rate"
                value={snapshot.errorRatePercent}
                formattedValue={formatPercentage(
                  snapshot.errorRatePercent,
                )}
                description="Percentage of failed HTTP requests"
                maximum={100}
                maximumLabel="100%"
              />
            </div>
          </section>
        )}

        {!isLoading && !errorMessage && (
          <section className="monitoring-events-panel">
            <div className="monitoring-events-heading">
              <div>
                <p className="monitoring-eyebrow">
                  Events
                </p>

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
                        <td>
                          {formatDateTime(
                            event.occurredAt,
                          )}
                        </td>

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
        )}
      </main>
    </div>
  )
}

export default MonitoringPage
