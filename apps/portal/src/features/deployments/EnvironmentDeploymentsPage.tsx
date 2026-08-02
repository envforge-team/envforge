import { useEffect, useState } from 'react'
import { fetchDeploymentHistory, updateEnvironment } from './deploymentApi'
import type { DeploymentResponse } from './deploymentTypes'
import { DeploymentHistoryTable } from './DeploymentHistoryTable'
import { UpdateEnvironmentForm } from './UpdateEnvironmentForm'

type Props = {
  environmentId: string
  environmentName: string
}

export function EnvironmentDeploymentsPage({ environmentId, environmentName }: Props) {
  const [deployments, setDeployments] = useState<DeploymentResponse[]>([])

  useEffect(() => {
    fetchDeploymentHistory(environmentId)
      .then(setDeployments)
      .catch(() => setDeployments([]))
  }, [environmentId])

  const handleUpdate = async (version: string) => {
    const newDeployment = await updateEnvironment(environmentId, version)
    setDeployments((prev) => [newDeployment, ...prev])
  }

  return (
    <>
      <UpdateEnvironmentForm environmentName={environmentName} onSubmit={handleUpdate} />
      <DeploymentHistoryTable deployments={deployments} />
    </>
  )
}
