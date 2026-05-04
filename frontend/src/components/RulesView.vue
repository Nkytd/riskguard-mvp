<template>
  <section class="rules-workspace">
    <section class="panel rules-list-panel">
      <div class="panel-header">
        <h2>Rule Catalog</h2>
        <span>{{ page.total }} total</span>
      </div>

      <form class="filter-bar" @submit.prevent="search">
        <input
          v-model.trim="filters.keyword"
          class="text-input compact-input"
          placeholder="Search code or name"
          @keydown.enter.prevent="search"
        />
        <select v-model="filters.eventType" class="text-input compact-input">
          <option value="">All Events</option>
          <option v-for="item in eventTypes" :key="item" :value="item">{{ item }}</option>
        </select>
        <select v-model="filters.status" class="text-input compact-input">
          <option value="">All Status</option>
          <option v-for="item in statuses" :key="item" :value="item">{{ item }}</option>
        </select>
        <button class="button" type="button" :disabled="loading" @click="search">
          <Search class="button-icon" />
          <span>Search</span>
        </button>
        <button class="button primary compact-primary" type="button" @click="startCreate">
          <Plus class="button-icon" />
          <span>New</span>
        </button>
      </form>

      <div v-if="error" class="alert alert-error" role="alert">{{ error }}</div>
      <div v-if="loading && page.records.length === 0" class="skeleton-panel compact-skeleton" aria-label="Loading rules">
        <span v-for="item in 8" :key="item" />
      </div>

      <div v-else class="table-wrap rules-table-wrap">
        <table class="data-table selectable-table">
          <thead>
            <tr>
              <th scope="col">Code</th>
              <th scope="col">Name</th>
              <th scope="col">Event</th>
              <th scope="col">Status</th>
              <th class="numeric" scope="col">Score</th>
              <th class="numeric" scope="col">Version</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="rule in page.records"
              :key="rule.id"
              :class="{ selected: rule.id === form.id }"
              @click="selectRule(rule)"
            >
              <td class="mono-cell">{{ rule.ruleCode }}</td>
              <td>{{ rule.ruleName }}</td>
              <td>{{ rule.eventType }}</td>
              <td><span :class="['status-pill', rule.status.toLowerCase()]">{{ rule.status }}</span></td>
              <td class="numeric">{{ rule.score }}</td>
              <td class="numeric">v{{ rule.version }}</td>
            </tr>
            <tr v-if="page.records.length === 0">
              <td class="empty-table" colspan="6">No rules found</td>
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
        <h2>{{ form.id ? 'Rule Detail' : 'New Rule' }}</h2>
        <span>{{ form.id ? `${form.status} / v${form.version}` : 'Draft' }}</span>
      </div>

      <div v-if="feedback.message" :class="['alert', feedback.type === 'error' ? 'alert-error' : 'alert-success']" role="status">
        {{ feedback.message }}
      </div>

      <form class="rule-form" @submit.prevent="save">
        <label class="form-field">
          <span>Rule Code</span>
          <input v-model.trim="form.ruleCode" class="text-input" :disabled="Boolean(form.id)" />
        </label>
        <label class="form-field">
          <span>Rule Name</span>
          <input v-model.trim="form.ruleName" class="text-input" />
        </label>
        <div class="form-grid">
          <label class="form-field">
            <span>Event</span>
            <select v-model="form.eventType" class="text-input">
              <option v-for="item in eventTypes" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label class="form-field">
            <span>Action</span>
            <select v-model="form.action" class="text-input">
              <option v-for="item in actions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
        </div>
        <div class="form-grid">
          <label class="form-field">
            <span>Score</span>
            <input v-model.number="form.score" class="text-input" min="0" type="number" />
          </label>
          <label class="form-field">
            <span>Priority</span>
            <input v-model.number="form.priority" class="text-input" min="0" type="number" />
          </label>
        </div>
        <label class="form-field">
          <span>Expression</span>
          <textarea v-model.trim="form.expression" class="text-input textarea-input" rows="5" />
        </label>
        <label class="form-field">
          <span>Description</span>
          <textarea v-model.trim="form.description" class="text-input textarea-input" rows="3" />
        </label>
        <label class="form-field">
          <span>Publish Note</span>
          <input v-model.trim="publishNote" class="text-input" placeholder="Optional note for publish" />
        </label>

        <div class="editor-actions">
          <button class="button" type="button" :disabled="working || !form.expression" @click="validateExpression">
            <Check class="button-icon" />
            <span>Validate</span>
          </button>
          <button class="button primary" type="submit" :disabled="working || !canSave">
            <EditPen class="button-icon" />
            <span>{{ form.id ? 'Save' : 'Create' }}</span>
          </button>
          <button class="button" type="button" :disabled="working || !form.id" @click="toggleStatus">
            <Switch class="button-icon" />
            <span>{{ form.status === 'ENABLED' ? 'Disable' : 'Enable' }}</span>
          </button>
          <button class="button" type="button" :disabled="working || !form.id" @click="publish">
            <Upload class="button-icon" />
            <span>Publish</span>
          </button>
        </div>
      </form>

      <section class="version-section">
        <div class="panel-header subheader">
          <h2>Versions</h2>
          <span>{{ versions.length }} records</span>
        </div>
        <div class="version-list">
          <article v-for="version in versions" :key="version.id" class="version-item">
            <strong>v{{ version.version }} / {{ version.status }}</strong>
            <span>{{ formatDateTime(version.createdAt) }}</span>
            <small>{{ version.publishNote || 'No note' }}</small>
          </article>
          <p v-if="versions.length === 0" class="empty-copy">No published versions</p>
        </div>
      </section>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, EditPen, Plus, Search, Switch, Upload } from '@element-plus/icons-vue'
import {
  createRule,
  disableRule,
  enableRule,
  loadRule,
  loadRuleVersions,
  loadRules,
  publishRule,
  updateRule,
  validateRuleExpression,
} from '../api/client'
import type { EventType, PublishStatus, RiskRule, RuleAction, RuleVersion } from '../api/types'

const eventTypes: EventType[] = ['LOGIN', 'PAYMENT']
const actions: RuleAction[] = ['SCORE', 'VERIFY', 'REVIEW', 'REJECT']
const statuses: PublishStatus[] = ['DRAFT', 'ENABLED', 'DISABLED']

const filters = reactive({
  keyword: '',
  eventType: '' as EventType | '',
  status: '' as PublishStatus | '',
})

const page = reactive({
  records: [] as RiskRule[],
  total: 0,
  pageNo: 1,
  pageSize: 10,
})

const form = reactive({
  id: null as number | null,
  ruleCode: '',
  ruleName: '',
  eventType: 'PAYMENT' as EventType,
  expression: '',
  score: 0,
  action: 'SCORE' as RuleAction,
  priority: 100,
  status: 'DRAFT' as PublishStatus,
  version: 0,
  description: '',
})

const versions = ref<RuleVersion[]>([])
const publishNote = ref('')
const loading = ref(false)
const working = ref(false)
const error = ref('')
const feedback = reactive({
  type: 'success' as 'success' | 'error',
  message: '',
})

const totalPages = computed(() => Math.max(1, Math.ceil(page.total / page.pageSize)))
const canSave = computed(() => {
  return Boolean(form.ruleCode && form.ruleName && form.eventType && form.action && form.expression && Number.isFinite(form.score))
})

async function load(pageNo = page.pageNo, keepSelection = true) {
  error.value = ''
  loading.value = true
  try {
    const result = await loadRules({
      pageNo,
      pageSize: page.pageSize,
      eventType: filters.eventType,
      status: filters.status,
      keyword: filters.keyword,
    })
    page.records = result.records
    page.total = result.total
    page.pageNo = result.pageNo
    page.pageSize = result.pageSize

    if (!keepSelection || (form.id && !page.records.some((rule) => rule.id === form.id))) {
      if (page.records.length > 0) {
        await selectRule(page.records[0])
      } else {
        startCreate()
      }
    } else if (!form.id && page.records.length > 0) {
      await selectRule(page.records[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load rules'
  } finally {
    loading.value = false
  }
}

function search() {
  void load(1, false)
}

function goPage(pageNo: number) {
  void load(pageNo, false)
}

async function selectRule(rule: RiskRule) {
  applyRule(rule)
  feedback.message = ''
  publishNote.value = ''
  try {
    versions.value = await loadRuleVersions(rule.id)
  } catch {
    versions.value = []
  }
}

function startCreate() {
  Object.assign(form, {
    id: null,
    ruleCode: '',
    ruleName: '',
    eventType: 'PAYMENT',
    expression: '',
    score: 0,
    action: 'SCORE',
    priority: 100,
    status: 'DRAFT',
    version: 0,
    description: '',
  })
  versions.value = []
  publishNote.value = ''
  feedback.message = ''
}

async function save() {
  if (!canSave.value) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    const payload = {
      ruleName: form.ruleName,
      eventType: form.eventType,
      expression: form.expression,
      score: Number(form.score),
      action: form.action,
      priority: Number(form.priority),
      description: form.description,
    }
    const saved = form.id
      ? await updateRule(form.id, payload)
      : await createRule({ ...payload, ruleCode: form.ruleCode })
    applyRule(saved)
    await load(page.pageNo, true)
    await selectRule(saved)
    setFeedback('success', form.id ? 'Rule saved' : 'Rule created')
  } catch (err) {
    setFeedback('error', err instanceof Error ? err.message : 'Save failed')
  } finally {
    working.value = false
  }
}

async function validateExpression() {
  feedback.message = ''
  working.value = true
  try {
    const result = await validateRuleExpression(form.expression)
    setFeedback(result.valid ? 'success' : 'error', result.message)
  } catch (err) {
    setFeedback('error', err instanceof Error ? err.message : 'Validation failed')
  } finally {
    working.value = false
  }
}

async function toggleStatus() {
  if (!form.id) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    const updated = form.status === 'ENABLED' ? await disableRule(form.id) : await enableRule(form.id)
    applyRule(updated)
    await load(page.pageNo, true)
    setFeedback('success', updated.status === 'ENABLED' ? 'Rule enabled' : 'Rule disabled')
  } catch (err) {
    setFeedback('error', err instanceof Error ? err.message : 'Status update failed')
  } finally {
    working.value = false
  }
}

async function publish() {
  if (!form.id) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    await publishRule(form.id, publishNote.value)
    const refreshed = await loadRule(form.id)
    applyRule(refreshed)
    versions.value = await loadRuleVersions(form.id)
    await load(page.pageNo, true)
    setFeedback('success', 'Rule published')
  } catch (err) {
    setFeedback('error', err instanceof Error ? err.message : 'Publish failed')
  } finally {
    working.value = false
  }
}

function applyRule(rule: RiskRule) {
  Object.assign(form, {
    id: rule.id,
    ruleCode: rule.ruleCode,
    ruleName: rule.ruleName,
    eventType: rule.eventType,
    expression: rule.expression,
    score: rule.score,
    action: rule.action,
    priority: rule.priority,
    status: rule.status,
    version: rule.version,
    description: rule.description ?? '',
  })
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
