import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

import {
  createEnvironment,
  EnvironmentApiError,
  getEnvironment,
  getTemplates,
} from './features/environments/environmentApi'

import type {
  CreateEnvironmentRequest,
  EnvironmentResponse,
  EnvironmentStatus,
  TemplateResponse,
} from './features/environments/environmentTypes'

import { EnvironmentDeploymentsPage } from './features/deployments/EnvironmentDeploymentsPage'
import { EnvironmentRuntimeCard } from './features/environments/EnvironmentRuntimeCard'

const initialForm: CreateEnvironmentRequest = {
  name: '',
  template: 'STATIC_WEB',
  imageVersion: '0.1.0',
  replicas: 1,
  resourceProfile: 'SMALL',
  lifetimeHours: 2,
  monitoringEnabled: true,
}

const provisioningStatuses = new Set<EnvironmentStatus>([
  'REQUESTED',
  'PROVISIONING',
  'DEPLOYING',
])

function isProvisioningStatus(
  status: EnvironmentStatus,
): boolean {
  return provisioningStatuses.has(status)
}

function App() {
  const [form, setForm] =
    useState<CreateEnvironmentRequest>(initialForm)

  const [createdEnvironment, setCreatedEnvironment] =
    useState<EnvironmentResponse | null>(null)

  const [templates, setTemplates] =
    useState<TemplateResponse[]>([])

  const [templatesLoading, setTemplatesLoading] =
    useState(true)

  const [templatesError, setTemplatesError] =
    useState<string | null>(null)

  const [isSubmitting, setIsSubmitting] =
    useState(false)

  const [submitError, setSubmitError] =
    useState<string | null>(null)

  const [statusRefreshError, setStatusRefreshError] =
    useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    async function loadTemplates() {
      setTemplatesLoading(true)
      setTemplatesError(null)

      try {
        const loadedTemplates = await getTemplates(
          controller.signal,
        )

        setTemplates(loadedTemplates)

        setForm((currentForm) => {
          const selectedTemplate =
            loadedTemplates.find(
              (template) =>
                template.code === currentForm.template,
            ) ?? loadedTemplates[0]

          if (!selectedTemplate) {
            return currentForm
          }

          return {
            ...currentForm,
            template: selectedTemplate.code,
            imageVersion:
              selectedTemplate.defaultImageVersion,
          }
        })
      } catch (error) {
        if (
          error instanceof DOMException &&
          error.name === 'AbortError'
        ) {
          return
        }

        setTemplatesError(
          'Application templates could not be loaded.',
        )
      } finally {
        if (!controller.signal.aborted) {
          setTemplatesLoading(false)
        }
      }
    }

    void loadTemplates()

    return () => {
      controller.abort()
    }
  }, [])

  useEffect(() => {
    const environmentId = createdEnvironment?.id
    const environmentStatus =
      createdEnvironment?.status

    if (
      !environmentId ||
      !environmentStatus ||
      !isProvisioningStatus(environmentStatus)
    ) {
      return
    }

    const environmentIdToPoll: string = environmentId
    const controller = new AbortController()
    let timeoutId: number | undefined

    async function refreshEnvironmentStatus() {
      try {
        const environment = await getEnvironment(
          environmentIdToPoll,
          controller.signal,
        )

        if (controller.signal.aborted) {
          return
        }

        setCreatedEnvironment(environment)
        setStatusRefreshError(null)

        if (isProvisioningStatus(environment.status)) {
          timeoutId = window.setTimeout(
            refreshEnvironmentStatus,
            2000,
          )
        }
      } catch (error) {
        if (
          error instanceof DOMException &&
          error.name === 'AbortError'
        ) {
          return
        }

        if (controller.signal.aborted) {
          return
        }

        setStatusRefreshError(
          'The latest provisioning status could not be loaded. EnvForge will try again.',
        )

        timeoutId = window.setTimeout(
          refreshEnvironmentStatus,
          5000,
        )
      }
    }

    setStatusRefreshError(null)

    timeoutId = window.setTimeout(
      refreshEnvironmentStatus,
      1500,
    )

    return () => {
      controller.abort()

      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId)
      }
    }
  }, [
    createdEnvironment?.id,
    createdEnvironment?.status,
  ])

  function handleTemplateChange(
    templateCode: string,
  ) {
    const selectedTemplate = templates.find(
      (template) => template.code === templateCode,
    )

    if (!selectedTemplate) {
      return
    }

    setForm({
      ...form,
      template: selectedTemplate.code,
      imageVersion:
        selectedTemplate.defaultImageVersion,
    })
  }

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()

    setIsSubmitting(true)
    setSubmitError(null)
    setStatusRefreshError(null)
    setCreatedEnvironment(null)

    try {
      const environment = await createEnvironment(form)
      setCreatedEnvironment(environment)
    } catch (error) {
      if (error instanceof EnvironmentApiError) {
        setSubmitError(error.message)
      } else {
        setSubmitError(
          'The Control API is unavailable. Please try again.',
        )
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const isProvisioning =
    createdEnvironment !== null &&
    isProvisioningStatus(createdEnvironment.status)

  return (
    <div className="app">
      <header className="header">
        <div>
          <span className="product-label">
            SELF-SERVICE AKS PLATFORM
          </span>
          <h1>EnvForge</h1>
        </div>

        <span className="environment-badge">
          Local development
        </span>
      </header>

      <main className="content">
        <section className="introduction">
          <p className="eyebrow">
            Create environment
          </p>

          <h2>Provision a temporary sandbox</h2>

          <p>
            Configure an isolated Kubernetes environment.
            EnvForge creates its namespace, installs the
            selected application through Helm and monitors
            its health.
          </p>
        </section>

        <form
          className="environment-form"
          onSubmit={handleSubmit}
        >
          <div className="field field-full">
            <label htmlFor="name">
              Environment name
            </label>

            <input
              id="name"
              name="name"
              type="text"
              placeholder="api-demo-bogdan"
              value={form.name}
              onChange={(event) =>
                setForm({
                  ...form,
                  name: event.target.value,
                })
              }
              minLength={3}
              maxLength={40}
              pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
              required
            />

            <small>
              Use lowercase letters, numbers and hyphens.
            </small>
          </div>

          <div className="field">
            <label htmlFor="template">
              Application template
            </label>

            <select
              id="template"
              value={form.template}
              onChange={(event) =>
                handleTemplateChange(
                  event.target.value,
                )
              }
              disabled={
                templatesLoading ||
                templates.length === 0
              }
            >
              {templatesLoading && (
                <option value="">
                  Loading templates...
                </option>
              )}

              {!templatesLoading &&
                templates.length === 0 && (
                  <option value="">
                    No templates available
                  </option>
                )}

              {templates.map((template) => (
                <option
                  key={template.id}
                  value={template.code}
                >
                  {template.displayName}
                </option>
              ))}
            </select>

            {templatesError && (
              <small className="field-error">
                {templatesError}
              </small>
            )}
          </div>

          <div className="field">
            <label htmlFor="imageVersion">
              Image version
            </label>

            <input
              id="imageVersion"
              type="text"
              value={form.imageVersion}
              onChange={(event) =>
                setForm({
                  ...form,
                  imageVersion:
                    event.target.value,
                })
              }
              placeholder="0.2.0"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="replicas">
              Replicas
            </label>

            <input
              id="replicas"
              type="number"
              min={1}
              max={5}
              value={form.replicas}
              onChange={(event) =>
                setForm({
                  ...form,
                  replicas: Number(
                    event.target.value,
                  ),
                })
              }
              required
            />
          </div>

          <div className="field">
            <label htmlFor="resourceProfile">
              Resource profile
            </label>

            <select
              id="resourceProfile"
              value={form.resourceProfile}
              onChange={(event) =>
                setForm({
                  ...form,
                  resourceProfile:
                    event.target
                      .value as CreateEnvironmentRequest['resourceProfile'],
                })
              }
            >
              <option value="SMALL">Small</option>
              <option value="MEDIUM">
                Medium
              </option>
              <option value="LARGE">Large</option>
            </select>
          </div>

          <div className="field">
            <label htmlFor="lifetimeHours">
              Lifetime
            </label>

            <select
              id="lifetimeHours"
              value={form.lifetimeHours}
              onChange={(event) =>
                setForm({
                  ...form,
                  lifetimeHours: Number(
                    event.target.value,
                  ),
                })
              }
            >
              <option value={1}>1 hour</option>
              <option value={2}>2 hours</option>
              <option value={4}>4 hours</option>
              <option value={8}>8 hours</option>
              <option value={24}>24 hours</option>
            </select>
          </div>

          <div className="field checkbox-field">
            <label htmlFor="monitoringEnabled">
              <input
                id="monitoringEnabled"
                type="checkbox"
                checked={form.monitoringEnabled}
                onChange={(event) =>
                  setForm({
                    ...form,
                    monitoringEnabled:
                      event.target.checked,
                  })
                }
              />

              <span>
                <strong>Enable monitoring</strong>

                <small>
                  Collect Prometheus metrics and display
                  them in Grafana.
                </small>
              </span>
            </label>
          </div>

          <div className="actions field-full">
            <button
              type="button"
              className="secondary-button"
              onClick={() => {
                setForm(initialForm)
                setCreatedEnvironment(null)
                setSubmitError(null)
                setStatusRefreshError(null)
              }}
            >
              Reset
            </button>

            <button
              type="submit"
              className="primary-button"
              disabled={
                isSubmitting ||
                templatesLoading ||
                templates.length === 0
              }
            >
              {isSubmitting
                ? 'Creating environment...'
                : 'Create environment'}
            </button>
          </div>
        </form>

        {submitError && (
          <section
            className="error-message"
            role="alert"
          >
            <strong>
              Environment could not be created
            </strong>

            <p>{submitError}</p>
          </section>
        )}

        {createdEnvironment && (
          <section
            className="result"
            aria-live="polite"
          >
            <div>
              <span className="result-status">
                {createdEnvironment.status}
              </span>

              <h3>{createdEnvironment.name}</h3>

              <p>
                {isProvisioning
                  ? 'EnvForge is creating the Kubernetes namespace and installing the Helm release.'
                  : createdEnvironment.status ===
                    'READY'
                    ? 'The Kubernetes environment is ready to use.'
                    : createdEnvironment.status ===
                      'FAILED'
                      ? 'Provisioning failed. Check the Control API and Kubernetes events.'
                      : `Environment lifecycle status: ${createdEnvironment.status}.`}
              </p>

              {isProvisioning && (
                <div
                  className="provisioning-progress"
                  role="status"
                >
                  <span
                    className="provisioning-spinner"
                    aria-hidden="true"
                  />

                  Refreshing provisioning status…
                </div>
              )}

              {statusRefreshError && (
                <p
                  className="status-refresh-error"
                  role="alert"
                >
                  {statusRefreshError}
                </p>
              )}
            </div>

            <dl>
              <div>
                <dt>Namespace</dt>
                <dd>
                  {createdEnvironment.namespace}
                </dd>
              </div>

              <div>
                <dt>Template</dt>
                <dd>
                  {createdEnvironment.template}
                </dd>
              </div>

              <div>
                <dt>Replicas</dt>
                <dd>
                  {createdEnvironment.replicas}
                </dd>
              </div>

              <div>
                <dt>Expires at</dt>
                <dd>
                  {new Date(
                    createdEnvironment.expiresAt,
                  ).toLocaleString()}
                </dd>
              </div>
            </dl>
          </section>
        )}

        {createdEnvironment?.status === 'READY' && (
          <EnvironmentRuntimeCard
            environmentId={createdEnvironment.id}
          />
        )}


        {createdEnvironment?.status ===
          'READY' && (
            <section className="deployments">
              <h3>
                Deployments for{' '}
                {createdEnvironment.name}
              </h3>

              <EnvironmentDeploymentsPage
                environmentId={
                  createdEnvironment.id
                }
                environmentName={
                  createdEnvironment.name
                }
              />
            </section>
          )}
      </main>
    </div>
  )
}

export default App