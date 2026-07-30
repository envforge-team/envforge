import { useState } from 'react';

type Props = {
  environmentName: string;
  onSubmit: (version: string) => Promise<void>;
};

export function UpdateEnvironmentForm({ environmentName, onSubmit }: Props) {
  const [version, setVersion] = useState('');
  const [status, setStatus] = useState<'idle' | 'submitting' | 'success' | 'error'>('idle');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setStatus('submitting');
    try {
      await onSubmit(version);
      setStatus('success');
    } catch {
      setStatus('error');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <h3>Update {environmentName}</h3>
      <input value={version} onChange={(e) => setVersion(e.target.value)} placeholder="1.4.2" />
      <button type="submit" disabled={status === 'submitting'}>Deploy</button>
      {status === 'success' && <p>Rollout inițiat.</p>}
      {status === 'error' && <p>Eroare la actualizare.</p>}
    </form>
  );
}