<template>
  <section class="management-workspace">
    <section class="panel management-list-panel">
      <div class="panel-header">
        <h2>Strategy Catalog</h2>
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

      <StateBlock
        v-if="error && page.records.length === 0"
        action-label="Retry"
        :description="error"
        title="Strategies unavailable"
        variant="error"
        @action="load(1, false)"
      />
      <div v-else-if="error" class="alert alert-error" role="alert">{{ error }}</div>
      <div v-else-if="loading && page.records.length === 0" class="skeleton-panel compact-skeleton" aria-label="Loading strategies">
        <span v-for="item in 8" :key="item" />
      </div>

      <div v-else class="table-wrap management-table-wrap">
        <table class="data-table selectable-table">
          <thead>
            <tr>
              <th scope="col">Code</th>
              <th scope="col">Name</th>
              <th scope="col">Event</th>
              <th scope="col">Status</th>
              <th class="numeric" scope="col">Gray</th>
              <th class="numeric" scope="col">Version</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="strategy in page.records"
              :key="strategy.id"
              :class="{ selected: strategy.id === form.id }"
              @click="selectStrategy(strategy)"
            >
              <td class="mono-cell">{{ strategy.strategyCode }}</td>
              <td>{{ strategy.strategyName }}</td>
              <td>{{ strategy.eventType }}</td>
              <td><span :class="['status-pill', strategy.status.toLowerCase()]">{{ strategy.status }}</span></td>
              <td class="numeric">{{ strategy.grayRatio }}%</td>
              <td class="numeric">v{{ strategy.version }}</td>
            </tr>
            <tr v-if="page.records.length === 0">
              <td class="empty-table" colspan="6">
                <StateBlock
                  compact
                  description="Create a strategy or adjust filters to bring records back into view."
                  title="No strategies found"
                />
              </td>
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
        <h2>{{ form.id ? 'Strategy Detail' : 'New Strategy' }}</h2>
        <span>{{ form.id ? `${form.status} / v${form.version}` : 'Draft' }}</span>
      </div>

      <div v-if="feedback.message" :class="['alert', feedback.type === 'error' ? 'alert-error' : 'alert-success']" role="status">
        {{ feedback.message }}
      </div>

      <form class="management-form" @submit.prevent="save">
        <label class="form-field">
          <span>Strategy Code</span>
          <input v-model.trim="form.strategyCode" class="text-input" :disabled="Boolean(form.id)" />
        </label>
        <label class="form-field">
          <span>Strategy Name</span>
          <input v-model.trim="form.strategyName" class="text-input" />
        </label>
        <div class="form-grid">
          <label class="form-field">
            <span>Event</span>
            <select v-model="form.eventType" class="text-input" @change="loadAvailableRules">
              <option v-for="item in eventTypes" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label class="form-field">
            <span>Gray Ratio</span>
            <input v-model.number="form.grayRatio" class="text-input" max="100" min="0" type="number" />
          </label>
        </div>
        <label class="form-field">
          <span>Description</span>
          <textarea v-model.trim="form.description" class="text-input textarea-input" rows="3" />
        </label>
        <label class="form-field">
          <span>Publish Note</span>
          <input v-model.trim="publishNote" class="text-input" placeholder="Optional note for publish" />
        </label>

        <div class="editor-actions">
          <button class="button primary" type="submit" :disabled="working || !canSave">
            <EditPen class="button-icon" />
            <span>{{ form.id ? 'Save' : 'Create' }}</span>
          </button>
          <button class="button" type="button" :disabled="working || !form.id" @click="toggleStatus">
            <Switch class="button-icon" />
            <span>{{ form.status === 'ENABLED' ? 'Disable' : 'Enable' }}</span>
          </button>
          <button class="button" type="button" :disabled="working || !form.id || bindings.length === 0" @click="publish">
            <Upload class="button-icon" />
            <span>Publish</span>
          </button>
        </div>
      </form>

      <section class="version-section">
        <div class="panel-header subheader">
          <h2>Bound Rules</h2>
          <span>{{ bindings.length }} records</span>
        </div>

        <div class="binding-toolbar">
          <select v-model.number="bindingForm.ruleId" class="text-input compact-input" :disabled="!form.id">
            <option :value="0">Select enabled rule</option>
            <option v-for="rule in availableRules" :key="rule.id" :value="rule.id">
              {{ rule.ruleCode }} / v{{ rule.version }}
            </option>
          </select>
          <input v-model.number="bindingForm.executeOrder" class="text-input compact-input mini-number" min="0" type="number" :disabled="!form.id" />
          <label class="inline-checkbox">
            <input v-model="bindingForm.enabled" type="checkbox" :disabled="!form.id" />
            <span>Enabled</span>
          </label>
          <button class="button" type="button" :disabled="working || !form.id || !bindingForm.ruleId" @click="bindRule">
            <Plus class="button-icon" />
            <span>Bind</span>
          </button>
        </div>

        <div class="table-wrap binding-table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th scope="col">Rule</th>
                <th class="numeric" scope="col">Version</th>
                <th class="numeric" scope="col">Order</th>
                <th scope="col">Enabled</th>
                <th scope="col">Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="binding in bindings" :key="binding.ruleId">
                <td>
                  <span class="mono-cell">{{ binding.ruleCode }}</span>
                  <small>{{ binding.ruleName }}</small>
                </td>
                <td class="numeric">v{{ binding.ruleVersion }}</td>
                <td class="numeric">
                  <input v-model.number="binding.executeOrder" class="text-input compact-input order-input" min="0" type="number" />
                </td>
                <td>
                  <label class="inline-checkbox">
                    <input v-model="binding.enabled" type="checkbox" />
                    <span>{{ binding.enabled ? 'Yes' : 'No' }}</span>
                  </label>
                </td>
                <td>
                  <button class="button danger-button" type="button" :disabled="working" @click="removeRule(binding.ruleId)">Remove</button>
                </td>
              </tr>
              <tr v-if="bindings.length === 0">
                <td class="empty-table" colspan="5">
                  <StateBlock
                    compact
                    description="Bind enabled rules before publishing this strategy."
                    title="No rules bound"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="editor-actions compact-actions">
          <button class="button" type="button" :disabled="working || !form.id || bindings.length === 0" @click="saveBindings">
            <Check class="button-icon" />
            <span>Save Bindings</span>
          </button>
        </div>
      </section>

      <section class="version-section">
        <div class="panel-header subheader">
          <h2>Versions</h2>
          <span>{{ versions.length }} records</span>
        </div>
        <div class="version-list">
          <article v-for="version in versions" :key="version.id" class="version-item">
            <strong>v{{ version.version }} / {{ version.status }}</strong>
            <span>{{ formatDateTime(version.createdAt) }} / {{ version.grayRatio }}% gray / {{ snapshotCount(version.ruleSnapshot) }} rules</span>
            <small>{{ version.publishNote || 'No note' }}</small>
            <button class="button" type="button" :disabled="working || version.version === form.version" @click="rollback(version.version)">
              Rollback
            </button>
          </article>
          <StateBlock
            v-if="versions.length === 0"
            compact
            description="Publish this strategy to create a versioned rule snapshot."
            title="No published versions"
          />
        </div>
      </section>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, EditPen, Plus, Search, Switch, Upload } from '@element-plus/icons-vue'
import StateBlock from './StateBlock.vue'
import {
  bindStrategyRule,
  createStrategy,
  disableStrategy,
  enableStrategy,
  formatApiError,
  loadRules,
  loadStrategies,
  loadStrategy,
  loadStrategyRules,
  loadStrategyVersions,
  publishStrategy,
  removeStrategyRule,
  rollbackStrategy,
  updateStrategy,
} from '../api/client'
import type { EventType, PublishStatus, RiskRule, RiskStrategy, StrategyRule, StrategyVersion } from '../api/types'

const eventTypes: EventType[] = ['LOGIN', 'PAYMENT']
const statuses: PublishStatus[] = ['DRAFT', 'ENABLED', 'DISABLED']

const filters = reactive({
  keyword: '',
  eventType: '' as EventType | '',
  status: '' as PublishStatus | '',
})

const page = reactive({
  records: [] as RiskStrategy[],
  total: 0,
  pageNo: 1,
  pageSize: 10,
})

const form = reactive({
  id: null as number | null,
  strategyCode: '',
  strategyName: '',
  eventType: 'PAYMENT' as EventType,
  grayRatio: 100,
  status: 'DRAFT' as PublishStatus,
  version: 0,
  description: '',
})

const bindingForm = reactive({
  ruleId: 0,
  executeOrder: 100,
  enabled: true,
})

const bindings = ref<StrategyRule[]>([])
const versions = ref<StrategyVersion[]>([])
const availableRules = ref<RiskRule[]>([])
const publishNote = ref('')
const loading = ref(false)
const working = ref(false)
const error = ref('')
const feedback = reactive({
  type: 'success' as 'success' | 'error',
  message: '',
})

const totalPages = computed(() => Math.max(1, Math.ceil(page.total / page.pageSize)))
const canSave = computed(() => Boolean(form.strategyCode && form.strategyName && form.eventType && Number.isFinite(form.grayRatio)))

async function load(pageNo = page.pageNo, keepSelection = true) {
  error.value = ''
  loading.value = true
  try {
    const result = await loadStrategies({
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

    if (!keepSelection || (form.id && !page.records.some((strategy) => strategy.id === form.id))) {
      if (page.records.length > 0) {
        await selectStrategy(page.records[0])
      } else {
        startCreate()
      }
    } else if (!form.id && page.records.length > 0) {
      await selectStrategy(page.records[0])
    }
  } catch (err) {
    error.value = formatApiError(err, 'Failed to load strategies')
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

async function selectStrategy(strategy: RiskStrategy) {
  applyStrategy(strategy)
  feedback.message = ''
  publishNote.value = ''
  bindingForm.ruleId = 0
  await Promise.all([loadBindings(), loadVersions(), loadAvailableRules()])
}

function startCreate() {
  Object.assign(form, {
    id: null,
    strategyCode: '',
    strategyName: '',
    eventType: 'PAYMENT',
    grayRatio: 100,
    status: 'DRAFT',
    version: 0,
    description: '',
  })
  bindings.value = []
  versions.value = []
  publishNote.value = ''
  bindingForm.ruleId = 0
  feedback.message = ''
  void loadAvailableRules()
}

async function save() {
  if (!canSave.value) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    const payload = {
      strategyName: form.strategyName,
      eventType: form.eventType,
      grayRatio: Number(form.grayRatio),
      description: form.description,
    }
    const saved = form.id
      ? await updateStrategy(form.id, payload)
      : await createStrategy({ ...payload, strategyCode: form.strategyCode })
    applyStrategy(saved)
    await load(page.pageNo, true)
    await selectStrategy(saved)
    setFeedback('success', form.id ? 'Strategy saved' : 'Strategy created')
  } catch (err) {
    setFeedback('error', formatApiError(err, 'Save failed'))
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
    const updated = form.status === 'ENABLED' ? await disableStrategy(form.id) : await enableStrategy(form.id)
    applyStrategy(updated)
    await load(page.pageNo, true)
    setFeedback('success', updated.status === 'ENABLED' ? 'Strategy enabled' : 'Strategy disabled')
  } catch (err) {
    setFeedback('error', formatApiError(err, 'Status update failed'))
  } finally {
    working.value = false
  }
}

async function bindRule() {
  if (!form.id || !bindingForm.ruleId) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    await bindStrategyRule(form.id, {
      ruleId: bindingForm.ruleId,
      executeOrder: Number(bindingForm.executeOrder),
      enabled: bindingForm.enabled,
    })
    bindingForm.ruleId = 0
    await loadBindings()
    setFeedback('success', 'Rule bound')
  } catch (err) {
    setFeedback('error', formatApiError(err, 'Bind failed'))
  } finally {
    working.value = false
  }
}

async function saveBindings() {
  if (!form.id) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    for (const binding of bindings.value) {
      await bindStrategyRule(form.id, {
        ruleId: binding.ruleId,
        ruleVersion: binding.ruleVersion,
        executeOrder: Number(binding.executeOrder),
        enabled: binding.enabled,
      })
    }
    await loadBindings()
    setFeedback('success', 'Bindings saved')
  } catch (err) {
    setFeedback('error', formatApiError(err, 'Binding update failed'))
  } finally {
    working.value = false
  }
}

async function removeRule(ruleId: number) {
  if (!form.id || !window.confirm('Remove this rule binding?')) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    await removeStrategyRule(form.id, ruleId)
    await loadBindings()
    setFeedback('success', 'Rule binding removed')
  } catch (err) {
    setFeedback('error', formatApiError(err, 'Remove failed'))
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
    await publishStrategy(form.id, publishNote.value)
    const refreshed = await loadStrategy(form.id)
    applyStrategy(refreshed)
    await Promise.all([load(page.pageNo, true), loadVersions()])
    setFeedback('success', 'Strategy published')
  } catch (err) {
    setFeedback('error', formatApiError(err, 'Publish failed'))
  } finally {
    working.value = false
  }
}

async function rollback(version: number) {
  if (!form.id || !window.confirm(`Rollback to strategy version ${version}?`)) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    await rollbackStrategy(form.id, version)
    const refreshed = await loadStrategy(form.id)
    applyStrategy(refreshed)
    await Promise.all([load(page.pageNo, true), loadVersions()])
    setFeedback('success', `Rolled back to v${version}`)
  } catch (err) {
    setFeedback('error', formatApiError(err, 'Rollback failed'))
  } finally {
    working.value = false
  }
}

async function loadBindings() {
  if (!form.id) {
    bindings.value = []
    return
  }
  bindings.value = await loadStrategyRules(form.id)
}

async function loadVersions() {
  if (!form.id) {
    versions.value = []
    return
  }
  versions.value = await loadStrategyVersions(form.id)
}

async function loadAvailableRules() {
  try {
    const result = await loadRules({
      pageNo: 1,
      pageSize: 100,
      eventType: form.eventType,
      status: 'ENABLED',
      keyword: '',
    })
    availableRules.value = result.records
  } catch {
    availableRules.value = []
  }
}

function applyStrategy(strategy: RiskStrategy) {
  Object.assign(form, {
    id: strategy.id,
    strategyCode: strategy.strategyCode,
    strategyName: strategy.strategyName,
    eventType: strategy.eventType,
    grayRatio: strategy.grayRatio,
    status: strategy.status,
    version: strategy.version,
    description: strategy.description ?? '',
  })
}

function setFeedback(type: 'success' | 'error', message: string) {
  feedback.type = type
  feedback.message = message
}

function snapshotCount(value: string) {
  try {
    const parsed = JSON.parse(value) as unknown[]
    return Array.isArray(parsed) ? parsed.length : 0
  } catch {
    return 0
  }
}

function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  void load(1, false)
})
</script>
