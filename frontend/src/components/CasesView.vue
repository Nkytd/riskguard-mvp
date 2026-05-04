<template>
  <section class="rules-workspace cases-workspace">
    <section class="panel rules-list-panel">
      <div class="panel-header">
        <h2>Case Queue</h2>
        <span>{{ page.total }} total</span>
      </div>

      <form class="filter-bar case-filter-bar" @submit.prevent="search">
        <input
          v-model.trim="filters.userId"
          class="text-input compact-input"
          placeholder="User ID"
          @keydown.enter.prevent="search"
        />
        <select v-model="filters.status" class="text-input compact-input">
          <option value="">All Status</option>
          <option v-for="item in statuses" :key="item" :value="item">{{ item }}</option>
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
      <div v-if="loading && page.records.length === 0" class="skeleton-panel compact-skeleton" aria-label="Loading cases">
        <span v-for="item in 8" :key="item" />
      </div>

      <div v-else class="table-wrap rules-table-wrap">
        <table class="data-table selectable-table">
          <thead>
            <tr>
              <th scope="col">Case No</th>
              <th scope="col">User</th>
              <th scope="col">Risk</th>
              <th scope="col">Status</th>
              <th class="numeric" scope="col">Assignee</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in page.records"
              :key="item.caseNo"
              :class="{ selected: item.caseNo === selected?.caseNo }"
              @click="selectCase(item)"
            >
              <td>
                <span class="mono-cell">{{ item.caseNo }}</span>
                <small>{{ formatDateTime(item.createdAt) }}</small>
              </td>
              <td class="mono-cell">{{ item.userId }}</td>
              <td>
                <span :class="['risk-pill', item.riskLevel.toLowerCase()]">{{ item.riskLevel }}</span>
                <small>{{ item.riskScore }} score</small>
              </td>
              <td><span :class="['case-status-pill', item.status.toLowerCase()]">{{ item.status }}</span></td>
              <td class="numeric">{{ item.assigneeId ?? '-' }}</td>
            </tr>
            <tr v-if="page.records.length === 0">
              <td class="empty-table" colspan="5">No cases found</td>
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

    <section class="panel rule-editor-panel">
      <div class="panel-header">
        <h2>Case Detail</h2>
        <span>{{ selected ? `${selected.status} / ${selected.riskLevel}` : 'No selection' }}</span>
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
            <span>Status</span>
            <strong>{{ selected.status }}</strong>
          </article>
          <article>
            <span>Assignee</span>
            <strong>{{ selected.assigneeId ?? '-' }}</strong>
          </article>
        </section>

        <dl class="detail-list decision-detail-list">
          <div>
            <dt>Case No</dt>
            <dd class="mono-cell">{{ selected.caseNo }}</dd>
          </div>
          <div>
            <dt>Decision No</dt>
            <dd class="mono-cell">{{ selected.decisionNo }}</dd>
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
            <dt>Created</dt>
            <dd>{{ formatDateTime(selected.createdAt) }}</dd>
          </div>
          <div>
            <dt>Updated</dt>
            <dd>{{ formatDateTime(selected.updatedAt) }}</dd>
          </div>
        </dl>

        <section class="version-section">
          <div class="panel-header subheader">
            <h2>Audit</h2>
            <span>{{ selected.auditResult || 'No result' }}</span>
          </div>
          <p class="reason-box">{{ selected.auditOpinion || selected.aiSummary || 'No audit opinion' }}</p>
        </section>

        <section class="version-section">
          <div class="panel-header subheader">
            <h2>Action</h2>
            <span>Operator is current user</span>
          </div>
          <label class="form-field">
            <span>Remark / Opinion</span>
            <textarea v-model.trim="actionText" class="text-input textarea-input" rows="3" />
          </label>
          <div class="editor-actions case-actions">
            <button class="button" type="button" :disabled="working || selected.status !== 'PENDING'" @click="performAction('claim')">
              <User class="button-icon" />
              <span>Claim</span>
            </button>
            <button class="button" type="button" :disabled="working || isFinished" @click="performAction('approve')">
              <Check class="button-icon" />
              <span>Approve</span>
            </button>
            <button class="button danger-button" type="button" :disabled="working || isFinished" @click="performAction('reject')">
              <Close class="button-icon" />
              <span>Reject</span>
            </button>
            <button class="button" type="button" :disabled="working || isFinished" @click="performAction('falsePositive')">
              <Warning class="button-icon" />
              <span>False Positive</span>
            </button>
            <button class="button" type="button" :disabled="working || selected.status === 'CLOSED'" @click="performAction('close')">
              <FolderChecked class="button-icon" />
              <span>Close</span>
            </button>
          </div>
        </section>

        <section class="version-section">
          <div class="panel-header subheader">
            <h2>Operations</h2>
            <span>{{ operations.length }} records</span>
          </div>
          <div class="table-wrap case-operation-table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th scope="col">Time</th>
                  <th scope="col">Operation</th>
                  <th scope="col">Status</th>
                  <th scope="col">Remark</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="operation in operations" :key="`${operation.operation}-${operation.createdAt}`">
                  <td>{{ formatDateTime(operation.createdAt) }}</td>
                  <td>
                    {{ operation.operation }}
                    <small>operator {{ operation.operatorId ?? '-' }}</small>
                  </td>
                  <td>{{ operation.beforeStatus || '-' }} -> {{ operation.afterStatus }}</td>
                  <td>{{ operation.remark || '-' }}</td>
                </tr>
                <tr v-if="operations.length === 0">
                  <td class="empty-table compact-empty" colspan="4">No operations</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>

      <p v-else class="empty-copy">Select a case to inspect its status and operation history.</p>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, Close, FolderChecked, RefreshRight, Search, User, Warning } from '@element-plus/icons-vue'
import {
  approveCase,
  claimCase,
  closeCase,
  loadCase,
  loadCaseOperations,
  loadCases,
  markCaseFalsePositive,
  rejectCase,
} from '../api/client'
import type { CaseOperation, CaseStatus, RiskCase } from '../api/types'

type CaseAction = 'claim' | 'approve' | 'reject' | 'falsePositive' | 'close'

const statuses: CaseStatus[] = ['PENDING', 'PROCESSING', 'APPROVED', 'REJECTED', 'CLOSED']

const filters = reactive({
  userId: '',
  status: '' as CaseStatus | '',
})

const page = reactive({
  records: [] as RiskCase[],
  total: 0,
  pageNo: 1,
  pageSize: 10,
})

const selected = ref<RiskCase | null>(null)
const operations = ref<CaseOperation[]>([])
const actionText = ref('')
const loading = ref(false)
const working = ref(false)
const error = ref('')
const feedback = reactive({
  type: 'success' as 'success' | 'error',
  message: '',
})

const totalPages = computed(() => Math.max(1, Math.ceil(page.total / page.pageSize)))
const isFinished = computed(() => selected.value ? ['APPROVED', 'REJECTED', 'CLOSED'].includes(selected.value.status) : true)

async function load(pageNo = page.pageNo, keepSelection = true) {
  error.value = ''
  loading.value = true
  try {
    const result = await loadCases({
      pageNo,
      pageSize: page.pageSize,
      status: filters.status,
      userId: filters.userId,
    })
    page.records = result.records
    page.total = result.total
    page.pageNo = result.pageNo
    page.pageSize = result.pageSize

    if (!keepSelection || (selected.value && !page.records.some((item) => item.caseNo === selected.value?.caseNo))) {
      if (page.records.length > 0) {
        await selectCase(page.records[0])
      } else {
        selected.value = null
        operations.value = []
      }
    } else if (!selected.value && page.records.length > 0) {
      await selectCase(page.records[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load cases'
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

async function selectCase(item: RiskCase) {
  feedback.message = ''
  selected.value = item
  actionText.value = ''
  try {
    const [detail, rows] = await Promise.all([
      loadCase(item.caseNo),
      loadCaseOperations(item.caseNo),
    ])
    selected.value = detail
    operations.value = rows
  } catch (err) {
    operations.value = []
    setFeedback('error', err instanceof Error ? err.message : 'Failed to load case detail')
  }
}

async function performAction(action: CaseAction) {
  if (!selected.value) {
    return
  }
  const message = actionMessages[action]
  if (!window.confirm(`${message} case ${selected.value.caseNo}?`)) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    const payload = action === 'claim'
      ? { remark: actionText.value }
      : { auditOpinion: actionText.value, remark: actionText.value }
    const caseNo = selected.value.caseNo
    const updated = await actionHandlers[action](caseNo, payload)
    selected.value = updated
    await Promise.all([load(page.pageNo, true), loadOperations(caseNo)])
    actionText.value = ''
    setFeedback('success', `${message} completed`)
  } catch (err) {
    setFeedback('error', err instanceof Error ? err.message : `${message} failed`)
  } finally {
    working.value = false
  }
}

async function loadOperations(caseNo: string) {
  operations.value = await loadCaseOperations(caseNo)
}

const actionMessages: Record<CaseAction, string> = {
  claim: 'Claim',
  approve: 'Approve',
  reject: 'Reject',
  falsePositive: 'Mark false positive',
  close: 'Close',
}

const actionHandlers = {
  claim: claimCase,
  approve: approveCase,
  reject: rejectCase,
  falsePositive: markCaseFalsePositive,
  close: closeCase,
}

function setFeedback(type: 'success' | 'error', message: string) {
  feedback.type = type
  feedback.message = message
}

function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  void load(1, false)
})
</script>
