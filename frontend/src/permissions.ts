import type { AuthUser } from './api/types'

const CASE_HANDLER_ROLES = new Set(['ADMIN', 'RISK_OPERATOR'])
const CONFIG_MANAGER_ROLES = new Set(['ADMIN', 'RISK_OPERATOR'])

function roleOf(user: Pick<AuthUser, 'roleCode'> | null | undefined) {
  return user?.roleCode.trim().toUpperCase() ?? ''
}

export function canHandleCases(user: Pick<AuthUser, 'roleCode'> | null | undefined) {
  return CASE_HANDLER_ROLES.has(roleOf(user))
}

export function canManageRiskConfig(user: Pick<AuthUser, 'roleCode'> | null | undefined) {
  return CONFIG_MANAGER_ROLES.has(roleOf(user))
}
