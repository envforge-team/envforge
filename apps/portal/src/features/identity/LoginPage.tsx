export type LoginPageProps = {
  onSignIn?: () => void
  isSigningIn?: boolean
  error?: string | null
}

export function LoginPage({
  onSignIn,
  isSigningIn = false,
  error = null,
}: LoginPageProps) {
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
          <p className="eyebrow">Sign in</p>
          <h2>Access your EnvForge account</h2>
          <p>
            Sign in with your organization&apos;s Microsoft account to manage
            environments, view deployment history and monitor reliability.
          </p>
        </section>
        <div className="actions field-full">
          <button
            type="button"
            className="primary-button"
            onClick={onSignIn}
            disabled={isSigningIn}
          >
            {isSigningIn ? 'Signing in...' : 'Sign in with Microsoft'}
          </button>
        </div>
        {error && (
          <section className="error-message" role="alert">
            <strong>Sign in failed</strong>
            <p>{error}</p>
          </section>
        )}
      </main>
    </div>
  )
}

export default LoginPage
