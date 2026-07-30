import type { DeploymentResponse } from './deploymentTypes';

type Props = { deployments: DeploymentResponse[] };

export function DeploymentHistoryTable({ deployments }: Props) {
  return (
    <table>
      <thead>
        <tr><th>Version</th><th>Status</th><th>Triggered by</th><th>Started at</th></tr>
      </thead>
      <tbody>
        {deployments.map((d) => (
          <tr key={d.id}>
            <td>{d.requestedVersion}</td>
            <td>{d.status}</td>
            <td>{d.triggeredBy}</td>
            <td>{new Date(d.startedAt).toLocaleString()}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}