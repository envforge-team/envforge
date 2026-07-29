import type { CurrentUserProfile, UserRole } from './identityTypes'

export type ProfilePageProps = {
  user: CurrentUserProfile | null
  isLoading?: boolean
}

const roleDescriptions: Record<UserRole, string> = {
  USER: 'Read-only access to environments and deployment history.',
  OPERATOR: 'Can update, rollback and manage the environments you own.',
  ADMIN: 'Full access, including user roles and the audit history.',
}

export function ProfilePage({
  user,
  isLoading = false,
}: ProfilePageProps) {
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
          <p className="eyebrow">Profile</p>
          <h2>Your account</h2>
        </section>
        {isLoading && <p>Loading profile...</p>}
        {!isLoading && user && (
          <section className="result" aria-live="polite">
            <div>
              <span className="result-status">{user.role}</span>
              <h3>{user.displayName}</h3>
              <p>{roleDescriptions[user.role]}</p>
            </div>
            <dl>
              <div>
                <dt>Email</dt>
                <dd>{user.email}</dd>
              </div>
              <div>
                <dt>Member since</dt>
                <dd>
                  {new Date(user.createdAt).toLocaleDateString()}
                </dd>
              </div>
            </dl>
          </section>
        )}
        {!isLoading && !user && (
          <section className="error-message" role="alert">
            <strong>Not signed in</strong>
            <p>Sign in to view your profile.</p>
          </section>
        )}
      </main>
    </div>
  )
}

export default ProfilePage
