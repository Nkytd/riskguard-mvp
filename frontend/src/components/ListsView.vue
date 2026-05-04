<template>
  <section class="management-workspace">
    <section class="panel management-list-panel">
      <div class="panel-header">
        <h2>List Entries</h2>
        <span>{{ page.total }} total</span>
      </div>

      <form class="filter-bar list-filter-bar" @submit.prevent="search">
        <input
          v-model.trim="filters.keyword"
          class="text-input compact-input"
          placeholder="Search value or reason"
          @keydown.enter.prevent="search"
        />
        <select v-model="filters.listType" class="text-input compact-input">
          <option value="">All Lists</option>
          <option v-for="item in listTypes" :key="item" :value="item">{{ item }}</option>
        </select>
        <select v-model="filters.objectType" class="text-input compact-input">
          <option value="">All Objects</option>
          <option v-for="item in objectTypes" :key="item" :value="item">{{ item }}</option>
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
      <div v-if="loading && page.records.length === 0" class="skeleton-panel compact-skeleton" aria-label="Loading list entries">
        <span v-for="item in 8" :key="item" />
      </div>

      <div v-else class="table-wrap management-table-wrap">
        <table class="data-table selectable-table">
          <thead>
            <tr>
              <th scope="col">List</th>
              <th scope="col">Object</th>
              <th scope="col">Value</th>
              <th scope="col">Effect</th>
              <th scope="col">Status</th>
              <th scope="col">Updated</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="entry in page.records"
              :key="entry.id"
              :class="{ selected: entry.id === form.id }"
              @click="selectEntry(entry)"
            >
              <td>
                <span :class="['list-type-pill', entry.listType.toLowerCase()]">{{ entry.listType }}</span>
              </td>
              <td>{{ entry.objectType }}</td>
              <td class="mono-cell">{{ entry.objectValue }}</td>
              <td>
                {{ entry.effectType }}
                <small>{{ entry.riskLevel || 'NO_LEVEL' }} / {{ signedScore(entry.scoreDelta) }}</small>
              </td>
              <td><span :class="['status-pill', entry.status.toLowerCase()]">{{ entry.status }}</span></td>
              <td>{{ formatDateTime(entry.updatedAt) }}</td>
            </tr>
            <tr v-if="page.records.length === 0">
              <td class="empty-table" colspan="6">No list entries found</td>
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
        <h2>{{ form.id ? 'List Detail' : 'New List Entry' }}</h2>
        <span>{{ form.id ? form.status : 'Enabled after create' }}</span>
      </div>

      <div v-if="feedback.message" :class="['alert', feedback.type === 'error' ? 'alert-error' : 'alert-success']" role="status">
        {{ feedback.message }}
      </div>

      <form class="management-form" @submit.prevent="save">
        <div class="form-grid">
          <label class="form-field">
            <span>List Type</span>
            <select v-model="form.listType" class="text-input" :disabled="Boolean(form.id)">
              <option v-for="item in listTypes" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label class="form-field">
            <span>Object Type</span>
            <select v-model="form.objectType" class="text-input" :disabled="Boolean(form.id)">
              <option v-for="item in objectTypes" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
        </div>
        <label class="form-field">
          <span>Object Value</span>
          <input v-model.trim="form.objectValue" class="text-input mono-input" :disabled="Boolean(form.id)" />
        </label>
        <div class="form-grid">
          <label class="form-field">
            <span>Effect</span>
            <select v-model="form.effectType" class="text-input">
              <option v-for="item in effectTypes" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label class="form-field">
            <span>Risk Level</span>
            <select v-model="form.riskLevel" class="text-input">
              <option value="">NO_LEVEL</option>
              <option v-for="item in riskLevels" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
        </div>
        <div class="form-grid">
          <label class="form-field">
            <span>Score Delta</span>
            <input v-model.number="form.scoreDelta" class="text-input" type="number" />
          </label>
          <label class="form-field">
            <span>Start Time</span>
            <input v-model="form.startTime" class="text-input" type="datetime-local" />
          </label>
        </div>
        <label class="form-field">
          <span>End Time</span>
          <input v-model="form.endTime" class="text-input" type="datetime-local" />
        </label>
        <label class="form-field">
          <span>Reason</span>
          <textarea v-model.trim="form.reason" class="text-input textarea-input" rows="3" />
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
          <button class="button danger-button" type="button" :disabled="working || !form.id" @click="removeEntry">
            <Delete class="button-icon" />
            <span>Delete</span>
          </button>
        </div>
      </form>

      <section class="version-section">
        <div class="panel-header subheader">
          <h2>Entry Window</h2>
          <span>{{ form.id ? `#${form.id}` : 'New' }}</span>
        </div>
        <dl class="detail-list">
          <div>
            <dt>Active From</dt>
            <dd>{{ form.startTime ? formatDateTime(form.startTime) : 'Immediate' }}</dd>
          </div>
          <div>
            <dt>Active Until</dt>
            <dd>{{ form.endTime ? formatDateTime(form.endTime) : 'Open ended' }}</dd>
          </div>
          <div>
            <dt>Last Updated</dt>
            <dd>{{ form.updatedAt ? formatDateTime(form.updatedAt) : 'Not saved' }}</dd>
          </div>
        </dl>
      </section>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, EditPen, Plus, Search, Switch } from '@element-plus/icons-vue'
import {
  createRiskList,
  deleteRiskList,
  disableRiskList,
  enableRiskList,
  loadRiskList,
  loadRiskLists,
  updateRiskList,
} from '../api/client'
import type { ListEffectType, ListType, ObjectType, PublishStatus, RiskLevel, RiskList } from '../api/types'

const listTypes: ListType[] = ['BLACK', 'WHITE']
const objectTypes: ObjectType[] = ['USER', 'DEVICE', 'IP', 'PHONE', 'MERCHANT']
const effectTypes: ListEffectType[] = ['REJECT', 'SCORE_UP', 'SCORE_DOWN']
const riskLevels: RiskLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const statuses: PublishStatus[] = ['DRAFT', 'ENABLED', 'DISABLED']

const filters = reactive({
  keyword: '',
  listType: '' as ListType | '',
  objectType: '' as ObjectType | '',
  status: '' as PublishStatus | '',
})

const page = reactive({
  records: [] as RiskList[],
  total: 0,
  pageNo: 1,
  pageSize: 10,
})

const form = reactive({
  id: null as number | null,
  listType: 'BLACK' as ListType,
  objectType: 'USER' as ObjectType,
  objectValue: '',
  riskLevel: 'CRITICAL' as RiskLevel | '',
  effectType: 'REJECT' as ListEffectType,
  scoreDelta: 100,
  reason: '',
  startTime: '',
  endTime: '',
  status: 'ENABLED' as PublishStatus,
  updatedAt: '',
})

const loading = ref(false)
const working = ref(false)
const error = ref('')
const feedback = reactive({
  type: 'success' as 'success' | 'error',
  message: '',
})

const totalPages = computed(() => Math.max(1, Math.ceil(page.total / page.pageSize)))
const canSave = computed(() => {
  return Boolean(form.listType && form.objectType && form.objectValue && form.effectType && Number.isFinite(form.scoreDelta))
})

async function load(pageNo = page.pageNo, keepSelection = true) {
  error.value = ''
  loading.value = true
  try {
    const result = await loadRiskLists({
      pageNo,
      pageSize: page.pageSize,
      listType: filters.listType,
      objectType: filters.objectType,
      status: filters.status,
      keyword: filters.keyword,
    })
    page.records = result.records
    page.total = result.total
    page.pageNo = result.pageNo
    page.pageSize = result.pageSize

    if (!keepSelection || (form.id && !page.records.some((entry) => entry.id === form.id))) {
      if (page.records.length > 0) {
        await selectEntry(page.records[0])
      } else {
        startCreate()
      }
    } else if (!form.id && page.records.length > 0) {
      await selectEntry(page.records[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load list entries'
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

async function selectEntry(entry: RiskList) {
  applyEntry(entry)
  feedback.message = ''
  try {
    applyEntry(await loadRiskList(entry.id))
  } catch {
    setFeedback('error', 'Failed to refresh list detail')
  }
}

function startCreate() {
  Object.assign(form, {
    id: null,
    listType: 'BLACK',
    objectType: 'USER',
    objectValue: '',
    riskLevel: 'CRITICAL',
    effectType: 'REJECT',
    scoreDelta: 100,
    reason: '',
    startTime: '',
    endTime: '',
    status: 'ENABLED',
    updatedAt: '',
  })
  feedback.message = ''
}

async function save() {
  if (!canSave.value) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    const isCreate = !form.id
    const payload = {
      riskLevel: form.riskLevel,
      effectType: form.effectType,
      scoreDelta: Number(form.scoreDelta),
      reason: form.reason,
      startTime: toApiDate(form.startTime),
      endTime: toApiDate(form.endTime),
    }
    const saved = isCreate
      ? await createRiskList({
          ...payload,
          listType: form.listType,
          objectType: form.objectType,
          objectValue: form.objectValue,
        })
      : await updateRiskList(form.id as number, payload)
    applyEntry(saved)
    await load(page.pageNo, true)
    await selectEntry(saved)
    setFeedback('success', isCreate ? 'List entry created' : 'List entry saved')
  } catch (err) {
    setFeedback('error', err instanceof Error ? err.message : 'Save failed')
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
    const updated = form.status === 'ENABLED' ? await disableRiskList(form.id) : await enableRiskList(form.id)
    applyEntry(updated)
    await load(page.pageNo, true)
    setFeedback('success', updated.status === 'ENABLED' ? 'List entry enabled' : 'List entry disabled')
  } catch (err) {
    setFeedback('error', err instanceof Error ? err.message : 'Status update failed')
  } finally {
    working.value = false
  }
}

async function removeEntry() {
  if (!form.id || !window.confirm('Delete this list entry?')) {
    return
  }
  feedback.message = ''
  working.value = true
  try {
    await deleteRiskList(form.id)
    await load(1, false)
    setFeedback('success', 'List entry deleted')
  } catch (err) {
    setFeedback('error', err instanceof Error ? err.message : 'Delete failed')
  } finally {
    working.value = false
  }
}

function applyEntry(entry: RiskList) {
  Object.assign(form, {
    id: entry.id,
    listType: entry.listType,
    objectType: entry.objectType,
    objectValue: entry.objectValue,
    riskLevel: entry.riskLevel ?? '',
    effectType: entry.effectType,
    scoreDelta: entry.scoreDelta,
    reason: entry.reason ?? '',
    startTime: toDateTimeInput(entry.startTime),
    endTime: toDateTimeInput(entry.endTime),
    status: entry.status,
    updatedAt: entry.updatedAt,
  })
}

function setFeedback(type: 'success' | 'error', message: string) {
  feedback.type = type
  feedback.message = message
}

function signedScore(value: number) {
  return value > 0 ? `+${value}` : String(value)
}

function toDateTimeInput(value: string | null) {
  return value ? value.replace(' ', 'T').slice(0, 16) : ''
}

function toApiDate(value: string) {
  if (!value) {
    return undefined
  }
  return value.length === 16 ? `${value}:00` : value
}

function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  void load(1, false)
})
</script>
