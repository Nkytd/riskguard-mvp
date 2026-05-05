import type {
  ApiResponse,
  AuthUser,
  CaseStatistics,
  CaseActionPayload,
  CaseOperation,
  CaseStatus,
  DashboardData,
  DashboardDistributionItem,
  DashboardOverview,
  DecisionHitRule,
  DecisionLog,
  EventType,
  ExpressionValidateResult,
  ListType,
  ObjectType,
  LoginResponse,
  PageResponse,
  PublishStatus,
  RiskList,
  RiskListPayload,
  RiskRule,
  RiskRulePayload,
  RiskStrategy,
  RiskStrategyPayload,
  RiskTrendItem,
  StrategyRule,
  StrategyVersion,
  RuleVersion,
  RuleHitRankItem,
  RiskDecisionOutcome,
  RiskCase,
} from './types'

const TOKEN_KEY = 'riskguard.accessToken'
const USER_KEY = 'riskguard.user'

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly code: number,
  ) {
    super(message)
  }
}

export function formatApiError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    if (error.code === 401) {
      return 'Session expired. Sign in again.'
    }
    if (error.code === 403) {
      return 'You do not have permission to perform this action.'
    }
    return error.message || fallback
  }
  if (error instanceof TypeError) {
    return 'Network error. Check that the backend service is running and try again.'
  }
  if (error instanceof Error) {
    return error.message || fallback
  }
  return fallback
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function getStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

export function saveSession(token: string, user: AuthUser) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(path, {
      ...init,
      headers,
    })
  } catch (error) {
    throw new ApiError(formatApiError(error, 'Request failed'), 0)
  }

  let payload: ApiResponse<T>
  try {
    payload = (await response.json()) as ApiResponse<T>
  } catch {
    throw new ApiError(response.statusText || 'Invalid server response', response.status)
  }
  if (!response.ok || payload.code !== 0) {
    if (response.status === 401 || response.status === 403 || payload.code === 401 || payload.code === 403) {
      clearSession()
    }
    throw new ApiError(payload.message || response.statusText, payload.code || response.status)
  }
  return payload.data
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export async function loadCurrentUser(): Promise<AuthUser> {
  return request<AuthUser>('/api/v1/auth/me')
}

export async function loadDashboard(): Promise<DashboardData> {
  const [overview, decisionDistribution, ruleHitRank, riskTrend, caseStatistics] = await Promise.all([
    request<DashboardOverview>('/api/v1/dashboard/overview'),
    request<DashboardDistributionItem[]>('/api/v1/dashboard/decision-distribution'),
    request<RuleHitRankItem[]>('/api/v1/dashboard/rule-hit-rank'),
    request<RiskTrendItem[]>('/api/v1/dashboard/risk-trend'),
    request<CaseStatistics>('/api/v1/dashboard/case-statistics'),
  ])
  return {
    overview,
    decisionDistribution,
    ruleHitRank,
    riskTrend,
    caseStatistics,
  }
}

export interface RuleListParams {
  pageNo: number
  pageSize: number
  eventType?: EventType | ''
  status?: PublishStatus | ''
  keyword?: string
}

function toQuery(params: Record<string, string | number | undefined>) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      search.set(key, String(value))
    }
  }
  return search.toString()
}

export async function loadRules(params: RuleListParams): Promise<PageResponse<RiskRule>> {
  const query = toQuery({
    pageNo: params.pageNo,
    pageSize: params.pageSize,
    eventType: params.eventType,
    status: params.status,
    keyword: params.keyword?.trim(),
  })
  return request<PageResponse<RiskRule>>(`/api/v1/rules?${query}`)
}

export async function loadRule(id: number): Promise<RiskRule> {
  return request<RiskRule>(`/api/v1/rules/${id}`)
}

export async function createRule(payload: RiskRulePayload): Promise<RiskRule> {
  return request<RiskRule>('/api/v1/rules', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function updateRule(id: number, payload: RiskRulePayload): Promise<RiskRule> {
  return request<RiskRule>(`/api/v1/rules/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function enableRule(id: number): Promise<RiskRule> {
  return request<RiskRule>(`/api/v1/rules/${id}/enable`, { method: 'POST' })
}

export async function disableRule(id: number): Promise<RiskRule> {
  return request<RiskRule>(`/api/v1/rules/${id}/disable`, { method: 'POST' })
}

export async function publishRule(id: number, publishNote: string): Promise<RuleVersion> {
  return request<RuleVersion>(`/api/v1/rules/${id}/publish`, {
    method: 'POST',
    body: JSON.stringify({ publishNote }),
  })
}

export async function loadRuleVersions(id: number): Promise<RuleVersion[]> {
  return request<RuleVersion[]>(`/api/v1/rules/${id}/versions`)
}

export async function validateRuleExpression(expression: string): Promise<ExpressionValidateResult> {
  return request<ExpressionValidateResult>('/api/v1/rules/validate-expression', {
    method: 'POST',
    body: JSON.stringify({ expression }),
  })
}

export interface StrategyListParams {
  pageNo: number
  pageSize: number
  eventType?: EventType | ''
  status?: PublishStatus | ''
  keyword?: string
}

export async function loadStrategies(params: StrategyListParams): Promise<PageResponse<RiskStrategy>> {
  const query = toQuery({
    pageNo: params.pageNo,
    pageSize: params.pageSize,
    eventType: params.eventType,
    status: params.status,
    keyword: params.keyword?.trim(),
  })
  return request<PageResponse<RiskStrategy>>(`/api/v1/strategies?${query}`)
}

export async function loadStrategy(id: number): Promise<RiskStrategy> {
  return request<RiskStrategy>(`/api/v1/strategies/${id}`)
}

export async function createStrategy(payload: RiskStrategyPayload): Promise<RiskStrategy> {
  return request<RiskStrategy>('/api/v1/strategies', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function updateStrategy(id: number, payload: RiskStrategyPayload): Promise<RiskStrategy> {
  return request<RiskStrategy>(`/api/v1/strategies/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function enableStrategy(id: number): Promise<RiskStrategy> {
  return request<RiskStrategy>(`/api/v1/strategies/${id}/enable`, { method: 'POST' })
}

export async function disableStrategy(id: number): Promise<RiskStrategy> {
  return request<RiskStrategy>(`/api/v1/strategies/${id}/disable`, { method: 'POST' })
}

export async function loadStrategyRules(id: number): Promise<StrategyRule[]> {
  return request<StrategyRule[]>(`/api/v1/strategies/${id}/rules`)
}

export async function bindStrategyRule(
  strategyId: number,
  payload: { ruleId: number; ruleVersion?: number; executeOrder?: number; enabled?: boolean },
): Promise<StrategyRule> {
  return request<StrategyRule>(`/api/v1/strategies/${strategyId}/rules`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function removeStrategyRule(strategyId: number, ruleId: number): Promise<void> {
  return request<void>(`/api/v1/strategies/${strategyId}/rules/${ruleId}`, { method: 'DELETE' })
}

export async function updateStrategyRuleOrder(strategyId: number, items: Array<{ ruleId: number; executeOrder: number }>): Promise<StrategyRule[]> {
  return request<StrategyRule[]>(`/api/v1/strategies/${strategyId}/rules/order`, {
    method: 'PUT',
    body: JSON.stringify({ items }),
  })
}

export async function publishStrategy(id: number, publishNote: string): Promise<StrategyVersion> {
  return request<StrategyVersion>(`/api/v1/strategies/${id}/publish`, {
    method: 'POST',
    body: JSON.stringify({ publishNote }),
  })
}

export async function rollbackStrategy(id: number, version: number): Promise<StrategyVersion> {
  return request<StrategyVersion>(`/api/v1/strategies/${id}/rollback`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  })
}

export async function loadStrategyVersions(id: number): Promise<StrategyVersion[]> {
  return request<StrategyVersion[]>(`/api/v1/strategies/${id}/versions`)
}

export interface RiskListParams {
  pageNo: number
  pageSize: number
  listType?: ListType | ''
  objectType?: ObjectType | ''
  status?: PublishStatus | ''
  keyword?: string
}

export async function loadRiskLists(params: RiskListParams): Promise<PageResponse<RiskList>> {
  const query = toQuery({
    pageNo: params.pageNo,
    pageSize: params.pageSize,
    listType: params.listType,
    objectType: params.objectType,
    status: params.status,
    keyword: params.keyword?.trim(),
  })
  return request<PageResponse<RiskList>>(`/api/v1/risk-lists?${query}`)
}

export async function loadRiskList(id: number): Promise<RiskList> {
  return request<RiskList>(`/api/v1/risk-lists/${id}`)
}

export async function createRiskList(payload: RiskListPayload): Promise<RiskList> {
  return request<RiskList>('/api/v1/risk-lists', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function updateRiskList(id: number, payload: RiskListPayload): Promise<RiskList> {
  return request<RiskList>(`/api/v1/risk-lists/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function enableRiskList(id: number): Promise<RiskList> {
  return request<RiskList>(`/api/v1/risk-lists/${id}/enable`, { method: 'POST' })
}

export async function disableRiskList(id: number): Promise<RiskList> {
  return request<RiskList>(`/api/v1/risk-lists/${id}/disable`, { method: 'POST' })
}

export async function deleteRiskList(id: number): Promise<void> {
  return request<void>(`/api/v1/risk-lists/${id}`, { method: 'DELETE' })
}

export interface DecisionListParams {
  pageNo: number
  pageSize: number
  eventType?: EventType | ''
  decision?: RiskDecisionOutcome | ''
  userId?: string
}

export async function loadDecisionLogs(params: DecisionListParams): Promise<PageResponse<DecisionLog>> {
  const query = toQuery({
    pageNo: params.pageNo,
    pageSize: params.pageSize,
    eventType: params.eventType,
    decision: params.decision,
    userId: params.userId?.trim(),
  })
  return request<PageResponse<DecisionLog>>(`/api/v1/risk/decisions?${query}`)
}

export async function loadDecisionLog(decisionNo: string): Promise<DecisionLog> {
  return request<DecisionLog>(`/api/v1/risk/decisions/${encodeURIComponent(decisionNo)}`)
}

export async function loadDecisionHitRules(decisionNo: string): Promise<DecisionHitRule[]> {
  return request<DecisionHitRule[]>(`/api/v1/risk/decisions/${encodeURIComponent(decisionNo)}/hit-rules`)
}

export interface CaseListParams {
  pageNo: number
  pageSize: number
  status?: CaseStatus | ''
  userId?: string
}

export async function loadCases(params: CaseListParams): Promise<PageResponse<RiskCase>> {
  const query = toQuery({
    pageNo: params.pageNo,
    pageSize: params.pageSize,
    status: params.status,
    userId: params.userId?.trim(),
  })
  return request<PageResponse<RiskCase>>(`/api/v1/cases?${query}`)
}

export async function loadCase(caseNo: string): Promise<RiskCase> {
  return request<RiskCase>(`/api/v1/cases/${encodeURIComponent(caseNo)}`)
}

export async function loadCaseOperations(caseNo: string): Promise<CaseOperation[]> {
  return request<CaseOperation[]>(`/api/v1/cases/${encodeURIComponent(caseNo)}/operations`)
}

function submitCaseAction(caseNo: string, action: string, payload: CaseActionPayload): Promise<RiskCase> {
  return request<RiskCase>(`/api/v1/cases/${encodeURIComponent(caseNo)}/${action}`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function claimCase(caseNo: string, payload: CaseActionPayload): Promise<RiskCase> {
  return submitCaseAction(caseNo, 'claim', payload)
}

export async function approveCase(caseNo: string, payload: CaseActionPayload): Promise<RiskCase> {
  return submitCaseAction(caseNo, 'approve', payload)
}

export async function rejectCase(caseNo: string, payload: CaseActionPayload): Promise<RiskCase> {
  return submitCaseAction(caseNo, 'reject', payload)
}

export async function closeCase(caseNo: string, payload: CaseActionPayload): Promise<RiskCase> {
  return submitCaseAction(caseNo, 'close', payload)
}

export async function markCaseFalsePositive(caseNo: string, payload: CaseActionPayload): Promise<RiskCase> {
  return submitCaseAction(caseNo, 'mark-false-positive', payload)
}
