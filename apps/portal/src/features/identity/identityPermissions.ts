import type { UserRole } from './identityTypes'

/**
 * Human-readable list of operations permitted for a given role.
 * Mirrors the rules enforced server-side by AuthorizationService —
 * update this alongside any change there.
 */
export function permittedOperations(role: UserRole): string[] {
  switch (role) {
    case 'ADMIN':
      return [
        'View environments and deployment history',
        'Update, rollback and delete environments',
        'Manage user roles',
        'View the audit log',
      ]
    case 'OPERATOR':
      return [
        'View environments and deployment history',
        'Update and rollback the environments you own',
      ]
    case 'USER':
    default:
      return ['View environments and deployment history']
  }
}
