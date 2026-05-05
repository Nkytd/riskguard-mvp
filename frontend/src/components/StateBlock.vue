<template>
  <div :class="['state-block', `state-block-${variant}`, { 'state-block-compact': compact }]" :role="role">
    <component :is="iconComponent" class="state-icon" />
    <div class="state-copy">
      <strong>{{ title }}</strong>
      <p v-if="description">{{ description }}</p>
    </div>
    <button v-if="actionLabel" class="button state-action" type="button" @click="emit('action')">
      <RefreshRight class="button-icon" />
      <span>{{ actionLabel }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RefreshRight, Search, Warning } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  variant?: 'empty' | 'error' | 'loading'
  title: string
  description?: string
  actionLabel?: string
  compact?: boolean
}>(), {
  variant: 'empty',
  description: '',
  actionLabel: '',
  compact: false,
})

const emit = defineEmits<{
  action: []
}>()

const iconComponent = computed(() => {
  if (props.variant === 'error') {
    return Warning
  }
  if (props.variant === 'loading') {
    return RefreshRight
  }
  return Search
})

const role = computed(() => props.variant === 'error' ? 'alert' : 'status')
</script>
