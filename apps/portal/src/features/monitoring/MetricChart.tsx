export type MetricChartProps = {
  label: string
  value: number | null
  formattedValue: string
  description: string
  maximum: number
  maximumLabel: string
}

function clampPercentage(value: number): number {
  return Math.min(100, Math.max(0, value))
}

export function MetricChart({
  label,
  value,
  formattedValue,
  description,
  maximum,
  maximumLabel,
}: MetricChartProps) {
  const hasValue =
    value !== null &&
    Number.isFinite(value) &&
    value >= 0 &&
    maximum > 0

  const percentage = hasValue
    ? clampPercentage((value / maximum) * 100)
    : 0

  const accessibleLabel = hasValue
    ? `${label}: ${formattedValue}. Scale from 0 to ${maximumLabel}.`
    : `${label}: unavailable.`

  return (
    <article className="monitoring-metric-chart">
      <div className="monitoring-metric-chart-heading">
        <span>{label}</span>
        <strong>{formattedValue}</strong>
      </div>

      <div
        className="monitoring-chart-area"
        role="img"
        aria-label={accessibleLabel}
      >
        {hasValue ? (
          <>
            <div
              className="monitoring-chart-track"
              aria-hidden="true"
            >
              <span
                className="monitoring-chart-fill"
                style={{ width: `${percentage}%` }}
              />
            </div>

            <div
              className="monitoring-chart-scale"
              aria-hidden="true"
            >
              <span>0</span>
              <span>{maximumLabel}</span>
            </div>
          </>
        ) : (
          <div className="monitoring-chart-unavailable">
            No metric data
          </div>
        )}
      </div>

      <small>{description}</small>
    </article>
  )
}
