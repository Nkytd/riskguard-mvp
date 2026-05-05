import { describe, expect, it } from 'vitest'
import { canHandleCases, canManageRiskConfig } from './permissions'

describe('role permissions', () => {
  it('allows admins and risk operators to handle cases', () => {
    expect(canHandleCases({ roleCode: 'ADMIN' })).toBe(true)
    expect(canHandleCases({ roleCode: 'RISK_OPERATOR' })).toBe(true)
  })

  it('keeps auditors read-only for case handling and risk configuration', () => {
    expect(canHandleCases({ roleCode: 'AUDITOR' })).toBe(false)
    expect(canManageRiskConfig({ roleCode: 'AUDITOR' })).toBe(false)
  })

  it('normalizes stored role codes defensively', () => {
    expect(canHandleCases({ roleCode: ' risk_operator ' })).toBe(true)
    expect(canManageRiskConfig(null)).toBe(false)
  })
})
