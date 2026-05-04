<template>
  <section class="management-workspace decisions-workspace">
    <section class="panel management-list-panel">
      <div class="panel-header">
        <h2>Decision Logs</h2>
        <span>{{ page.total }} total</span>
      </div>

      <form class="filter-bar decision-filter-bar" @submit.prevent="search">
        <input
          v-model.trim="filters.userId"
          class="text-input compact-input"
          placeholder="User ID"
          @keydown.enter.prevent="search"
        />
        <select v-model="filters.eventType" class="text-input compact-input">
          <option value="">All Events</option>
          <option v-for="item in eventTypes" :key="item" :value="item">{{ item }}</option>
        </select>
        <select v-model="filters.decision" class="text-input compact-input">
          <option value="">All Decisions</option>
          <option v-for="item in decisions" :key="item" :value="item">{{ item }}</option>
        </select>
        <button class="button" type="button" :disabled="loading" @click="search">
          <Search class="button-icon" />
          <span>Search</span>
        </button>
        <button class="button" type="button" :disabled="loading" @click="refresh">
          <RefreshRight class="button-icon" />
          <span>Refresh</span>
        </button>
      </form>

      <div v-if="error" class="alert alert-error" role="alert">{{ error }}</div>
      <div v-if="loading && page.records.length === 0" class="skeleton-panel compact-skeleton" aria-label="Loading decisions">
        <span v-for="item in 8" :key="item" />
      </div>

      <div v-else class="table-wrap management-table-wrap">
        <table class="data-table selectable-table">
          <thead>
            <tr>
              <th scope="col">Decision No</th>
              <th scope="col">User</th>
              <th scope="col">Event</th>
              <th scope="col">Decision</th>
              <th scope="col">Risk</th>
              <th class="numeric" scope="col">Cost</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="decision in page.records"
              :key="decision.decisionNo"
              :class="{ selected: decision.decisionNo === selected?.decisionNo }"
              @click="selectDecision(decision)"
            >
              <td>
                <span class="mono-cell">{{ decision.decisionNo }}</span>
                <small>{{ formatDateTime(decision.createdAt) }}</small>
              </td>
              <td class="mono-cell">{{ decision.userId }}</td>
              <td>{{ decision.eventType }}</td>
              <td>
                <span :class="['decision-pill', decision.decision.toLowerCase()]">{{ decision.decision }}</span>
              </td>
              <td>
                <span :class="['risk-pill', decision.riskLevel.toLowerCase()]">{{ decision.riskLevel }}</span>
                <small>{{ decision.riskScore }} score</small>
              </td>
              <td class="numeric">{{ decision.costMs }} ms</td>
            </tr>
            <tr v-if="page.records.length === 0">
              <td class="empty-table" colspan="6">No decisions found</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-bar">
        <button class="button" type="button" :disabled="loading || page.pageNo <= 1" @click="goPage(page.pageNo - 1)">Prev</button>
        <span>Page {{ page.pageNo }} / {{ totalPages }}</span>
        <button class="button" type="button" :disabled="loading || page.pageNo >= totalPages" @click="goPage(page.pageNo + 1)">Next</button>
      </div>
    </section>

    <section class="panel management-detail-panel">
      <div class="panel-header">
        <h2>Decision Detail</h2>
        <span>{{ selected ? `${selected.decision} / ${selected.riskLevel}` : 'No selection' }}</span>
      </div>

      <div v-if="feedback.message" :class="['alert', feedback.type === 'error' ? 'alert-error' : 'alert-success']" role="status">
        {{ feedback.message }}
      </div>

      <template v-if="selected">
        <section class="decision-summary-grid">
          <article>
            <span>Risk Score</span>
            <strong>{{ selected.riskScore }}</strong>
          </article>
          <article>
            <span>Cost</span>
            <strong>{{ selected.costMs }} ms</strong>
          </article>
          <article>
            <span>Strategy</span>
            <strong>#{{ selected.strategyId }} / v{{ selected.strategyVersion }}</strong>
          </article>
        </section>

        <dl class="detail-list decision-detail-list">
          <div>
            <dt>Decision No</dt>
            <dd class="mono-cell">{{ selected.decisionNo }}</dd>
          </div>
          <div>
            <dt>Request No</dt>
            <dd class="mono-cell">{{ selected.requestNo }}</dd>
          </div>
          <div>
            <dt>Event No</dt>
            <dd class="mono-cell">{{ selected.eventNo }}</dd>
          </div>
          <div>
            <dt>User ID</dt>
            <dd class="mono-cell">{{ selected.userId }}</dd>
          </div>
          <div>
            <dt>Trace ID</dt>
            <dd class="mono-cell">{{ selected.traceId }}</dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd>{{ formatDateTime(selected.createdAt) }}</dd>
          </div>
        </dl>

        <section class="version-section">
          <div class="panel-header subheader">
            <h2>Reason</h2>
            <span>{{ selected.eventType }}</span>
          </div>
          <p class="reason-box">{{ selected.reason || 'No reason' }}</p>
        </section>

        <section class="version-section">
          <div class="panel-header subheader">
            <h2>Hit Rules</h2>
            <span>{{ hitRules.length }} records</span>
          </div>
          <div class="table-wrap hit-rule-table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th scope="col">Rule</th>
                  <th class="numeric" scope="col">Version</th>
                  <th class="numeric" scope="col">Score</th>
                  <th scope="col">Action</th>
                  <th scope="col">Detail</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="rule in hitRules" :key="`${rule.ruleId}-${rule.ruleVersion}`">
                  <td>
                    <span class="mono-cell">{{ rule.ruleCode }}</span>
                    <small>{{ rule.ruleName }}</small>
                  </td>
                  <td class="numeric">v{{ rule.ruleVersion }}</td>
                  <td class="numeric">{{ signedScore(rule.scoreDelta) }}</td>
                  <td>{{ rule.action }}</td>
                  <td>{{ rule.hitDetail }}</td>
                </tr>
                <tr v-if="hitRules.length === 0">
                  <td class="empty-table compact-empty" colspan="5">No hit rules</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>

      <p v-else class="empty-copy">Select a decision to inspect its trace and hit rules.</p>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { loadDecisionHitRules, loadDecisionLog, loadDecisionLogs } from '../api/client'
import type { DecisionHitRule, DecisionLog, EventType, RiskDecisionOutcome } from '../api/types'

const eventTypes: EventType[] = ['LOGIN', 'PAYMENT']
const decisions: RiskDecisionOutcome[] = ['PASS', 'VERIFY', 'REVIEW', 'REJECT']

const filters = reactive({
  userId: '',
  eventType: '' as EventType | '',
  decision: '' as RiskDecisionOutcome | '',
})

const page = reactive({
  records: [] as DecisionLog[],
  total: 0,
  pageNo: 1,
  pageSize: 10,
})

const selected = ref<DecisionLog | null>(null)
const hitRules = ref<DecisionHitRule[]>([])
const loading = ref(false)
const detailLoading = ref(false)
const error = ref('')
const feedback = reactive({
  type: 'success' as 'success' | 'error',
  message: '',
})

const totalPages = computed(() => Math.max(1, Math.ceil(page.total / page.pageSize)))

async function load(pageNo = page.pageNo, keepSelection = true) {
  error.value = ''
  loading.value = true
  try {
    const result = await loadDecisionLogs({
      pageNo,
      pageSize: page.pageSize,
      eventType: filters.eventType,
      decision: filters.decision,
      userId: filters.userId,
    })
    page.records = result.records
    page.total = result.total
    page.pageNo = result.pageNo
    page.pageSize = result.pageSize

    if (!keepSelection || (selected.value && !page.records.some((item) => item.decisionNo === selected.value?.decisionNo))) {
      if (page.records.length > 0) {
        await selectDecision(page.records[0])
      } else {
        selected.value = null
        hitRules.value = []
      }
    } else if (!selected.value && page.records.length > 0) {
      await selectDecision(page.records[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load decisions'
  } finally {
    loading.value = false
  }
}

function search() {
  void load(1, false)
}

function refresh() {
  void load(page.pageNo, true)
}

function goPage(pageNo: number) {
  void load(pageNo, false)
}

async function selectDecision(decision: DecisionLog) {
  feedback.message = ''
  detailLoading.value = true
  selected.value = decision
  try {
    const [detail, rules] = await Promise.all([
      loadDecisionLog(decision.decisionNo),
      loadDecisionHitRules(decision.decisionNo),
    ])
    selected.value = detail
    hitRules.value = rules.length > 0 ? rules : detail.hitRules
  } catch (err) {
    hitRules.value = decision.hitRules
    setFeedback('error', err instanceof Error ? err.message : 'Failed to load decision detail')
  } finally {
    detailLoading.value = false
  }
}

function setFeedback(type: 'success' | 'error', message: string) {
  feedback.type = type
  feedback.message = message
}

function signedScore(value: number) {
  return value > 0 ? `+${value}` : String(value)
}

function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  void load(1, false)
})
</script>
