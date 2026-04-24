<template>
  <AppShell>
    <section class="page-grid admin-page">
      <PageHeader
        eyebrow="Admin"
        title="企业岗位管理"
        description="管理员可以直接查看和修改导入后的岗位信息。"
      />

      <EmptyState
        v-if="!authStore.isAuthenticated || !authStore.isAdmin"
        title="需要管理员权限"
        description="请使用管理员账号登录后再进入这个页面。"
      />

      <template v-else>
        <div class="admin-layout">
          <CardPanel title="岗位列表" description="左侧选择一个岗位，右侧修改基础信息。">
            <div class="job-list">
              <button
                v-for="job in jobs"
                :key="job.sourceId"
                type="button"
                :class="['job-list__item', selectedJob?.sourceId === job.sourceId && 'job-list__item--active']"
                @click="selectJob(job)"
              >
                <strong>{{ job.jobTitle }}</strong>
                <p class="text-secondary">{{ job.companyName }} · {{ job.city || '未标注城市' }}</p>
              </button>
            </div>
          </CardPanel>

          <CardPanel title="岗位编辑" description="直接修改企业岗位信息并保存。">
            <EmptyState v-if="!selectedJob" title="请选择一个岗位" description="从左侧列表里选择一个岗位后开始编辑。" />
            <form v-else class="form-grid" @submit.prevent="handleSave">
              <div class="form-grid columns-2">
                <input v-model="form.companyName" class="input" placeholder="企业名称" />
                <input v-model="form.jobTitle" class="input" placeholder="岗位名称" />
              </div>
              <div class="form-grid columns-2">
                <input v-model="form.jobCategory" class="input" placeholder="岗位类别" />
                <input v-model="form.city" class="input" placeholder="城市" />
              </div>
              <div class="form-grid columns-2">
                <input v-model="form.salaryText" class="input" placeholder="薪资文本" />
                <input v-model="form.experienceText" class="input" placeholder="经验要求" />
              </div>
              <input v-model="form.educationLevel" class="input" placeholder="学历要求" />
              <input v-model="skillsText" class="input" placeholder="技能，使用中文逗号分隔" />
              <textarea v-model="form.jobDescription" class="textarea" rows="8" placeholder="岗位描述" />
              <div class="form-grid columns-2">
                <input v-model="form.industry" class="input" placeholder="行业" />
                <input v-model="form.status" class="input" placeholder="状态，例如 active / rejected" />
              </div>
              <p v-if="message" class="text-secondary">{{ message }}</p>
              <div class="admin-actions">
                <AppButton type="submit">保存修改</AppButton>
              </div>
            </form>
          </CardPanel>
        </div>
      </template>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import type { AdminJobRecord } from '@/types/api';
import AppShell from '@/components/layout/AppShell.vue';
import AppButton from '@/components/ui/AppButton.vue';
import CardPanel from '@/components/ui/CardPanel.vue';
import EmptyState from '@/components/ui/EmptyState.vue';
import PageHeader from '@/components/ui/PageHeader.vue';
import { getAdminJobs, updateAdminJob } from '@/services/admin';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const jobs = ref<AdminJobRecord[]>([]);
const selectedJob = ref<AdminJobRecord | null>(null);
const skillsText = ref('');
const message = ref('');
const form = reactive<AdminJobRecord>({
  sourceId: '',
  companyName: '',
  jobTitle: '',
  jobCategory: '',
  city: '',
  industry: '',
  salaryText: '',
  experienceText: '',
  educationLevel: '',
  jobDescription: '',
  skills: [],
  status: '',
});

function selectJob(job: AdminJobRecord) {
  selectedJob.value = job;
  form.sourceId = job.sourceId;
  form.companyName = job.companyName;
  form.jobTitle = job.jobTitle;
  form.jobCategory = job.jobCategory || '';
  form.city = job.city || '';
  form.industry = job.industry || '';
  form.salaryText = job.salaryText || '';
  form.experienceText = job.experienceText || '';
  form.educationLevel = job.educationLevel || '';
  form.jobDescription = job.jobDescription || '';
  form.status = job.status || '';
  skillsText.value = (job.skills || []).join('，');
}

async function loadJobs() {
  jobs.value = await getAdminJobs();
  if (!selectedJob.value && jobs.value.length > 0) {
    selectJob(jobs.value[0]);
  }
}

async function handleSave() {
  if (!selectedJob.value) {
    return;
  }

  const updated = await updateAdminJob(selectedJob.value.sourceId, {
    ...form,
    skills: skillsText.value.split(/[，,]/).map((item) => item.trim()).filter(Boolean),
  });
  message.value = '保存成功';
  await loadJobs();
  selectJob(updated);
}

onMounted(async () => {
  if (authStore.isAuthenticated && authStore.isAdmin) {
    await loadJobs();
  }
});
</script>

<style scoped>
.admin-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.8fr) minmax(0, 1.2fr);
  gap: var(--space-5);
}

.job-list {
  display: grid;
  gap: var(--space-3);
}

.job-list__item {
  display: grid;
  gap: 0.35rem;
  text-align: left;
  padding: 0.95rem 1rem;
  border-radius: 12px;
  border: 1px solid color-mix(in oklab, var(--brand-500) 10%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-surface) 98%, white);
}

.job-list__item p,
.job-list__item strong {
  margin: 0;
}

.job-list__item--active {
  border-color: color-mix(in oklab, var(--brand-500) 24%, var(--border-strong));
  background: color-mix(in oklab, var(--brand-500) 8%, white);
}

.admin-actions {
  display: flex;
  justify-content: flex-start;
}

@media (max-width: 1040px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }
}
</style>
