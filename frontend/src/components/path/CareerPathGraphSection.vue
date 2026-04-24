<template>
  <div class="graph-layout">
    <CardPanel
      title="纵向发展历程"
      :description="highlightCurrent ? '从低到高展示岗位族的发展阶段，并重点标出当前更适合进入的具体阶段岗位。' : '从低到高展示岗位族的发展阶段。'"
    >
      <div v-if="verticalNodes.length" class="vertical-list">
        <article
          v-for="(node, index) in verticalNodes"
          :key="`vertical-${index}`"
          :class="['vertical-card', highlightCurrent && isCurrentNode(node, index) && 'vertical-card--current']"
        >
          <div class="vertical-card__header">
            <span class="eyebrow">{{ resolvePhase(node, index) }}</span>
            <div class="vertical-card__badges">
              <span v-if="highlightCurrent && isCurrentNode(node, index)" class="badge badge--focus">当前适合</span>
              <span class="badge">{{ resolveSalary(node) }}</span>
            </div>
          </div>
          <strong>{{ resolveTitle(node) }}</strong>
          <p class="text-secondary">{{ resolveText(node.jobDescription, '当前阶段暂无补充说明。') }}</p>
          <p class="vertical-card__relation">{{ resolveText(node.promotionRelation || node.reason, '暂无阶段说明。') }}</p>
          <div v-if="resolveSkills(node).length" class="tag-list">
            <span v-for="skill in resolveSkills(node)" :key="`${resolveTitle(node)}-${skill}`" class="badge">
              {{ skill }}
            </span>
          </div>
        </article>
      </div>
      <EmptyState
        v-else
        title="暂无纵向路径"
        description="当前还没有足够清晰的岗位族纵向发展信息。"
      />
    </CardPanel>

    <CardPanel
      v-if="showHorizontal"
      title="换岗方向"
      description="换岗建议会明确到更适合切入的目标阶段岗位，并说明为什么能转。"
    >
      <div v-if="switchGroups.length" class="switch-group-list">
        <article
          v-for="(group, index) in switchGroups"
          :key="`switch-${index}`"
          class="switch-group"
        >
          <div class="switch-group__header">
            <div>
              <p class="eyebrow">{{ group.relationshipType === 'current' ? '当前岗位族' : '关联岗位族' }}</p>
              <strong>{{ resolveTitle(group) }}</strong>
            </div>
            <span class="badge">{{ resolveSalary(group) }}</span>
          </div>
          <p class="text-secondary">{{ resolveText(group.jobDescription, '暂无补充说明。') }}</p>
          <p class="switch-group__reason">{{ resolveText(group.switchReason, '暂无换岗说明。') }}</p>

          <div class="switch-branches">
            <article
              v-for="(path, pathIndex) in resolvePaths(group)"
              :key="`path-${index}-${pathIndex}`"
              class="switch-branch"
            >
              <div class="switch-branch__header">
                <div>
                  <p class="eyebrow">{{ resolvePhase(path, pathIndex) }}</p>
                  <strong>{{ resolveTitle(path) }}</strong>
                </div>
                <span class="badge">{{ resolveSalary(path) }}</span>
              </div>
              <p class="text-secondary">{{ resolveText(path.jobDescription, '暂无补充说明。') }}</p>
              <p class="switch-branch__reason">{{ resolveText(path.switchReason, '暂无换岗说明。') }}</p>
            </article>
          </div>
        </article>
      </div>
      <EmptyState
        v-else
        title="暂无换岗路径"
        description="当前岗位族还没有额外的换岗建议。"
      />
    </CardPanel>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import CardPanel from '@/components/ui/CardPanel.vue';
import EmptyState from '@/components/ui/EmptyState.vue';
import type { CareerPathResponse } from '@/types/api';

const props = withDefaults(defineProps<{
  careerPath: CareerPathResponse | null;
  showHorizontal?: boolean;
  highlightCurrent?: boolean;
}>(), {
  showHorizontal: true,
  highlightCurrent: true,
});

type GenericRecord = Record<string, unknown>;

const verticalNodes = computed<GenericRecord[]>(() => (props.careerPath?.vertical_path as GenericRecord[]) || []);
const switchGroups = computed<GenericRecord[]>(() => (props.careerPath?.horizontal_path as GenericRecord[]) || []);

function resolveTitle(record: GenericRecord) {
  const candidates = [record.jobTitle, record.title, record.job_title, record.current];
  for (const item of candidates) {
    if (typeof item === 'string' && item.trim()) {
      return item.trim();
    }
  }
  return '未标注岗位';
}

function resolveSalary(record: GenericRecord) {
  const candidates = [record.referenceSalary, record.salaryRange, record.salary_range];
  for (const item of candidates) {
    if (typeof item === 'string' && item.trim()) {
      return item.trim();
    }
  }
  return '薪资面议';
}

function resolveText(value: unknown, fallback: string) {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback;
}

function resolveStage(record: GenericRecord, index: number) {
  const stage = Number(record.stage);
  if (Number.isFinite(stage) && stage > 0) {
    return Math.floor(stage);
  }
  return index + 1;
}

function resolvePhase(record: GenericRecord, index: number) {
  if (typeof record.phaseName === 'string' && record.phaseName.trim()) {
    return record.phaseName.trim();
  }
  return `阶段 ${resolveStage(record, index)}`;
}

function resolveSkills(record: GenericRecord) {
  if (!Array.isArray(record.requiredSkills)) {
    return [] as string[];
  }
  return record.requiredSkills
    .map((item) => String(item).trim())
    .filter(Boolean)
    .slice(0, 6);
}

function resolvePaths(record: GenericRecord) {
  if (!Array.isArray(record.paths)) {
    return [] as GenericRecord[];
  }
  return record.paths.filter((item): item is GenericRecord => !!item && typeof item === 'object');
}

function isCurrentNode(record: GenericRecord, index: number) {
  if (record.stageStatus === 'current') {
    return true;
  }
  if (record.stageStatus === 'entry' && index === 0) {
    return true;
  }
  return index === 0 && !verticalNodes.value.some((item) => item.stageStatus === 'current');
}
</script>

<style scoped>
.graph-layout {
  display: grid;
  gap: var(--space-5);
}

.vertical-list,
.switch-group-list {
  display: grid;
  gap: var(--space-4);
}

.vertical-card,
.switch-group {
  display: grid;
  gap: 0.65rem;
  padding: 1rem 1.05rem;
  border-radius: 14px;
  border: 1px solid color-mix(in oklab, var(--brand-500) 10%, var(--border-subtle));
  background: linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 99%, white), color-mix(in oklab, var(--bg-muted) 74%, white));
}

.vertical-card--current {
  border-color: color-mix(in oklab, #c56a19 40%, var(--brand-500));
  background: linear-gradient(180deg, color-mix(in oklab, #f8d9b8 34%, white), color-mix(in oklab, var(--bg-muted) 82%, white));
  box-shadow: 0 0 0 2px color-mix(in oklab, #c56a19 18%, transparent);
}

.vertical-card__header,
.switch-group__header,
.switch-branch__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.vertical-card__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  justify-content: flex-end;
}

.badge--focus {
  background: #c56a19;
  color: white;
}

.vertical-card strong,
.switch-group strong,
.switch-branch strong,
.vertical-card p,
.switch-group p,
.switch-branch p {
  margin: 0;
}

.vertical-card__relation,
.switch-group__reason,
.switch-branch__reason {
  color: var(--text-primary);
  line-height: 1.7;
}

.eyebrow {
  margin: 0;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.switch-branches {
  display: grid;
  gap: var(--space-3);
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.switch-branch {
  display: grid;
  gap: 0.55rem;
  padding: 0.9rem 0.95rem;
  border-radius: 12px;
  border: 1px solid color-mix(in oklab, var(--brand-500) 10%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-surface) 99%, white);
}

@media (max-width: 960px) {
  .switch-branches {
    grid-template-columns: 1fr;
  }
}
</style>
