<template>
  <main class="login-screen">
    <section class="login-panel">
      <div class="brand-mark">RG</div>
      <div class="login-heading">
        <h1>RiskGuard</h1>
        <p>Management Console</p>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <label class="form-field">
          <span>Username</span>
          <input v-model.trim="form.username" class="text-input" autocomplete="username" />
        </label>
        <label class="form-field">
          <span>Password</span>
          <input v-model="form.password" class="text-input" type="password" autocomplete="current-password" />
        </label>
        <div v-if="error" class="alert alert-error" role="alert">{{ error }}</div>
        <button class="button primary login-button" type="submit" :disabled="!canSubmit">
          <Promotion class="button-icon" />
          <span>{{ loading ? 'Signing In' : 'Sign In' }}</span>
        </button>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { formatApiError, login, saveSession } from '../api/client'
import type { AuthUser } from '../api/types'

const emit = defineEmits<{
  signedIn: [user: AuthUser]
}>()

const form = reactive({
  username: '',
  password: '',
})
const loading = ref(false)
const error = ref('')
const canSubmit = computed(() => !loading.value && form.username.length > 0 && form.password.length > 0)

async function submit() {
  if (!canSubmit.value) {
    return
  }
  error.value = ''
  loading.value = true
  try {
    const result = await login(form.username, form.password)
    saveSession(result.accessToken, result.user)
    emit('signedIn', result.user)
  } catch (err) {
    error.value = formatApiError(err, 'Sign in failed')
  } finally {
    loading.value = false
  }
}
</script>
