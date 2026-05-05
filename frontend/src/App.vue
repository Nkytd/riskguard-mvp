<template>
  <LoginView v-if="!currentUser" @signed-in="setUser" />
  <DashboardView v-else :user="currentUser" @signed-out="signOut" />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import DashboardView from './components/DashboardView.vue'
import LoginView from './components/LoginView.vue'
import { clearSession, getStoredUser, getToken, loadCurrentUser } from './api/client'
import type { AuthUser } from './api/types'

const currentUser = ref<AuthUser | null>(getStoredUser())

function setUser(user: AuthUser) {
  currentUser.value = user
}

function signOut() {
  clearSession()
  currentUser.value = null
}

onMounted(async () => {
  if (!getToken()) {
    currentUser.value = null
    return
  }
  try {
    currentUser.value = await loadCurrentUser()
  } catch {
    signOut()
  }
})
</script>
