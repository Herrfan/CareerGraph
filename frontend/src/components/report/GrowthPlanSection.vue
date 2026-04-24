<template>
  <CardPanel
    eyebrow="Action Plan"
    title="个性化阶段"
    description="这里会把更适合你当前阶段的后续建议整理出来，方便你一步一步往前走。"
  >
    <div v-if="actions.length === 0" class="growth-plan__empty text-secondary">
      生成行动计划后，这里会显示短期和中期的具体建议。
    </div>
    <div v-else>
      <div class="plan-tabs">
        <button 
          :class="['plan-tab', { 'plan-tab--active': activeTab === 'short' }]"
          @click="activeTab = 'short'"
        >
          短期计划
        </button>
        <button 
          :class="['plan-tab', { 'plan-tab--active': activeTab === 'mid' }]"
          @click="activeTab = 'mid'"
        >
          中期计划
        </button>
      </div>
      <div class="action-list">
        <article 
          v-for="(item, index) in filteredActions" 
          :key="`${item.source}-${index}`" 
          class="action-item"
        >
          <div class="action-item__topline">
            <span class="action-item__index">{{ index + 1 }}</span>
            <strong>{{ item.title }}</strong>
            <span class="badge">{{ item.sourceLabel }}</span>
          </div>
          <p v-if="item.detail" class="text-secondary">{{ item.detail }}</p>
          <p v-if="item.meta" class="action-item__meta">{{ item.meta }}</p>
        </article>
      </div>
    </div>
  </CardPanel>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { GrowthPlanResponse, GrowthPlanSection } from '@/types/api';
import CardPanel from '@/components/ui/CardPanel.vue';

const props = defineProps<{
  plan: GrowthPlanResponse | null;
}>();

const activeTab = ref<'short' | 'mid'>('short');

function resolveSection(section: GrowthPlanSection | undefined, fallbackSection: GrowthPlanSection | undefined) {
  return section ?? fallbackSection ?? {};
}

function toRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' ? (value as Record<string, unknown>) : {};
}

function readTitle(item: Record<string, unknown>, fallback: string) {
  return String(item.title ?? item.skill ?? item.topic ?? item.type ?? fallback);
}

function readDetail(item: Record<string, unknown>) {
  if (typeof item.detail === 'string' && item.detail.trim()) {
    return item.detail.trim();
  }
  if (typeof item.action === 'string' && item.action.trim()) {
    return item.action.trim();
  }
  if (Array.isArray(item.tasks) && item.tasks.length) {
    return item.tasks.map((task) => String(task)).join('；');
  }
  return '';
}

function readMeta(item: Record<string, unknown>) {
  const parts: string[] = [];
  if (typeof item.duration === 'string' && item.duration.trim()) {
    parts.push(`周期：${item.duration}`);
  }
  if (typeof item.timeline === 'string' && item.timeline.trim()) {
    parts.push(`时间：${item.timeline}`);
  }
  if (typeof item.deliverable === 'string' && item.deliverable.trim()) {
    parts.push(`产出：${item.deliverable}`);
  }
  if (Array.isArray(item.resources) && item.resources.length) {
    parts.push(`资源：${item.resources.map((resource) => String(resource)).join(' / ')}`);
  }
  return parts.join(' | ');
}

function dedupeActions(items: Array<{
  source: string;
  sourceLabel: string;
  title: string;
  detail: string;
  meta: string;
}>) {
  const seen = new Set<string>();
  return items.filter((item) => {
    const key = `${item.title} ${item.detail}`.toLowerCase().replace(/\s+/g, '');
    if (!key || seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}

const allActions = computed(() => {
  if (!props.plan) {
    return [];
  }

  const shortSection = resolveSection(props.plan.short_term_plan, props.plan.shortTermPlan);
  const midSection = resolveSection(props.plan.mid_term_plan, props.plan.midTermPlan);

  const shortLearningPath = shortSection.learning_path ?? shortSection.learningPath ?? [];
  const shortPracticeArrangements = shortSection.practice_arrangements ?? shortSection.practiceArrangements ?? [];
  const midLearningPath = midSection.learning_path ?? midSection.learningPath ?? [];
  const midPracticeArrangements = midSection.practice_arrangements ?? midSection.practiceArrangements ?? [];

  const shortLearningItems = shortLearningPath.map((value) => {
    const item = toRecord(value);
    return {
      source: 'short-learning',
      sourceLabel: '短期学习',
      title: readTitle(item, '学习任务'),
      detail: readDetail(item),
      meta: readMeta(item),
    };
  });

  const shortPracticeItems = shortPracticeArrangements.map((value) => {
    const item = toRecord(value);
    return {
      source: 'short-practice',
      sourceLabel: '短期实践',
      title: readTitle(item, '实践任务'),
      detail: readDetail(item),
      meta: readMeta(item),
    };
  });

  const midLearningItems = midLearningPath.map((value) => {
    const item = toRecord(value);
    return {
      source: 'mid-learning',
      sourceLabel: '中期学习',
      title: readTitle(item, '学习任务'),
      detail: readDetail(item),
      meta: readMeta(item),
    };
  });

  const midPracticeItems = midPracticeArrangements.map((value) => {
    const item = toRecord(value);
    return {
      source: 'mid-practice',
      sourceLabel: '中期实践',
      title: readTitle(item, '实践任务'),
      detail: readDetail(item),
      meta: readMeta(item),
    };
  });

  return dedupeActions([
    ...shortLearningItems,
    ...shortPracticeItems,
    ...midLearningItems,
    ...midPracticeItems,
  ]);
});

const filteredActions = computed(() => {
  return allActions.value.filter(item => {
    if (activeTab.value === 'short') {
      return item.source.startsWith('short-');
    } else {
      return item.source.startsWith('mid-');
    }
  });
});

const actions = computed(() => allActions.value);
</script>

<style scoped>
.growth-plan__empty {
  min-height: 120px;
  display: grid;
  align-items: center;
}

.plan-tabs {
  display: inline-flex;
  gap: 0.75rem;
  padding: 0.5rem;
  background: rgba(241, 245, 249, 0.8);
  border-radius: 16px;
  margin-bottom: 1.25rem;
}

.plan-tab {
  padding: 0.75rem 1.5rem;
  border: none;
  background: transparent;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s ease;
}

.plan-tab:hover {
  background: rgba(255, 255, 255, 0.8);
  color: #4f46e5;
}

.plan-tab--active {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: white;
  box-shadow: 0 4px 12px -4px rgba(79, 70, 229, 0.4);
}

.action-list {
  display: grid;
  gap: var(--space-3);
}

.action-item {
  display: grid;
  gap: 0.45rem;
  padding: 1.25rem;
  border-radius: 18px;
  background: linear-gradient(135deg, #ffffff, rgba(248, 250, 252, 0.95));
  border: 2px solid rgba(226, 232, 240, 0.9);
  transition: all 0.3s ease;
}

.action-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px -12px rgba(0, 0, 0, 0.1);
  border-color: rgba(79, 70, 229, 0.2);
}

.action-item__topline {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: var(--space-3);
  align-items: center;
}

.action-item__meta {
  margin: 0;
  font-size: 0.86rem;
  color: var(--text-muted);
}

.action-item p,
.action-item strong {
  margin: 0;
}

.action-item__index {
  width: 2rem;
  height: 2rem;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.15), rgba(79, 70, 229, 0.08));
  color: #4f46e5;
  font-size: 0.85rem;
  font-weight: 700;
  border: 1px solid rgba(79, 70, 229, 0.2);
}

.badge {
  padding: 0.4rem 0.8rem;
  border-radius: 50px;
  font-size: 0.8rem;
  font-weight: 600;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.12), rgba(79, 70, 229, 0.08));
  color: #4f46e5;
  border: 1px solid rgba(79, 70, 229, 0.2);
}
</style>
