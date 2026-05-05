export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId: string
  timestamp: string
}

export interface PageResponse<T> {
  records: T[]
  total: number
  pageNo: number
  pageSize: number
}

export interface AuthUser {
  id: number
  username: string
  realName: string
  roleCode: string
}

export interface LoginResponse {
  tokenType: string
  accessToken: string
  expiresAt: string
  user: AuthUser
}

export interface DashboardOverview {
  startTime: string
  endTime: string
  todayDecisionCount: number
  passCount: number
  verifyCount: number
  reviewCount: number
  rejectCount: number
  averageCostMs: number
  highRiskCount: number
  criticalRiskCount: number
  highAndCriticalRiskCount: number
}

export interface DashboardDistributionItem {
  name: string
  count: number
  ratio: number
}

export interface RuleHitRankItem {
  ruleCode: string
  ruleName: string
  hitCount: number
  totalScoreDelta: number
  lastHitAt: string
}

export interface RiskTrendItem {
  timeBucket: string
  decisionCount: number
  highRiskCount: number
  criticalRiskCount: number
  reviewCount: number
  rejectCount: number
}

export interface CaseStatistics {
  totalCount: number
  pendingCount: number
  processingCount: number
  approvedCount: number
  rejectedCount: number
  closedCount: number
  approvalRate: number
  rejectionRate: number
}

export interface DashboardData {
  overview: DashboardOverview
  decisionDistribution: DashboardDistributionItem[]
  ruleHitRank: RuleHitRankItem[]
  riskTrend: RiskTrendItem[]
  caseStatistics: CaseStatistics
}

export type EventType = 'LOGIN' | 'PAYMENT'
export type RuleAction = 'SCORE' | 'VERIFY' | 'REVIEW' | 'REJECT'
export type PublishStatus = 'DRAFT' | 'ENABLED' | 'DISABLED'
export type ListType = 'BLACK' | 'WHITE'
export type ListEffectType = 'REJECT' | 'SCORE_UP' | 'SCORE_DOWN'
export type ObjectType = 'USER' | 'DEVICE' | 'IP' | 'PHONE' | 'MERCHANT'
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type RiskDecisionOutcome = 'PASS' | 'VERIFY' | 'REVIEW' | 'REJECT'
export type CaseStatus = 'PENDING' | 'PROCESSING' | 'APPROVED' | 'REJECTED' | 'CLOSED'
export type AuditResult = 'APPROVE' | 'REJECT' | 'FALSE_POSITIVE'

export interface RiskRule {
  id: number
  ruleCode: string
  ruleName: string
  eventType: EventType
  expression: string
  score: number
  action: RuleAction
  priority: number
  status: PublishStatus
  version: number
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface RiskRulePayload {
  ruleCode?: string
  ruleName: string
  eventType: EventType
  expression: string
  score: number
  action: RuleAction
  priority: number
  description: string
}

export interface RuleVersion {
  id: number
  ruleId: number
  version: number
  expression: string
  score: number
  action: RuleAction
  priority: number
  status: PublishStatus
  publishNote: string | null
  createdAt: string
}

export interface ExpressionValidateResult {
  valid: boolean
  message: string
}

export interface RiskStrategy {
  id: number
  strategyCode: string
  strategyName: string
  eventType: EventType
  status: PublishStatus
  version: number
  grayRatio: number
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface RiskStrategyPayload {
  strategyCode?: string
  strategyName: string
  eventType: EventType
  grayRatio: number
  description: string
}

export interface StrategyRule {
  id: number
  strategyId: number
  ruleId: number
  ruleCode: string
  ruleName: string
  eventType: EventType
  ruleVersion: number
  executeOrder: number
  enabled: boolean
  createdAt: string
}

export interface StrategyVersion {
  id: number
  strategyId: number
  version: number
  ruleSnapshot: string
  grayRatio: number
  status: PublishStatus
  publishNote: string | null
  createdAt: string
}

export interface RiskList {
  id: number
  listType: ListType
  objectType: ObjectType
  objectValue: string
  riskLevel: RiskLevel | null
  effectType: ListEffectType
  scoreDelta: number
  reason: string | null
  startTime: string | null
  endTime: string | null
  status: PublishStatus
  createdAt: string
  updatedAt: string
}

export interface RiskListPayload {
  listType?: ListType
  objectType?: ObjectType
  objectValue?: string
  riskLevel?: RiskLevel | ''
  effectType: ListEffectType
  scoreDelta: number
  reason: string
  startTime?: string
  endTime?: string
}

export interface DecisionHitRule {
  ruleId: number
  ruleCode: string
  ruleName: string
  ruleVersion: number
  scoreDelta: number
  action: RuleAction
  hitDetail: string
}

export interface DecisionLog {
  decisionNo: string
  eventNo: string
  requestNo: string
  eventType: EventType
  userId: string
  strategyId: number
  strategyVersion: number
  riskScore: number
  riskLevel: RiskLevel
  decision: RiskDecisionOutcome
  reason: string
  costMs: number
  traceId: string
  createdAt: string
  hitRules: DecisionHitRule[]
}

export interface RiskCase {
  caseNo: string
  decisionNo: string
  eventNo: string
  userId: string
  riskScore: number
  riskLevel: RiskLevel
  status: CaseStatus
  assigneeId: number | null
  aiSummary: string | null
  auditResult: AuditResult | null
  auditOpinion: string | null
  createdAt: string
  updatedAt: string
}

export interface CaseOperation {
  caseNo: string
  operatorId: number | null
  operation: string
  beforeStatus: CaseStatus | null
  afterStatus: CaseStatus
  remark: string | null
  createdAt: string
}

export interface CaseActionPayload {
  operatorId?: number
  assigneeId?: number
  auditOpinion?: string
  remark?: string
}
