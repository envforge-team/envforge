import { useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

import type {
  CreateEnvironmentRequest,
  EnvironmentResponse,
} from './features/environments/environmentTypes'

const initialForm: CreateEnvironmentRequest = {
  name: '',
  template: 'STATIC_WEB',
  imageVersion: '0.1.0',
  replicas: 1,
  resourceProfile: 'SMALL',
  lifetimeHours: 2,
  monitoringEnabled: true,
}

function App() {
  const [form, setForm] =
    useState<CreateEnvironmentRequest>(initialForm)

  const [submittedEnvironment, setSubmittedEnvironment] =
    useState<CreateEnvironmentRequest | null>(null)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    console.log('Environment request:', form)
    setSubmittedEnvironment(form)
  }

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
          <p className="eyebrow">Create environment</p>
          <h2>Provision a temporary sandbox</h2>
          <p>
            Configure an isolated Kubernetes environment. EnvForge will later
            create its namespace, install the selected application through Helm
            and monitor its health.
          </p>
        </section>

        <form
          className="environment-form"
          onSubmit={handleSubmit}
        >
          <div className="field field-full">
            <label htmlFor="name">Environment name</label>
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
                setForm({
                  ...form,
                  template:
                    event.target
                      .value as CreateEnvironmentRequest['template'],
                })
              }
            >
              <option value="STATIC_WEB">
                Static Web App
              </option>
              <option value="RELIABILITY_API">
                Reliability Demo API
              </option>
            </select>
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
                  imageVersion: event.target.value,
                })
              }
              placeholder="0.1.0"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="replicas">Replicas</label>
            <input
              id="replicas"
              type="number"
              min={1}
              max={5}
              value={form.replicas}
              onChange={(event) =>
                setForm({
                  ...form,
                  replicas: Number(event.target.value),
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
              <option value="MEDIUM">Medium</option>
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
                  lifetimeHours: Number(event.target.value),
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
                    monitoringEnabled: event.target.checked,
                  })
                }
              />

              <span>
                <strong>Enable monitoring</strong>
                <small>
                  Collect Prometheus metrics and display them in Grafana.
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
                setSubmittedEnvironment(null)
              }}
            >
              Reset
            </button>

            <button
              type="submit"
              className="primary-button"
            >
              Create environment
            </button>
          </div>
        </form>

        {submittedEnvironment && (
          <section
            className="result"
            aria-live="polite"
          >
            <div>
              <span className="result-status">
                REQUESTED
              </span>
              <h3>{submittedEnvironment.name}</h3>
              <p>
                The request was validated locally. It is not yet sent to the
                Spring Boot API.
              </p>
            </div>

            <dl>
              <div>
                <dt>Namespace</dt>
                <dd>
                  env-{submittedEnvironment.name}
                </dd>
              </div>

              <div>
                <dt>Template</dt>
                <dd>
                  {submittedEnvironment.template}
                </dd>
              </div>

              <div>
                <dt>Replicas</dt>
                <dd>
                  {submittedEnvironment.replicas}
                </dd>
              </div>

              <div>
                <dt>Lifetime</dt>
                <dd>
                  {submittedEnvironment.lifetimeHours} hours
                </dd>
              </div>
            </dl>
          </section>
        )}
      </main>
    </div>
  )
}

export default App