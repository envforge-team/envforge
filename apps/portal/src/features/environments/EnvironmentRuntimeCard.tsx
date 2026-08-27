import { useEffect, useState } from 'react'

import { getEnvironmentRuntime } from './environmentRuntimeApi'
import type { EnvironmentRuntimeResponse } from './environmentRuntimeTypes'

import './EnvironmentRuntimeCard.css'

type EnvironmentRuntimeCardProps = {
  environmentId: string
}

export function EnvironmentRuntimeCard({
  environmentId,
}: EnvironmentRuntimeCardProps) {
  const [runtime, setRuntime] =
    useState<EnvironmentRuntimeResponse | null>(null)

  const [isLoading, setIsLoading] =
    useState(true)

  const [error, setError] =
    useState<string | null>(null)

  const [refreshSequence, setRefreshSequence] =
    useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadRuntime() {
      setIsLoading(true)
      setError(null)

      try {
        const response = await getEnvironmentRuntime(
          environmentId,
          controller.signal,
        )

        if (!controller.signal.aborted) {
          setRuntime(response)
        }
      } catch (requestError) {
        if (
          requestError instanceof DOMException &&
          requestError.name === 'AbortError'
        ) {
          return
        }

        if (!controller.signal.aborted) {
          setError(
            'Kubernetes runtime details could not be loaded.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadRuntime()

    return () => {
      controller.abort()
    }
  }, [
    environmentId,
    refreshSequence,
  ])

  const runtimeHealthy =
  runtime !== null &&
  runtime.namespaceExists &&
  runtime.helmStatus === 'deployed' &&
  runtime.deploymentName !== null &&
  runtime.serviceName !== null &&
  runtime.desiredReplicas !== null &&
  runtime.readyReplicas !== null &&
  runtime.desiredReplicas ===
    runtime.readyReplicas

  return (
    <section
      className="runtime-card"
      aria-live="polite"
    >
      <div className="runtime-header">
        <div>
          <p className="runtime-eyebrow">
            Kubernetes runtime
          </p>

          <h3>Live environment state</h3>
        </div>

        <div className="runtime-actions">
          {runtime && (
            <span
              className={
                runtimeHealthy
                  ? 'runtime-health runtime-health-ready'
                  : 'runtime-health runtime-health-warning'
              }
            >
              {runtimeHealthy
                ? 'Healthy'
                : 'Attention required'}
            </span>
          )}

          <button
            type="button"
            className="runtime-refresh-button"
            disabled={isLoading}
            onClick={() =>
              setRefreshSequence(
                (current) => current + 1,
              )
            }
          >
            {isLoading
              ? 'Refreshing...'
              : 'Refresh'}
          </button>
        </div>
      </div>

      {isLoading && !runtime && (
        <p className="runtime-message">
          Loading Kubernetes and Helm details...
        </p>
      )}

      {error && (
        <div
          className="runtime-error"
          role="alert"
        >
          <strong>
            Runtime inspection failed
          </strong>

          <p>{error}</p>
        </div>
      )}

      {runtime && (
        <>
          <dl className="runtime-grid">
            <div>
              <dt>Namespace</dt>
              <dd>{runtime.namespace}</dd>
            </div>

            <div>
              <dt>Namespace state</dt>
              <dd>
                {runtime.namespaceExists
                  ? 'Exists'
                  : 'Missing'}
              </dd>
            </div>

            <div>
              <dt>Helm release</dt>
              <dd>{runtime.helmRelease}</dd>
            </div>

            <div>
              <dt>Helm status</dt>
              <dd>{runtime.helmStatus}</dd>
            </div>

            <div>
              <dt>Deployment</dt>
              <dd>
                {runtime.deploymentName ??
                  'Not found'}
              </dd>
            </div>

            <div>
              <dt>Ready replicas</dt>
              <dd>
                {runtime.readyReplicas ??
                  0}
                {' / '}
                {runtime.desiredReplicas ??
                  0}
              </dd>
            </div>

            <div>
              <dt>Service</dt>
              <dd>
                {runtime.serviceName ??
                  'Not found'}
              </dd>
            </div>

            <div>
              <dt>Observed at</dt>
              <dd>
                {new Date(
                  runtime.observedAt,
                ).toLocaleString()}
              </dd>
            </div>
          </dl>

          <p className="runtime-source">
            Live data retrieved from Kind through
            kubectl and Helm.
          </p>
        </>
      )}
    </section>
  )
}