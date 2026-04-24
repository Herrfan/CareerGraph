<template>
  <div class="portrait-stack">
    <section class="overview-section">
      <p class="section-label">岗位概览</p>
      <dl class="overview-list">
        <div class="overview-row" v-for="item in overviewItems" :key="item.label">
          <dt>{{ item.label }}</dt>
          <dd>{{ item.value }}</dd>
        </div>
      </dl>
    </section>

    <section class="responsibility-section">
      <p class="section-label">岗位职责</p>
      <p class="responsibility-text text-secondary">{{ responsibilitiesText }}</p>
    </section>

    <div class="portrait-grid">
      <article v-for="item in dimensionItems" :key="item.key" class="portrait-card">
        <div class="portrait-card__header">
          <strong>{{ item.label }}</strong>
          <span class="badge">{{ item.priority }}</span>
        </div>
        <p class="text-secondary">{{ item.summary }}</p>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { JobProfile } from '@/types/api';

const props = defineProps<{
  job: JobProfile;
}>();

const order = [
  'professional_skills',
  'certificates',
  'innovation',
  'learning',
  'stress_tolerance',
  'communication',
  'internship',
  'teamwork',
  'execution',
  'problem_solving',
  'responsibility',
];

const labels: Record<string, string> = {
  professional_skills: '专业技能',
  certificates: '证书要求',
  innovation: '创新能力',
  learning: '学习能力',
  stress_tolerance: '抗压能力',
  communication: '沟通能力',
  internship: '实习/实践能力',
  teamwork: '团队协作',
  execution: '执行推进',
  problem_solving: '问题分析与解决',
  responsibility: '责任意识',
};

function normalizeRawText(value?: string) {
  if (!value) {
    return '';
  }
  return value
    .replace(/\r\n?/g, '\n')
    .replace(/[\u200B-\u200D\uFEFF]/g, '')
    .replace(/[ \t\u00A0]+/g, ' ')
    .replace(/\n{2,}/g, '\n')
    .trim();
}

function normalizeFragment(value: string) {
  return value
    .replace(/^[\-*•\s]+/, '')
    .replace(/[：:\s]+$/g, '')
    .trim();
}

function splitAndCleanFragments(value: string, maxCount = 10) {
  const fragments = normalizeRawText(value)
    .split(/[\n；。]+/)
    .map(normalizeFragment)
    .filter(Boolean);

  const unique = new Set<string>();
  const deduped: string[] = [];
  for (const item of fragments) {
    const key = item.toLowerCase().replace(/[^\u4e00-\u9fa5a-z0-9]/g, '');
    if (!key || unique.has(key)) {
      continue;
    }
    unique.add(key);
    deduped.push(item);
    if (deduped.length >= maxCount) {
      break;
    }
  }
  return deduped;
}

function cleanOverviewValue(value?: string, defaultValue = '未标注') {
  const normalized = normalizeRawText(value);
  return normalized || defaultValue;
}

function cleanLongField(value?: string, defaultValue = '未标注', maxCount = 4) {
  const fragments = splitAndCleanFragments(value || '', maxCount);
  return fragments.length ? fragments.join('；\n') : defaultValue;
}

function buildResponsibilitiesText(job: JobProfile) {
  const portrait = job.ability_portrait ?? {};
  const portraitText = normalizeRawText((portrait as Record<string, string>).job_responsibilities);
  if (portraitText) {
    const candidates = splitAndCleanFragments(portraitText, 5);
    if (candidates.length) {
      return candidates.join('；\n');
    }
  }

  const description = normalizeRawText(job.description);
  if (!description) {
    return '岗位职责未标注。';
  }

  const candidates = splitAndCleanFragments(description, 5);
  return candidates.length ? candidates.join('；\n') : '岗位职责未标注。';
}

const overviewItems = computed(() => [
  { label: '岗位名称', value: cleanOverviewValue(props.job.title) },
  { label: '代表薪资', value: cleanOverviewValue(props.job.salary_range || props.job.salary_band) },
  { label: '城市层级', value: cleanOverviewValue(props.job.city_tier || props.job.city) },
  { label: '行业', value: cleanOverviewValue(props.job.industry || props.job.department) },
]);

const responsibilitiesText = computed(() => buildResponsibilitiesText(props.job));

const dimensionItems = computed(() => {
  const portrait = props.job.ability_portrait ?? {};
  const priority = props.job.ability_priority ?? {};
  return order.map((key) => ({
    key,
    label: labels[key] ?? key,
    summary: cleanLongField((portrait as Record<string, string>)[key], '当前岗位样本未提供该维度的画像说明。', 3),
    priority: `${(priority as Record<string, number>)[key] ?? 60}分`,
  }));
});
</script>

<style scoped>
.portrait-stack {
  display: grid;
  gap: var(--space-4);
}

.overview-section,
.responsibility-section {
  padding: 1rem 1.05rem;
  border: 1px solid color-mix(in oklab, var(--brand-500) 12%, var(--border-subtle));
  border-radius: 14px;
  background: linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 98%, white), color-mix(in oklab, var(--bg-muted) 74%, white));
}

.section-label {
  margin: 0 0 0.75rem;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.overview-list {
  margin: 0;
  display: grid;
  gap: 0.7rem;
}

.overview-row {
  display: grid;
  gap: 0.18rem;
  padding-bottom: 0.7rem;
  border-bottom: 1px solid color-mix(in oklab, var(--brand-500) 10%, var(--border-subtle));
}

.overview-row:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.overview-row dt,
.overview-row dd,
.responsibility-text {
  margin: 0;
}

.overview-row dt {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--text-muted);
}

.overview-row dd,
.responsibility-text {
  line-height: 1.75;
  white-space: pre-line;
  word-break: break-word;
}

.portrait-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.portrait-card {
  display: grid;
  gap: 0.55rem;
  padding: 0.95rem 1rem;
  border: 1px solid color-mix(in oklab, var(--brand-500) 10%, var(--border-subtle));
  border-radius: 12px;
  background: color-mix(in oklab, var(--bg-surface) 99%, white);
}

.portrait-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.portrait-card p,
.portrait-card strong {
  margin: 0;
}

.portrait-card p {
  white-space: pre-line;
  line-height: 1.75;
}

@media (max-width: 900px) {
  .portrait-grid {
    grid-template-columns: 1fr;
  }
}
</style>
