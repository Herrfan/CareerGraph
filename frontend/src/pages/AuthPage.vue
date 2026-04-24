<template>
  <AppShell>
    <section class="page-grid auth-page">
      <CardPanel
        eyebrow="Account"
        title="登录或注册"
        description="登录后才会保存学生画像、报告快照和智能体对话记忆。游客模式不保留历史。"
      >
        <div class="auth-layout">
          <div class="auth-panel">
            <div class="mode-toggle">
              <button :class="['mode-toggle__item', mode === 'login' && 'mode-toggle__item--active']" type="button" @click="mode = 'login'">
                登录
              </button>
              <button :class="['mode-toggle__item', mode === 'register' && 'mode-toggle__item--active']" type="button" @click="mode = 'register'">
                注册
              </button>
            </div>

            <form class="form-grid" @submit.prevent="handleSubmit">
              <input v-model="username" class="input" placeholder="用户名" required />
              <input v-model="password" class="input" type="password" placeholder="密码（至少 6 位）" required />
              <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
              <div class="auth-actions">
                <AppButton type="submit" :disabled="authStore.isLoading">
                  {{ authStore.isLoading ? '提交中...' : mode === 'login' ? '登录' : '注册并登录' }}
                </AppButton>
                <AppButton variant="secondary" type="button" @click="continueAsGuest">游客进入</AppButton>
              </div>
            </form>
          </div>

          <div class="auth-panel auth-panel--note">
            <div class="note-card">
              <p class="note-card__label">账号模式</p>
              <strong>保存画像、报告和对话历史</strong>
              <p class="text-secondary">适合长期使用，也方便后续持续补全职业规划记录。</p>
            </div>
            <div class="note-card">
              <p class="note-card__label">游客模式</p>
              <strong>可直接体验，但不会保留历史</strong>
              <p class="text-secondary">刷新页面或退出后，游客模式下的数据和记忆都不会自动恢复。</p>
            </div>
          </div>
        </div>
      </CardPanel>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/layout/AppShell.vue';
import AppButton from '@/components/ui/AppButton.vue';
import CardPanel from '@/components/ui/CardPanel.vue';
import { useAuthStore } from '@/stores/auth';
import { useProfileStore } from '@/stores/profile';
import { useReportStore } from '@/stores/report';

const router = useRouter();
const authStore = useAuthStore();
const profileStore = useProfileStore();
const reportStore = useReportStore();
const mode = ref<'login' | 'register'>('login');
const username = ref('');
const password = ref('');
const errorMessage = ref('');

async function handleSubmit() {
  errorMessage.value = '';
  try {
    if (mode.value === 'login') {
      await authStore.loginWithPassword(username.value, password.value);
    } else {
      await authStore.registerWithPassword(username.value, password.value);
    }
    await profileStore.loadMyProfile();
    router.push('/');
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '提交失败，请稍后重试';
  }
}

async function continueAsGuest() {
  await authStore.logoutCurrentUser();
  authStore.enterGuestMode();
  profileStore.clearProfile();
  reportStore.reset();
  router.push('/');
}
</script>

<style scoped>
.auth-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
}

.auth-panel,
.auth-panel--note {
  display: grid;
  gap: var(--space-4);
}

.mode-toggle {
  display: inline-flex;
  padding: 0.2rem;
  border: 1px solid color-mix(in oklab, var(--brand-500) 12%, var(--border-subtle));
  border-radius: 10px;
  background: color-mix(in oklab, var(--bg-surface) 96%, white);
}

.mode-toggle__item {
  min-width: 86px;
  padding: 0.45rem 0.75rem;
  border-radius: 8px;
  color: var(--text-secondary);
  font-weight: 600;
}

.mode-toggle__item--active {
  color: var(--brand-700);
  background: color-mix(in oklab, var(--brand-500) 10%, white);
}

.auth-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.note-card {
  display: grid;
  gap: 0.45rem;
  padding: 1rem 1.05rem;
  border-radius: 14px;
  border: 1px solid color-mix(in oklab, var(--brand-500) 10%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-surface) 98%, white);
}

.note-card p {
  margin: 0;
}

.note-card__label {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.form-error {
  margin: 0;
  color: var(--danger-500);
}

@media (max-width: 960px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }
}
</style>
