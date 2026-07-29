import type { AuditLogEntry } from './identityTypes'

export type AuditPageProps = {
  events: AuditLogEntry[]
  isLoading?: boolean
}

export function AuditPage({
  events,
  isLoading = false,
}: AuditPageProps) {
  return (
    <div className="app">
      <header className="header">
        <div>
          <span className="product-label">
            SELF-SERVICE AKS PLATFORM
          </span>
          <h1>EnvForge</h1>
        </div>
      </header>
      <main className="content">
        <section className="introduction">
          <p className="eyebrow">Audit</p>
          <h2>Recent activity</h2>
          <p>
            Who did what, and when — across environments, users and
            rollbacks.
          </p>
        </section>
        {isLoading && <p>Loading audit log...</p>}
        {!isLoading && events.length === 0 && (
          <p>No audit events recorded yet.</p>
        )}
        {!isLoading && events.length > 0 && (
          <table className="audit-table">
            <thead>
              <tr>
                <th>When</th>
                <th>Actor</th>
                <th>Action</th>
                <th>Resource</th>
                <th>Result</th>
              </tr>
            </thead>
            <tbody>
              {events.map((event) => (
                <tr key={event.id}>
                  <td>
                    {new Date(event.createdAt).toLocaleString()}
                  </td>
                  <td>{event.actor}</td>
                  <td>{event.action}</td>
                  <td>
                    {event.resourceType}
                    {event.resourceId ? ` / ${event.resourceId}` : ''}
                  </td>
                  <td>
                    <span
                      className={
                        event.result === 'SUCCESS'
                          ? 'audit-result-success'
                          : 'audit-result-failure'
                      }
                    >
                      {event.result}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </div>
  )
}

export default AuditPage
