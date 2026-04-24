<template>
  <AppShell>
    <section class="page-grid history-page">
      <PageHeader
        eyebrow="History"
        title="我的记录"
        description="登录用户可以查看当前账号下保存的画像历史、报告历史和对话记录。"
      />

      <EmptyState
        v-if="!authStore.isAuthenticated"
        title="请先登录"
        description="游客模式不保存历史记录。登录后才能查看当前账号下的画像、报告和对话记忆。"
      >
        <template #actions>
          <AppButton @click="router.push('/auth')">前往登录</AppButton>
        </template>
      </EmptyState>

      <template v-else>
        <div class="history-layout">
          <CardPanel title="历史画像" description="这里展示当前账号保存过的学生画像快照。">
            <div v-if="profiles.length" class="history-list">
              <button
                v-for="profile in profiles"
                :key="profile.snapshot_id"
                type="button"
                class="history-item history-item--interactive"
                @click="openProfileSnapshot(profile)"
              >
                <strong>{{ profile.profile_data.basic_info.name || '未命名画像' }}</strong>
                <p class="text-secondary">
                  {{ profile.profile_data.basic_info.school }} · {{ profile.profile_data.basic_info.major }}
                </p>
                <p class="history-meta">{{ formatTime(profile.created_at) }}</p>
                <span class="history-action">点击恢复到画像页</span>
              </button>
            </div>
            <EmptyState v-else title="暂无画像历史" description="登录后保存学生画像，这里会自动出现记录。" />
          </CardPanel>

          <CardPanel title="历史报告" description="这里展示当前账号保存过的报告快照。">
            <div v-if="reports.length" class="history-list">
              <button
                v-for="report in reports"
                :key="report.snapshot_id"
                type="button"
                class="history-item history-item--interactive"
                @click="openReportSnapshot(report)"
              >
                <strong>{{ report.target_job }}</strong>
                <p class="text-secondary">{{ report.matched_job_title || '未记录匹配岗位' }}</p>
                <p class="history-meta">{{ formatTime(report.updated_at) }}</p>
                <span class="history-action">点击恢复到报告页</span>
              </button>
            </div>
            <EmptyState v-else title="暂无报告历史" description="生成报告或成长计划后，会自动保存到账户历史中。" />
          </CardPanel>

          <CardPanel title="对话记录" description="这里只展示当前登录账号下最近的智能体对话。">
            <div v-if="chats.length" class="history-list">
              <article v-for="chat in chats" :key="`${chat.conversationKey}-${chat.createdAt}-${chat.role}`" class="history-item">
                <strong>{{ chat.role === 'assistant' ? '智能体回复' : '用户提问' }}</strong>
                <p class="text-secondary">{{ chat.content }}</p>
                <p class="history-meta">{{ formatTime(chat.createdAt) }}</p>
              </article>
            </div>
            <EmptyState v-else title="暂无对话记录" description="登录后进行智能体对话，这里会显示最近记录。" />
          </CardPanel>
        </div>
      </template>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { ChatMessageDTO, ReportSnapshot, StudentProfileSnapshot } from '@/types/api';
import AppShell from '@/components/layout/AppShell.vue';
import AppButton from '@/components/ui/AppButton.vue';
import CardPanel from '@/components/ui/CardPanel.vue';
import EmptyState from '@/components/ui/EmptyState.vue';
import PageHeader from '@/components/ui/PageHeader.vue';
import { getMyHistory } from '@/services/history';
import { useAuthStore } from '@/stores/auth';
import { useProfileStore } from '@/stores/profile';
import { useReportStore } from '@/stores/report';

const router = useRouter();
const authStore = useAuthStore();
const profileStore = useProfileStore();
const reportStore = useReportStore();
const profiles = ref<StudentProfileSnapshot[]>([]);
const reports = ref<ReportSnapshot[]>([]);
const chats = ref<ChatMessageDTO[]>([]);

async function loadHistory() {
  if (!authStore.isAuthenticated) {
    profiles.value = [];
    reports.value = [];
    chats.value = [];
    return;
  }

  const data = await getMyHistory();
  profiles.value = data.profiles ?? [];
  reports.value = data.reports ?? [];
  chats.value = data.chats ?? [];
}

function formatTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function openProfileSnapshot(snapshot: StudentProfileSnapshot) {
  profileStore.setProfile(snapshot.profile_data);
  router.push('/profile');
}

async function openReportSnapshot(snapshot: ReportSnapshot) {
  reportStore.applySnapshot(snapshot);
  if (!profileStore.studentProfile) {
    await profileStore.loadMyProfile();
  }
  router.push({
    path: '/report',
    query: {
      history: '1',
      snapshot: snapshot.snapshot_id,
    },
  });
}

onMounted(loadHistory);
</script>

<style scoped>
.history-layout {
  display: grid;
  gap: var(--space-5);
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.history-list {
  display: grid;
  gap: var(--space-3);
}

.history-item {
  display: grid;
  gap: 0.45rem;
  padding: 1rem 1.05rem;
  text-align: left;
  border-radius: 12px;
  border: 1px solid color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  background: linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 99%, white), color-mix(in oklab, var(--bg-muted) 74%, white));
}

.history-item p,
.history-item strong {
  margin: 0;
}

.history-meta {
  font-size: 0.84rem;
  color: var(--text-muted);
}

.history-action {
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--brand-700);
}

.history-item--interactive {
  cursor: pointer;
  transition: transform 140ms ease, border-color 140ms ease, box-shadow 140ms ease;
}

.history-item--interactive:hover {
  transform: translateY(-1px);
  border-color: color-mix(in oklab, var(--brand-500) 18%, var(--border-strong));
  box-shadow: 0 12px 22px color-mix(in oklab, var(--brand-700) 7%, transparent);
}

@media (max-width: 1120px) {
  .history-layout {
    grid-template-columns: 1fr;
  }
}
</style>
