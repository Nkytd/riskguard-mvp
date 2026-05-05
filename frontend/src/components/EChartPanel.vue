<template>
  <section class="panel">
    <div class="panel-header">
      <h2>{{ title }}</h2>
      <span v-if="caption">{{ caption }}</span>
    </div>
    <div ref="chartElement" class="chart-surface" />
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import type { EChartsCoreOption, EChartsType } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  GridComponent,
  LegendComponent,
  LineChart,
  PieChart,
  TooltipComponent,
  CanvasRenderer,
])

const props = defineProps<{
  title: string
  caption?: string
  option: EChartsCoreOption
}>()

const chartElement = ref<HTMLDivElement | null>(null)
let chart: EChartsType | null = null

const render = () => {
  if (!chartElement.value) {
    return
  }
  if (!chart) {
    chart = echarts.init(chartElement.value)
  }
  chart.setOption(props.option, true)
}

const resize = () => chart?.resize()

onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})

watch(() => props.option, render, { deep: true })
</script>
