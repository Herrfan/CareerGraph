<template>
  <div class="app-shell">
    <header class="app-shell__header">
      <div class="page-shell app-shell__header-inner">
        <div class="app-shell__utility">
          <p class="app-shell__utility-copy">Career Agent Plus</p>
          <div class="app-shell__utility-actions">
            <template v-if="authStore.isAuthenticated && authStore.currentUser">
              <span class="app-shell__account-chip">{{ authStore.currentUser.username }}</span>
              <button type="button" class="app-shell__account-link" @click="logout">{{ labels.logout }}</button>
            </template>
            <template v-else-if="authStore.isGuest">
              <span class="app-shell__account-chip">{{ labels.guest }}</span>
              <RouterLink to="/auth" class="app-shell__account-link">{{ labels.login }}</RouterLink>
              <span class="app-shell__divider">/</span>
              <RouterLink to="/auth" class="app-shell__account-link">{{ labels.register }}</RouterLink>
            </template>
            <template v-else>
              <RouterLink to="/auth" class="app-shell__account-link">{{ labels.login }}</RouterLink>
              <span class="app-shell__divider">/</span>
              <RouterLink to="/auth" class="app-shell__account-link">{{ labels.register }}</RouterLink>
            </template>
          </div>
        </div>

        <div class="app-shell__mainbar">
          <RouterLink class="app-shell__brand" to="/">
            <div class="app-shell__brand-mark">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M2 17L12 22L22 17" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M2 12L12 17L22 12" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span class="app-shell__brand-copy">
              <strong class="app-shell__brand-title">{{ labels.brand }}</strong>
              <span class="app-shell__brand-subtitle">{{ labels.brandSubtitle }}</span>
            </span>
          </RouterLink>

          <div class="app-shell__mainbar-right">
            <nav class="app-shell__nav" aria-label="主导航">
              <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="app-shell__nav-link">
                {{ item.label }}
              </RouterLink>
              <RouterLink v-if="authStore.isAuthenticated" to="/history" class="app-shell__nav-link">
                {{ labels.history }}
              </RouterLink>
              <RouterLink v-if="authStore.isAdmin" to="/admin/jobs" class="app-shell__nav-link">
                {{ labels.adminPanel }}
              </RouterLink>
            </nav>

            <div class="app-shell__lang-row">
              <button
                type="button"
                :class="['app-shell__lang-button', localeStore.locale === 'zh' && 'app-shell__lang-button--active']"
                @click="localeStore.setLocale('zh')"
                aria-label="切换到中文"
              >
                CN
              </button>
              <button
                type="button"
                :class="['app-shell__lang-button', localeStore.locale === 'en' && 'app-shell__lang-button--active']"
                @click="localeStore.setLocale('en')"
                aria-label="Switch to English"
              >
                EN
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main class="page-shell app-shell__main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useChatStore } from '@/stores/chat';
import { useLocaleStore } from '@/stores/locale';
import { useProfileStore } from '@/stores/profile';
import { useReportStore } from '@/stores/report';

const router = useRouter();
const authStore = useAuthStore();
const chatStore = useChatStore();
const profileStore = useProfileStore();
const reportStore = useReportStore();
const localeStore = useLocaleStore();

const labels = computed(() =>
  localeStore.locale === 'en'
    ? {
        brand: 'Career Agent',
        brandSubtitle: 'University Career Planning Platform',
        home: 'Home',
        profile: 'Profile',
        jobs: 'Jobs',
        matching: 'Matching',
        path: 'Career Path',
        report: 'Report Center',
        history: 'History',
        adminPanel: 'Admin Panel',
        logout: 'Logout',
        guest: 'Guest',
        login: 'Login',
        register: 'Register',
      }
    : {
        brand: '大学生职业规划智能体',
        brandSubtitle: 'University Career Planning Platform',
        home: '首页',
        profile: '学生画像',
        jobs: '岗位浏览',
        matching: '岗位匹配',
        path: '职业路径',
        report: '报告中心',
        history: '我的记录',
        adminPanel: '管理后台',
        logout: '退出登录',
        guest: '游客模式',
        login: '登录',
        register: '注册',
      },
);

const navItems = computed(() => [
  { to: '/', label: labels.value.home },
  { to: '/profile', label: labels.value.profile },
  { to: '/jobs', label: labels.value.jobs },
  { to: '/matching', label: labels.value.matching },
  { to: '/career-path', label: labels.value.path },
  { to: '/report', label: labels.value.report },
]);

async function logout() {
  await authStore.logoutCurrentUser();
  chatStore.reset();
  profileStore.clearProfile();
  reportStore.reset();
  router.push('/auth');
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 96%, white), var(--bg-canvas) 22%, var(--bg-canvas)),
    radial-gradient(circle at top right, color-mix(in oklab, var(--brand-500) 5%, transparent), transparent 30%);
}

.app-shell__header {
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid color-mix(in oklab, var(--brand-700) 7%, var(--border-subtle));
  background:
    linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 94%, white), color-mix(in oklab, var(--bg-surface) 90%, white));
  backdrop-filter: blur(18px);
  box-shadow: 0 16px 34px color-mix(in oklab, var(--brand-700) 5%, transparent);
}

.app-shell__header-inner {
  display: grid;
  gap: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.app-shell__utility {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  min-height: 2.15rem;
  padding: 0.28rem 0;
  border-bottom: 1px solid color-mix(in oklab, var(--brand-700) 5%, var(--border-subtle));
}

.app-shell__utility-copy {
  margin: 0;
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.app-shell__utility-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.app-shell__mainbar {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 1.4rem;
  align-items: center;
  min-height: 3.95rem;
  padding: 0.55rem 0;
}

.app-shell__brand {
  display: inline-flex;
  align-items: center;
  gap: 0.7rem;
  min-width: 0;
}

.app-shell__brand-mark {
  width: 2rem;
  height: 2rem;
  display: grid;
  place-items: center;
  border-radius: 0.65rem;
  background: linear-gradient(180deg, var(--brand-500), var(--brand-700));
  color: white;
  font-weight: 800;
  box-shadow: 0 8px 18px color-mix(in oklab, var(--brand-700) 18%, transparent);
}

.app-shell__brand-mark svg {
  width: 1.25rem;
  height: 1.25rem;
  fill: none;
  stroke: white;
  stroke-width: 1.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.app-shell__brand-copy {
  display: grid;
  gap: 0.05rem;
  min-width: 0;
}

.app-shell__brand-title {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.app-shell__brand-subtitle {
  font-size: 0.72rem;
  color: var(--text-muted);
  letter-spacing: 0.04em;
}

.app-shell__mainbar-right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 1rem;
  min-width: 0;
}

.app-shell__account-chip {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0.28rem 0.72rem;
  border-radius: 999px;
  border: 1px solid color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-surface) 96%, white);
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 600;
}

.app-shell__account-link {
  color: var(--text-secondary);
  font-size: 0.84rem;
  font-weight: 600;
}

.app-shell__account-link:hover {
  color: var(--brand-700);
}

.app-shell__divider {
  color: var(--text-muted);
}

.app-shell__nav {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.5rem;
}

.app-shell__nav-link {
  position: relative;
  min-height: 2.1rem;
  padding: 0.38rem 0.82rem;
  border-radius: 999px;
  color: var(--text-secondary);
  font-size: 0.88rem;
  font-weight: 600;
  border: 1px solid transparent;
  transition: color var(--transition-base), background var(--transition-base), border-color var(--transition-base), box-shadow var(--transition-base);
}

.app-shell__nav-link:hover {
  color: var(--text-primary);
  background: color-mix(in oklab, var(--brand-700) 4%, var(--bg-muted));
  border-color: color-mix(in oklab, var(--brand-700) 5%, var(--border-subtle));
}

.app-shell__nav-link.router-link-active {
  color: var(--brand-700);
  background: linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 97%, white), color-mix(in oklab, var(--bg-muted) 74%, white));
  border-color: color-mix(in oklab, var(--brand-700) 9%, var(--border-subtle));
  box-shadow:
    inset 0 1px 0 color-mix(in oklab, white 80%, transparent),
    0 8px 18px color-mix(in oklab, var(--brand-700) 5%, transparent);
}

.app-shell__lang-row {
  display: flex;
  gap: 0.35rem;
  padding-left: 0.2rem;
  border-left: 1px solid color-mix(in oklab, var(--brand-700) 5%, var(--border-subtle));
}

.app-shell__lang-button {
  min-width: 2.1rem;
  min-height: 1.95rem;
  border-radius: 999px;
  border: 1px solid color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-surface) 98%, white);
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 700;
}

.app-shell__lang-button--active {
  color: var(--brand-700);
  border-color: color-mix(in oklab, var(--brand-500) 20%, var(--border-strong));
  background: color-mix(in oklab, var(--bg-surface) 94%, white);
}

.app-shell__main {
  padding-top: 1.9rem;
  padding-bottom: 3rem;
}

@media (max-width: 860px) {
  .app-shell__utility,
  .app-shell__mainbar {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .app-shell__utility {
    display: grid;
  }

  .app-shell__utility-actions,
  .app-shell__mainbar-right {
    justify-content: flex-start;
  }

  .app-shell__mainbar-right {
    display: grid;
    gap: 0.8rem;
  }

  .app-shell__nav {
    justify-content: flex-start;
  }

  .app-shell__lang-row {
    padding-left: 0;
    border-left: 0;
  }
}
</style>
