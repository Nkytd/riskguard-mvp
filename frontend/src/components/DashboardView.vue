<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-mark compact">RG</div>
        <span>RiskGuard</span>
      </div>
      <nav class="nav-menu">
        <button :class="['nav-item', { active: activeView === 'dashboard' }]" type="button" @click="activeView = 'dashboard'">
          <DataBoard class="nav-icon" />
          Dashboard
        </button>
        <button :class="['nav-item', { active: activeView === 'rules' }]" type="button" @click="activeView = 'rules'">
          <DocumentChecked class="nav-icon" />
          Rules
        </button>
        <button :class="['nav-item', { active: activeView === 'strategies' }]" type="button" @click="activeView = 'strategies'">
          <Tickets class="nav-icon" />
          Strategies
        </button>
        <button :class="['nav-item', { active: activeView === 'lists' }]" type="button" @click="activeView = 'lists'">
          <Collection class="nav-icon" />
          Lists
        </button>
        <button :class="['nav-item', { active: activeView === 'decisions' }]" type="button" @click="activeView = 'decisions'">
          <DataAnalysis class="nav-icon" />
          Decisions
        </button>
        <button :class="['nav-item', { active: activeView === 'cases' }]" type="button" @click="activeView = 'cases'">
          <FolderOpened class="nav-icon" />
          Cases
        </button>
      </nav>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageSubtitle }}</p>
        </div>
        <div class="topbar-actions">
          <button v-if="activeView === 'dashboard'" class="button" type="button" :disabled="loading" @click="load">
            <RefreshRight class="button-icon" />
            <span>{{ loading ? 'Refreshing' : 'Refresh' }}</span>
          </button>
          <div class="user-chip">
            <Avatar class="button-icon" />
            <span>{{ user.realName || user.username }}</span>
            <small>{{ user.roleCode }}</small>
          </div>
          <button class="button" type="button" @click="emit('signedOut')">
            <SwitchButton class="button-icon" />
            <span>Logout</span>
          </button>
        </div>
      </header>

      <template v-if="activeView === 'dashboard'">
        <StateBlock
          v-if="error && !dashboard"
          action-label="Retry"
          :description="error"
          title="Dashboard unavailable"
          variant="error"
          @action="load"
        />
        <div v-else-if="error" class="alert alert-error" role="alert">{{ error }}</div>
        <div v-if="loading && !dashboard && !error" class="skeleton-panel" aria-label="Loading dashboard">
          <span v-for="item in 12" :key="item" />
        </div>

        <template v-if="dashboard">
          <section class="metric-grid">
            <article v-for="item in metricItems" :key="item.label" class="metric-tile">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.detail }}</small>
            </article>
          </section>

          <section class="chart-grid">
            <EChartPanel title="Risk Trend" caption="Last 24 hours" :option="trendOption" />
            <EChartPanel title="Decision Mix" caption="All decisions" :option="decisionOption" />
          </section>

          <section class="content-grid">
            <section class="panel">
              <div class="panel-header">
                <h2>Rule Hit Rank</h2>
                <span>Top 10</span>
              </div>
              <div class="table-wrap">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th scope="col">Code</th>
                      <th scope="col">Name</th>
                      <th class="numeric" scope="col">Hits</th>
                      <th class="numeric" scope="col">Score</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in dashboard.ruleHitRank" :key="item.ruleCode">
                      <td>{{ item.ruleCode }}</td>
                      <td>{{ item.ruleName }}</td>
                      <td class="numeric">{{ formatNumber(item.hitCount) }}</td>
                      <td class="numeric">{{ formatNumber(item.totalScoreDelta) }}</td>
                    </tr>
                    <tr v-if="dashboard.ruleHitRank.length === 0">
                      <td class="empty-table" colspan="4">
                        <StateBlock
                          compact
                          description="No rules have been hit in the current decision sample."
                          title="No rule hits"
                        />
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <EChartPanel title="Case Status" caption="Workflow state" :option="caseOption" />
          </section>
        </template>
      </template>

      <RulesView v-else-if="activeView === 'rules'" />
      <StrategyView v-else-if="activeView === 'strategies'" />
      <ListsView v-else-if="activeView === 'lists'" />
      <DecisionsView v-else-if="activeView === 'decisions'" />
      <CasesView v-else />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Avatar, Collection, DataAnalysis, DataBoard, DocumentChecked, FolderOpened, RefreshRight, SwitchButton, Tickets } from '@element-plus/icons-vue'
import type { EChartsCoreOption } from 'echarts/core'
import CasesView from './CasesView.vue'
import DecisionsView from './DecisionsView.vue'
import EChartPanel from './EChartPanel.vue'
import ListsView from './ListsView.vue'
import RulesView from './RulesView.vue'
import StateBlock from './StateBlock.vue'
import StrategyView from './StrategyView.vue'
import { formatApiError, loadDashboard } from '../api/client'
import type { AuthUser, DashboardData } from '../api/types'

type ConsoleView = 'dashboard' | 'rules' | 'strategies' | 'lists' | 'decisions' | 'cases'

defineProps<{
  user: AuthUser
}>()

const emit = defineEmits<{
  signedOut: []
}>()

const activeView = ref<ConsoleView>('dashboard')
const dashboard = ref<DashboardData | null>(null)
const loading = ref(false)
const error = ref('')

const pageTitle = computed(() => {
  switch (activeView.value) {
    case 'dashboard':
      return 'Dashboard'
    case 'rules':
      return 'Rules'
    case 'strategies':
      return 'Strategies'
    case 'lists':
      return 'Lists'
    case 'decisions':
      return 'Decisions'
    case 'cases':
      return 'Cases'
  }
})
const pageSubtitle = computed(() => {
  switch (activeView.value) {
    case 'dashboard':
      return todayRange.value
    case 'rules':
      return 'Rule definitions, status and version publishing'
    case 'strategies':
      return 'Strategy definitions, rule bindings and publishing'
    case 'lists':
      return 'Blacklist, whitelist and effective windows'
    case 'decisions':
      return 'Decision logs, risk reasons and hit rules'
    case 'cases':
      return 'Case queue, audit actions and operation history'
  }
})

async function load() {
  error.value = ''
  loading.value = true
  try {
    dashboard.value = await loadDashboard()
  } catch (err) {
    error.value = formatApiError(err, 'Failed to load dashboard')
  } finally {
    loading.value = false
  }
}

const todayRange = computed(() => {
  if (!dashboard.value) {
    return 'Loading operational metrics'
  }
  return `${formatDateTime(dashboard.value.overview.startTime)} - ${formatDateTime(dashboard.value.overview.endTime)}`
})

const metricItems = computed(() => {
  const overview = dashboard.value?.overview
  const cases = dashboard.value?.caseStatistics
  if (!overview || !cases) {
    return []
  }
  return [
    { label: 'Decisions Today', value: formatNumber(overview.todayDecisionCount), detail: `${overview.averageCostMs.toFixed(2)} ms avg` },
    { label: 'Pass', value: formatNumber(overview.passCount), detail: 'Low risk outcomes' },
    { label: 'Verify', value: formatNumber(overview.verifyCount), detail: 'Step-up checks' },
    { label: 'Review', value: formatNumber(overview.reviewCount), detail: 'Manual workflow' },
    { label: 'Reject', value: formatNumber(overview.rejectCount), detail: 'Blocked events' },
    { label: 'High Risk', value: formatNumber(overview.highAndCriticalRiskCount), detail: `${overview.criticalRiskCount} critical` },
    { label: 'Cases', value: formatNumber(cases.totalCount), detail: `${rate(cases.approvalRate)} approve rate` },
    { label: 'Pending', value: formatNumber(cases.pendingCount), detail: `${cases.processingCount} processing` },
  ]
})

const trendOption = computed<EChartsCoreOption>(() => {
  const rows = dashboard.value?.riskTrend ?? []
  const option: EChartsCoreOption = {
    color: ['#256f63', '#c45a3c', '#6d5bd0'],
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 42, right: 24, top: 48, bottom: 32 },
    xAxis: {
      type: 'category',
      data: rows.map((item) => item.timeBucket.slice(11, 16)),
      axisTick: { show: false },
    },
    yAxis: { type: 'value' },
    series: [
      { name: 'Decisions', type: 'line', smooth: true, data: rows.map((item) => item.decisionCount), areaStyle: { opacity: 0.08 } },
      { name: 'Review', type: 'line', smooth: true, data: rows.map((item) => item.reviewCount) },
      { name: 'Reject', type: 'line', smooth: true, data: rows.map((item) => item.rejectCount) },
    ],
  }
  return option
})

const decisionOption = computed<EChartsCoreOption>(() => {
  const option: EChartsCoreOption = {
    color: ['#256f63', '#d3a02d', '#6d5bd0', '#c45a3c'],
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        name: 'Decision',
        type: 'pie',
        radius: ['52%', '72%'],
        center: ['50%', '45%'],
        data: dashboard.value?.decisionDistribution.map((item) => ({ name: item.name, value: item.count })) ?? [],
        label: { formatter: '{b}' },
      },
    ],
  }
  return option
})

const caseOption = computed<EChartsCoreOption>(() => {
  const cases = dashboard.value?.caseStatistics
  const data: Array<[string, number]> = cases
    ? [
        ['Pending', cases.pendingCount],
        ['Processing', cases.processingCount],
        ['Approved', cases.approvedCount],
        ['Rejected', cases.rejectedCount],
        ['Closed', cases.closedCount],
      ]
    : []
  const option: EChartsCoreOption = {
    color: ['#256f63'],
    tooltip: { trigger: 'axis' },
    grid: { left: 42, right: 24, top: 24, bottom: 36 },
    xAxis: { type: 'category', data: data.map(([name]) => name), axisTick: { show: false } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: data.map(([, value]) => value), barWidth: 30, itemStyle: { borderRadius: [4, 4, 0, 0] } }],
  }
  return option
})

function formatNumber(value: number) {
  return new Intl.NumberFormat('en-US').format(value)
}

function rate(value: number) {
  return `${Math.round(value * 100)}%`
}

function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(load)
</script>
