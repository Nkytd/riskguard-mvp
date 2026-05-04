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
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

const props = defineProps<{
  title: string
  caption?: string
  option: EChartsOption
}>()

const chartElement = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

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
