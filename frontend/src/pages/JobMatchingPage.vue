<template>
  <AppShell>
    <section class="page-grid matching-page">
      <PageHeader
        eyebrow="Match"
        title="岗位匹配"
        description="这里会结合你的学生画像，判断你目前更适合先从哪个岗位方向切入，并给出更适合的起步阶段。"
      >
        <template #actions>
          <StepGuide
            storage-key="job-matching"
            title="先看懂匹配结果，再决定后续走向"
            description="岗位匹配不只是看分数高低，还会决定后续职业路径和职业报告围绕哪个方向展开。"
            trigger-eyebrow="Next Step"
            trigger-title="岗位匹配说明"
            trigger-summary="看看分数该怎么理解，以及当前选择会如何影响后续页面。"
            note="如果你从这里进入职业路径或职业报告，系统会把当前选中的岗位方向当成目标继续分析。换方向后，后面的结果也会跟着变化。"
            :sections="matchingGuideSections"
          />
        </template>
      </PageHeader>

      <MockPreviewOverlay
        v-if="!profileStore.studentProfile"
        title="上传简历，找到真正适合你的岗位机会"
        @upload="router.push('/profile')"
      >
        <CardPanel
          eyebrow="Profile"
          title="当前匹配基线（示例）"
          description="先看当前画像和求职偏好，再理解下面的方向排序与阶段判断。"
          class="profile-baseline-card"
        >
          <div class="profile-summary">
            <div class="profile-avatar">
              <span class="avatar-icon">👨‍🎓</span>
            </div>
            <div class="profile-info">
              <h3 class="profile-name">张三</h3>
              <p class="profile-detail">浙江工业大学 | 软件工程 | 本科</p>
            </div>
            <div class="profile-tags">
              <span class="tag-chip">前端开发</span>
              <span class="tag-chip">技能 6</span>
            </div>
          </div>

          <div class="profile-details-grid">
            <div class="detail-card">
              <div class="detail-title">
                <span class="detail-icon">🛠️</span>
                <span>技能标签</span>
              </div>
              <div class="tag-list">
                <span class="skill-tag">Vue</span>
                <span class="skill-tag">TypeScript</span>
                <span class="skill-tag">React</span>
                <span class="skill-tag">HTML/CSS</span>
                <span class="skill-tag">Node.js</span>
              </div>
            </div>
            <div class="detail-card">
              <div class="detail-title">
                <span class="detail-icon">🎯</span>
                <span>求职偏好</span>
              </div>
              <div class="preference-list">
                <div class="preference-item">
                  <span class="pref-label">岗位:</span>
                  <span class="pref-value">前端开发</span>
                </div>
                <div class="preference-item">
                  <span class="pref-label">薪资:</span>
                  <span class="pref-value">8K-12K</span>
                </div>
                <div class="preference-item">
                  <span class="pref-label">城市:</span>
                  <span class="pref-value">杭州</span>
                </div>
              </div>
            </div>
          </div>
        </CardPanel>

        <div class="metric-grid">
          <StatCard
            label="当前优先方向"
            value="前端开发"
            hint="会优先展示当前更适合先进入的岗位方向。"
            class="metric-card metric-card--primary"
          />
          <StatCard
            label="更适合起步阶段"
            value="前端开发实习生"
            hint="会落到岗位方向里的具体阶段，方便你判断应该从哪里开始。"
            class="metric-card"
          />
          <StatCard
            label="当前匹配层级"
            value="优先推荐"
            hint="高匹配优先考虑，中低匹配先当作探索方向，不再一律显示为推荐。"
            class="metric-card"
          />
        </div>

        <div class="matching-layout">
          <CardPanel
            class="matching-col matching-col--categories"
            title="岗位方向排序（示例）"
            description="先看你和哪些方向更接近，再决定把精力重点投向哪里。"
          >
            <ul class="list-reset category-list">
              <li class="category-item category-item--active">
                <div class="category-item__header">
                  <div class="category-info">
                    <div class="category-rank">1</div>
                    <div>
                      <strong class="category-name">前端开发</strong>
                      <span class="category-badge category-badge--high">优先推荐</span>
                    </div>
                  </div>
                  <div class="match-score">
                    <div class="score-bar">
                      <div class="score-fill score-fill--high" style="width: 78%"></div>
                    </div>
                    <span class="score-number">78%</span>
                  </div>
                </div>
                <p class="category-tip">当前画像与这个方向已经比较贴近，适合优先深入了解。</p>
              </li>
              <li class="category-item">
                <div class="category-item__header">
                  <div class="category-info">
                    <div class="category-rank">2</div>
                    <div>
                      <strong class="category-name">Java开发</strong>
                      <span class="category-badge category-badge--medium">可以尝试</span>
                    </div>
                  </div>
                  <div class="match-score">
                    <div class="score-bar">
                      <div class="score-fill score-fill--medium" style="width: 56%"></div>
                    </div>
                    <span class="score-number">56%</span>
                  </div>
                </div>
                <p class="category-tip">有一定匹配基础，建议再结合兴趣和岗位画像进一步确认。</p>
              </li>
              <li class="category-item">
                <div class="category-item__header">
                  <div class="category-info">
                    <div class="category-rank">3</div>
                    <div>
                      <strong class="category-name">软件测试</strong>
                      <span class="category-badge category-badge--low">暂作参考</span>
                    </div>
                  </div>
                  <div class="match-score">
                    <div class="score-bar">
                      <div class="score-fill score-fill--low" style="width: 32%"></div>
                    </div>
                    <span class="score-number">32%</span>
                  </div>
                </div>
                <p class="category-tip">当前差距还比较明显，更适合作为备选方向而不是直接主推。</p>
              </li>
            </ul>
          </CardPanel>

          <CardPanel
            class="matching-col matching-col--result"
            title="当前更适合从哪里起步（示例）"
            description="这里会重点告诉你：现在更适合先从哪个阶段开始，以及为什么是这个阶段。"
          >
            <section class="stage-panel">
              <div class="stage-icon">🚀</div>
              <div class="stage-content">
                <p class="stage-label">当前更适合进入</p>
                <strong class="stage-title">前端开发实习生</strong>
                <p class="stage-desc">基于你的技能和经验，当前更适合从实习生阶段开始，可以在实际项目中积累前端开发经验。</p>
              </div>
            </section>
          </CardPanel>

          <CardPanel
            class="matching-col matching-col--portrait"
            title="当前方向画像（示例）"
            description="这里会帮你快速理解这个方向通常看重什么能力，以及你接下来最适合沿着哪个目标继续看。"
          >
            <div class="portrait-placeholder">
              <p class="text-secondary">这里会展示岗位画像内容...</p>
            </div>

            <section class="selection-impact">
              <div class="impact-icon">📌</div>
              <div class="impact-content">
                <p class="impact-label">后续会沿用这个目标</p>
                <strong class="impact-title">前端开发</strong>
                <p class="impact-desc">
                  如果你现在进入职业路径或职业报告，系统会按这个岗位方向继续分析。想换方向，可以先切换左侧结果。
                </p>
              </div>
            </section>

            <div class="portrait-actions">
              <AppButton variant="secondary" @click="router.push('/career-path')" class="action-btn">
                <span>查看职业路径</span>
                <span class="btn-arrow">→</span>
              </AppButton>
              <AppButton @click="router.push('/report')" class="action-btn action-btn--primary">
                <span>生成职业报告</span>
                <span class="btn-arrow">→</span>
              </AppButton>
            </div>
          </CardPanel>
        </div>
      </MockPreviewOverlay>

      <template v-else>
        <CardPanel
          eyebrow="Profile"
          title="当前匹配基线"
          description="先看当前画像和求职偏好，再理解下面的方向排序与阶段判断。"
          class="profile-baseline-card"
        >
          <div class="profile-summary">
            <div class="profile-avatar">
              <span class="avatar-icon">👨‍🎓</span>
            </div>
            <div class="profile-info">
              <h3 class="profile-name">{{ profileStore.studentProfile.basic_info.name || '未填写姓名' }}</h3>
              <p class="profile-detail">
                {{ profileStore.studentProfile.basic_info.school || '学校未填' }} |
                {{ profileStore.studentProfile.basic_info.major || '专业未填' }}
              </p>
            </div>
            <div class="profile-tags">
              <span class="tag-chip">{{ profileStore.studentProfile.job_preference.expected_position || '岗位待确认' }}</span>
              <span class="tag-chip">技能 {{ profileStore.studentProfile.skills.length }}</span>
            </div>
          </div>

          <div class="profile-details-grid">
            <div class="detail-card">
              <div class="detail-title">
                <span class="detail-icon">🛠️</span>
                <span>技能标签</span>
              </div>
              <div class="tag-list">
                <span v-for="skill in profileStore.studentProfile.skills.slice(0, 10)" :key="skill" class="skill-tag">{{ skill }}</span>
                <span v-if="profileStore.studentProfile.skills.length === 0" class="text-secondary">暂时还没有明确技能</span>
              </div>
            </div>
            <div class="detail-card">
              <div class="detail-title">
                <span class="detail-icon">🎯</span>
                <span>求职偏好</span>
              </div>
              <div class="preference-list">
                <div class="preference-item">
                  <span class="pref-label">岗位:</span>
                  <span class="pref-value">{{ profileStore.studentProfile.job_preference.expected_position || '未指定' }}</span>
                </div>
                <div class="preference-item">
                  <span class="pref-label">薪资:</span>
                  <span class="pref-value">{{ profileStore.studentProfile.job_preference.expected_salary || '未指定' }}</span>
                </div>
                <div class="preference-item">
                  <span class="pref-label">城市:</span>
                  <span class="pref-value">{{ profileStore.studentProfile.job_preference.expected_city || '未指定' }}</span>
                </div>
              </div>
            </div>
          </div>
        </CardPanel>

        <div class="metric-grid">
          <StatCard
            label="当前优先方向"
            :value="matchingStore.selectedCategory || '等待计算'"
            hint="会优先展示当前更适合先进入的岗位方向。"
            class="metric-card metric-card--primary"
          />
          <StatCard
            label="更适合起步阶段"
            :value="currentStageTitle || '等待判断'"
            hint="会落到岗位方向里的具体阶段，方便你判断应该从哪里开始。"
            class="metric-card"
          />
          <StatCard
            label="当前匹配层级"
            :value="selectedRecommendationLabel"
            hint="高匹配优先考虑，中低匹配先当作探索方向，不再一律显示为推荐。"
            class="metric-card"
          />
        </div>

        <p v-if="matchingStore.errorMessage" class="matching-error">{{ matchingStore.errorMessage }}</p>
        <LoadingBlock v-if="matchingStore.isLoadingCategories" label="正在计算岗位匹配..." />

        <div v-else class="matching-layout">
          <CardPanel
            class="matching-col matching-col--categories"
            title="岗位方向排序"
            description="先看你和哪些方向更接近，再决定把精力重点投向哪里。"
          >
            <EmptyState
              v-if="matchingStore.categories.length === 0"
              title="暂时还没有匹配结果"
              description="可以先补充学生画像里的技能、证书或求职偏好，再回来刷新。"
            />

            <ul v-else class="list-reset category-list">
              <li
                v-for="(category, index) in matchingStore.categories"
                :key="category.category"
                :class="['category-item', matchingStore.selectedCategory === category.category && 'category-item--active']"
                @click="handleSelectCategory(category.category)"
              >
                <div class="category-item__header">
                  <div class="category-info">
                    <div class="category-rank">{{ index + 1 }}</div>
                    <div>
                      <strong class="category-name">{{ category.category }}</strong>
                      <span :class="['category-badge', getBadgeClass(category.match_score)]">{{ getCategoryBadge(category.match_score).label }}</span>
                    </div>
                  </div>
                  <div class="match-score">
                    <div class="score-bar">
                      <div :class="['score-fill', getScoreFillClass(category.match_score)]" :style="{ width: `${category.match_score}%` }"></div>
                    </div>
                    <span class="score-number">{{ category.match_score.toFixed(0) }}%</span>
                  </div>
                </div>
                <p class="category-tip">{{ getCategoryBadge(category.match_score).tip }}</p>
              </li>
            </ul>
          </CardPanel>

          <CardPanel
            class="matching-col matching-col--result"
            title="当前更适合从哪里起步"
            description="这里会重点告诉你：现在更适合先从哪个阶段开始，以及为什么是这个阶段。"
          >
            <LoadingBlock v-if="matchingStore.isLoadingJobs || detailLoading" label="正在判断当前阶段..." />
            <EmptyState
              v-else-if="!careerPath"
              title="暂无路径结果"
              description="这个方向的成长路径暂时还没整理出来，可以先看看岗位画像。"
            />
            <template v-else>
              <section v-if="currentStageTitle" class="stage-panel">
                <div class="stage-icon">🚀</div>
                <div class="stage-content">
                  <p class="stage-label">当前更适合进入</p>
                  <strong class="stage-title">{{ currentStageTitle }}</strong>
                  <p class="stage-desc">{{ currentStageReason || '系统会根据你的画像，判断更适合从哪个阶段起步。' }}</p>
                </div>
              </section>

              <CareerPathGraphSection
                :career-path="careerPath"
                :show-horizontal="false"
                :highlight-current="true"
              />
            </template>
          </CardPanel>

          <CardPanel
            class="matching-col matching-col--portrait"
            title="当前方向画像"
            description="这里会帮你快速理解这个方向通常看重什么能力，以及你接下来最适合沿着哪个目标继续看。"
          >
            <EmptyState
              v-if="!matchingStore.selectedJob"
              title="还没有选中岗位方向"
              description="先从左侧选一个方向，这里会同步展开对应画像。"
            />
            <template v-else>
              <JobPortraitPanel :job="matchingStore.selectedJob.job" />

              <section class="selection-impact">
                <div class="impact-icon">📌</div>
                <div class="impact-content">
                  <p class="impact-label">后续会沿用这个目标</p>
                  <strong class="impact-title">{{ matchingStore.selectedJob.job.title }}</strong>
                  <p class="impact-desc">
                    如果你现在进入职业路径或职业报告，系统会按这个岗位方向继续分析。想换方向，可以先切换左侧结果。
                  </p>
                </div>
              </section>

              <div class="portrait-actions">
                <AppButton variant="secondary" @click="openPath" class="action-btn">
                  <span>查看职业路径</span>
                  <span class="btn-arrow">→</span>
                </AppButton>
                <AppButton @click="openReport" class="action-btn action-btn--primary">
                  <span>生成职业报告</span>
                  <span class="btn-arrow">→</span>
                </AppButton>
              </div>
            </template>
          </CardPanel>
        </div>
      </template>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { CareerPathResponse } from '@/types/api';
import AppShell from '@/components/layout/AppShell.vue';
import JobPortraitPanel from '@/components/job/JobPortraitPanel.vue';
import CareerPathGraphSection from '@/components/path/CareerPathGraphSection.vue';
import AppButton from '@/components/ui/AppButton.vue';
import CardPanel from '@/components/ui/CardPanel.vue';
import EmptyState from '@/components/ui/EmptyState.vue';
import LoadingBlock from '@/components/ui/LoadingBlock.vue';
import PageHeader from '@/components/ui/PageHeader.vue';
import StatCard from '@/components/ui/StatCard.vue';
import StepGuide from '@/components/ui/StepGuide.vue';
import MockPreviewOverlay from '@/components/ui/MockPreviewOverlay.vue';
import { generateCareerPath } from '@/services/retrieve';
import { useMatchingStore } from '@/stores/matching';
import { useProfileStore } from '@/stores/profile';
import { useReportStore } from '@/stores/report';

type MatchBadge = {
  label: string;
  tip: string;
  tone: 'strong' | 'medium' | 'weak';
};

const matchingGuideSections = [
  {
    title: '先看匹配层级，不要只看有没有"推荐"',
    description: '70% 以上会作为优先推荐，45% 到 69% 更适合进一步了解，45% 以下只作参考，不建议直接把低匹配结果当成最终目标。',
    tone: 'accent',
  },
  {
    title: '当前选中的方向会带到后续页面',
    description: '你从这里进入职业路径或职业报告时，系统会默认沿用当前选中的岗位方向，后续分析也会围绕这个目标展开。',
    tone: 'warning',
  },
  {
    title: '结果不理想时，优先回去补充画像',
    description: '如果你发现匹配方向偏了，通常先补充技能、证书、期望岗位、薪资或城市，比直接硬看报告更有效。',
  },
] as const;

const router = useRouter();
const profileStore = useProfileStore();
const matchingStore = useMatchingStore();
const reportStore = useReportStore();
const detailLoading = ref(false);
const careerPath = ref<CareerPathResponse | null>(null);

const currentStage = computed(() =>
  (careerPath.value?.vertical_path || []).find((item) => String(item.stageStatus || '') === 'current')
    ?? careerPath.value?.vertical_path?.[0]
    ?? null,
);
const currentStageTitle = computed(() => currentStage.value ? String(currentStage.value.jobTitle || '') : '');
const currentStageReason = computed(() => currentStage.value ? String(currentStage.value.reason || currentStage.value.promotionRelation || '') : '');
const selectedRecommendationLabel = computed(() =>
  matchingStore.selectedJob ? getCategoryBadge(matchingStore.selectedJob.match_score).label : '等待判断',
);

function getCategoryBadge(score: number): MatchBadge {
  if (score >= 70) {
    return {
      label: '优先推荐',
      tip: '当前画像与这个方向已经比较贴近，适合优先深入了解。',
      tone: 'strong',
    };
  }

  if (score >= 45) {
    return {
      label: '可以尝试',
      tip: '有一定匹配基础，建议再结合兴趣和岗位画像进一步确认。',
      tone: 'medium',
    };
  }

  return {
    label: '暂作参考',
    tip: '当前差距还比较明显，更适合作为备选方向而不是直接主推。',
    tone: 'weak',
  };
}

function getBadgeClass(score: number): string {
  if (score >= 70) return 'category-badge--high';
  if (score >= 45) return 'category-badge--medium';
  return 'category-badge--low';
}

function getScoreFillClass(score: number): string {
  if (score >= 70) return 'score-fill--high';
  if (score >= 45) return 'score-fill--medium';
  return 'score-fill--low';
}

async function bootstrap() {
  if (!profileStore.studentProfile) {
    return;
  }

  await matchingStore.loadMatchingOverview(profileStore.studentProfile);
  await loadSelectedDetail();
}

async function handleSelectCategory(category: string) {
  if (!profileStore.studentProfile) {
    return;
  }

  await matchingStore.selectCategory(profileStore.studentProfile, category);
  await loadSelectedDetail();
}

async function loadSelectedDetail() {
  if (!matchingStore.selectedJob || !profileStore.studentProfile) {
    careerPath.value = null;
    return;
  }

  detailLoading.value = true;
  try {
    careerPath.value = await generateCareerPath(matchingStore.selectedJob.job.title, profileStore.studentProfile);
  } catch {
    careerPath.value = null;
  } finally {
    detailLoading.value = false;
  }
}

function openPath() {
  if (!matchingStore.selectedJob) {
    return;
  }

  reportStore.setTargetContext(matchingStore.selectedJob.job.title, matchingStore.selectedJob);
  void router.push('/career-path');
}

function openReport() {
  if (!matchingStore.selectedJob) {
    return;
  }

  reportStore.setTargetContext(matchingStore.selectedJob.job.title, matchingStore.selectedJob);
  void router.push('/report');
}

onMounted(() => {
  void bootstrap();
});
</script>

<style scoped>
.matching-page {
  gap: var(--space-5);
}

.profile-baseline-card {
  background:
    linear-gradient(135deg, rgba(16, 185, 129, 0.04) 0%, rgba(59, 130, 246, 0.03) 100%),
    white;
}

.profile-summary {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  padding: 1.25rem;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 250, 252, 0.8));
  border-radius: 16px;
  margin-bottom: 1.25rem;
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4F46E5, #6366F1);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px -8px rgba(79, 70, 229, 0.3);
}

.avatar-icon {
  font-size: 2.25rem;
}

.profile-info {
  flex: 1;
}

.profile-name {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #1E293B;
}

.profile-detail {
  margin: 0.35rem 0 0;
  color: #64748B;
  font-size: 0.95rem;
}

.profile-tags {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.tag-chip {
  padding: 0.4rem 0.9rem;
  border-radius: 50px;
  font-size: 0.875rem;
  font-weight: 500;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.1), rgba(99, 102, 241, 0.08));
  color: #4F46E5;
  border: 1px solid rgba(79, 70, 229, 0.2);
}

.profile-details-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.25rem;
}

.detail-card {
  padding: 1.25rem;
  border-radius: 16px;
  background: linear-gradient(135deg, #FFFFFF, #F8FAFC);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.85rem;
  font-weight: 600;
  color: #1E293B;
  font-size: 0.95rem;
}

.detail-icon {
  font-size: 1.25rem;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.skill-tag {
  padding: 0.35rem 0.75rem;
  border-radius: 50px;
  font-size: 0.85rem;
  background: linear-gradient(135deg, #F1F5F9, #E2E8F0);
  color: #475569;
  font-weight: 500;
}

.preference-list {
  display: grid;
  gap: 0.6rem;
}

.preference-item {
  display: flex;
  gap: 0.5rem;
}

.pref-label {
  color: #64748B;
  font-weight: 500;
  min-width: 48px;
}

.pref-value {
  color: #1E293B;
  font-weight: 600;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
}

.metric-card {
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-4px);
}

.metric-card--primary :deep(.card-panel) {
  background:
    linear-gradient(135deg, rgba(79, 70, 229, 0.06) 0%, rgba(99, 102, 241, 0.04) 100%),
    white;
  border: 2px solid rgba(79, 70, 229, 0.2);
}

.matching-layout {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.25fr) minmax(0, 1.1fr);
  gap: var(--space-5);
  align-items: start;
}

.matching-col--portrait {
  position: sticky;
  top: 6.2rem;
}

.category-list {
  display: grid;
  gap: 1rem;
}

.category-item {
  padding: 1.15rem;
  border-radius: 16px;
  border: 2px solid rgba(226, 232, 240, 0.8);
  background: linear-gradient(135deg, #FFFFFF, #F8FAFC);
  cursor: pointer;
  transition: all 0.3s ease;
}

.category-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px -12px rgba(0, 0, 0, 0.12);
  border-color: rgba(79, 70, 229, 0.3);
}

.category-item--active {
  border-color: rgba(79, 70, 229, 0.5);
  background:
    linear-gradient(135deg, rgba(79, 70, 229, 0.06) 0%, rgba(99, 102, 241, 0.04) 100%),
    white;
  box-shadow: 0 12px 32px -12px rgba(79, 70, 229, 0.25);
}

.category-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.category-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.category-rank {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #E2E8F0, #CBD5E1);
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.9rem;
}

.category-item--active .category-rank,
.category-item:first-child .category-rank {
  background: linear-gradient(135deg, #4F46E5, #6366F1);
  color: white;
  box-shadow: 0 4px 12px -4px rgba(79, 70, 229, 0.4);
}

.category-name {
  display: block;
  font-size: 1.05rem;
  color: #1E293B;
  margin-bottom: 0.25rem;
}

.category-badge {
  display: inline-block;
  padding: 0.25rem 0.6rem;
  border-radius: 50px;
  font-size: 0.78rem;
  font-weight: 600;
}

.category-badge--high {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(16, 185, 129, 0.1));
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.category-badge--medium {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(245, 158, 11, 0.1));
  color: #B45309;
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.category-badge--low {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.12), rgba(239, 68, 68, 0.08));
  color: #DC2626;
  border: 1px solid rgba(239, 68, 68, 0.25);
}

.match-score {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.35rem;
  min-width: 100px;
}

.score-bar {
  width: 100px;
  height: 8px;
  border-radius: 50px;
  background: #E2E8F0;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  border-radius: 50px;
  transition: width 0.6s ease;
}

.score-fill--high {
  background: linear-gradient(90deg, #10B981, #059669);
}

.score-fill--medium {
  background: linear-gradient(90deg, #F59E0B, #D97706);
}

.score-fill--low {
  background: linear-gradient(90deg, #EF4444, #DC2626);
}

.score-number {
  font-size: 1.1rem;
  font-weight: 700;
  color: #1E293B;
}

.category-tip {
  margin: 0;
  color: #64748B;
  font-size: 0.9rem;
  line-height: 1.6;
}

.stage-panel {
  display: flex;
  gap: 1.25rem;
  padding: 1.5rem;
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(79, 70, 229, 0.06) 0%, rgba(16, 185, 129, 0.04) 100%),
    white;
  border: 2px solid rgba(79, 70, 229, 0.2);
  margin-bottom: var(--space-4);
}

.stage-icon {
  font-size: 3rem;
  flex-shrink: 0;
}

.stage-content {
  flex: 1;
}

.stage-label {
  margin: 0 0 0.4rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748B;
}

.stage-title {
  display: block;
  font-size: 1.35rem;
  color: #1E293B;
  margin-bottom: 0.5rem;
}

.stage-desc {
  margin: 0;
  color: #64748B;
  line-height: 1.7;
}

.selection-impact {
  display: flex;
  gap: 1rem;
  padding: 1.25rem;
  border-radius: 16px;
  background: linear-gradient(135deg, #F8FAFC, #F1F5F9);
  border: 1px solid rgba(226, 232, 240, 0.9);
  margin: 1.25rem 0;
}

.impact-icon {
  font-size: 2rem;
  flex-shrink: 0;
}

.impact-content {
  flex: 1;
}

.impact-label {
  margin: 0 0 0.3rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #64748B;
}

.impact-title {
  display: block;
  font-size: 1.15rem;
  color: #1E293B;
  margin-bottom: 0.4rem;
}

.impact-desc {
  margin: 0;
  color: #64748B;
  line-height: 1.65;
  font-size: 0.92rem;
}

.portrait-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.85rem;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex: 1;
  min-width: 140px;
  justify-content: center;
  transition: all 0.3s ease;
}

.action-btn:hover {
  gap: 0.5rem;
}

.action-btn--primary {
  background: linear-gradient(135deg, #4F46E5, #6366F1);
  box-shadow: 0 6px 20px -6px rgba(79, 70, 229, 0.4);
}

.action-btn--primary:hover {
  box-shadow: 0 8px 24px -8px rgba(79, 70, 229, 0.5);
  transform: translateY(-2px);
}

.btn-arrow {
  transition: transform 0.3s ease;
}

.matching-error {
  margin: 0;
  color: var(--danger-500);
}

@media (max-width: 1320px) {
  .matching-layout {
    grid-template-columns: 1fr;
  }

  .matching-col--portrait {
    position: static;
  }
}

@media (max-width: 960px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
  
  .profile-details-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .profile-summary {
    flex-direction: column;
    text-align: center;
  }
  
  .category-item__header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .match-score {
    width: 100%;
    align-items: stretch;
  }
  
  .score-bar {
    width: 100%;
  }
}
</style>
