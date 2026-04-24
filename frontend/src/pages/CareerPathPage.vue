<template>
  <AppShell>
    <section class="page-grid path-page">
      <PageHeader
        eyebrow="Path"
        title="职业路径"
        description="如果你已经有大致方向，这里会帮你看清：从现在开始，通常会怎么往上走，以及未来还能往哪些相近方向延展。"
      >
        <template #actions>
          <StepGuide
            storage-key="career-path"
            title="职业路径会跟着当前目标岗位变化"
            description="这一步不是给你贴固定标签，而是帮助你判断：以当前目标岗位为中心，你更适合从哪里起步，下一步应该往哪里走。"
            trigger-eyebrow="Next Step"
            trigger-title="职业路径说明"
            trigger-summary="看看这一步和岗位匹配、职业报告之间是什么关系。"
            note="如果你回到岗位匹配页换了目标方向，这里的职业路径也会跟着变化。确认方向后，再去生成职业报告会更稳。"
            :sections="careerPathGuideSections"
          />
        </template>
      </PageHeader>

      <!-- Mock Preview Section -->
      <MockPreviewOverlay
        v-if="!profileStore.studentProfile"
        title="上传简历，规划你的专属职业发展路径"
        @upload="router.push('/profile')"
      >
        <div class="mock-path-preview">
          <!-- Mock Path Graph -->
          <CardPanel
            eyebrow="Path"
            title="示例职业发展路径"
            description="这是一个以 Java 开发为例的典型发展路径，展示了从初级到高级的职业成长轨迹。"
          >
            <div class="mock-path-timeline">
              <!-- Start Node -->
              <div class="mock-path-node">
                <div class="mock-path-node__content">
                  <div class="mock-path-node__level">
                    <span class="mock-path-node__badge">初级</span>
                  </div>
                  <div class="mock-path-node__info">
                    <strong class="mock-path-node__title">初级 Java 开发工程师</strong>
                    <p class="text-secondary">0-2年经验</p>
                    <div class="tag-list">
                      <span class="badge">Java</span>
                      <span class="badge">Spring Boot</span>
                      <span class="badge">MySQL</span>
                    </div>
                    <div class="mock-path-node__salary">
                      <span class="mock-path-node__salary-label">参考薪资</span>
                      <span class="mock-path-node__salary-value">8k-15k</span>
                    </div>
                  </div>
                </div>
                <div class="mock-path-arrow">
                  <div class="mock-path-arrow__line"></div>
                  <div class="mock-path-arrow__dot"></div>
                </div>
              </div>

              <!-- Intermediate Node -->
              <div class="mock-path-node">
                <div class="mock-path-node__content">
                  <div class="mock-path-node__level">
                    <span class="mock-path-node__badge mock-path-node__badge--primary">中级</span>
                  </div>
                  <div class="mock-path-node__info">
                    <strong class="mock-path-node__title">中级 Java 开发工程师</strong>
                    <p class="text-secondary">2-5年经验</p>
                    <div class="tag-list">
                      <span class="badge">系统设计</span>
                      <span class="badge">微服务</span>
                      <span class="badge">性能优化</span>
                    </div>
                    <div class="mock-path-node__salary">
                      <span class="mock-path-node__salary-label">参考薪资</span>
                      <span class="mock-path-node__salary-value">15k-25k</span>
                    </div>
                  </div>
                </div>
                <div class="mock-path-arrow">
                  <div class="mock-path-arrow__line"></div>
                  <div class="mock-path-arrow__dot"></div>
                </div>
              </div>

              <!-- Branch Nodes -->
              <div class="mock-path-branch">
                <div class="mock-path-node mock-path-node--branch">
                  <div class="mock-path-node__content">
                    <div class="mock-path-node__level">
                      <span class="mock-path-node__badge mock-path-node__badge--accent">技术专家</span>
                    </div>
                    <div class="mock-path-node__info">
                      <strong class="mock-path-node__title">高级 Java 技术专家</strong>
                      <p class="text-secondary">5年+经验</p>
                      <div class="tag-list">
                        <span class="badge">架构设计</span>
                        <span class="badge">技术选型</span>
                        <span class="badge">团队指导</span>
                      </div>
                      <div class="mock-path-node__salary">
                        <span class="mock-path-node__salary-label">参考薪资</span>
                        <span class="mock-path-node__salary-value">25k-40k</span>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="mock-path-node mock-path-node--branch">
                  <div class="mock-path-node__content">
                    <div class="mock-path-node__level">
                      <span class="mock-path-node__badge mock-path-node__badge--secondary">技术管理</span>
                    </div>
                    <div class="mock-path-node__info">
                      <strong class="mock-path-node__title">技术经理/架构师</strong>
                      <p class="text-secondary">5年+经验</p>
                      <div class="tag-list">
                        <span class="badge">项目管理</span>
                        <span class="badge">团队管理</span>
                        <span class="badge">技术决策</span>
                      </div>
                      <div class="mock-path-node__salary">
                        <span class="mock-path-node__salary-label">参考薪资</span>
                        <span class="mock-path-node__salary-value">30k-50k</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Related Directions Section -->
            <div class="mock-related-directions">
              <p class="mock-related-directions__label">你还可以关注这些方向</p>
              <div class="mock-related-directions__list">
                <div class="mock-related-card">
                  <div class="mock-related-card__header">
                    <strong>Go 后端开发</strong>
                    <span class="badge badge--soft mock-related-card__similarity">相似度 85%</span>
                  </div>
                  <p class="text-secondary">与 Java 技术栈相近，适合后端转型，薪资区间 12k-28k</p>
                  <div class="tag-list">
                    <span class="badge">Go</span>
                    <span class="badge">后端</span>
                  </div>
                </div>
                <div class="mock-related-card">
                  <div class="mock-related-card__header">
                    <strong>大数据开发</strong>
                    <span class="badge badge--soft mock-related-card__similarity">相似度 78%</span>
                  </div>
                  <p class="text-secondary">基于 Java 生态，适合向数据领域发展，薪资区间 15k-35k</p>
                  <div class="tag-list">
                    <span class="badge">Hadoop</span>
                    <span class="badge">Spark</span>
                  </div>
                </div>
                <div class="mock-related-card">
                  <div class="mock-related-card__header">
                    <strong>云原生开发</strong>
                    <span class="badge badge--soft mock-related-card__similarity">相似度 72%</span>
                  </div>
                  <p class="text-secondary">适合向容器化、微服务方向发展，薪资区间 18k-40k</p>
                  <div class="tag-list">
                    <span class="badge">Kubernetes</span>
                    <span class="badge">Docker</span>
                  </div>
                </div>
              </div>
            </div>
          </CardPanel>
        </div>
      </MockPreviewOverlay>

      <!-- Empty State for No Target Job -->
      <EmptyState
        v-else-if="!pathTargetJob"
        title="还没有确定目标方向"
        description="你可以先去岗位匹配看看更适合自己的方向，再回来继续看路径。"
      >
        <template #actions>
          <AppButton @click="router.push('/matching')">前往岗位匹配</AppButton>
        </template>
      </EmptyState>

      <!-- Actual Content -->
      <template v-else>
        <CardPanel
          eyebrow="Profile"
          title="当前路径基线"
          description="这一步的重点不是直接下结论，而是帮你判断你现在大概站在这条路径的哪个位置。"
        >
          <div class="profile-summary">
            <div class="profile-summary__hero">
              <div>
                <p class="profile-summary__label">当前目标岗位</p>
                <strong>{{ pathTargetJob }}</strong>
                <p class="text-secondary">
                  {{ profileStore.studentProfile.basic_info.school || '学校未填写' }} |
                  {{ profileStore.studentProfile.basic_info.major || '专业未填写' }}
                </p>
              </div>

              <div class="tag-list">
                <span class="badge">{{ profileStore.studentProfile.job_preference.expected_position || '岗位待确认' }}</span>
                <span class="badge">技能 {{ profileStore.studentProfile.skills.length }}</span>
              </div>
            </div>

            <div class="profile-summary__grid">
              <div class="profile-summary__block">
                <p class="profile-summary__label">当前技能</p>
                <div class="tag-list">
                  <span v-for="skill in profileStore.studentProfile.skills.slice(0, 10)" :key="skill" class="badge">{{ skill }}</span>
                  <span v-if="profileStore.studentProfile.skills.length === 0" class="text-secondary">暂时还没有清晰的技能信息</span>
                </div>
              </div>

              <div class="profile-summary__block">
                <p class="profile-summary__label">求职偏好</p>
                <p>岗位：{{ profileStore.studentProfile.job_preference.expected_position || '未指定' }}</p>
                <p>薪资：{{ profileStore.studentProfile.job_preference.expected_salary || '未指定' }}</p>
                <p>城市：{{ profileStore.studentProfile.job_preference.expected_city || '未指定' }}</p>
              </div>
            </div>
          </div>

          <section class="path-impact">
            <p class="path-impact__label">当前来源</p>
            <strong>{{ targetSourceText }}</strong>
            <p class="text-secondary">
              现在展示的路径会围绕"{{ pathTargetJob }}"展开。如果你想看别的方向，请先回岗位匹配页切换目标。
            </p>
          </section>
        </CardPanel>

        <LoadingBlock v-if="reportStore.isLoading" label="正在生成职业路径..." />

        <CareerPathGraphSection
          v-else-if="reportStore.careerPath"
          :career-path="reportStore.careerPath"
          :show-horizontal="true"
          :highlight-current="true"
        />

        <EmptyState
          v-else
          title="暂时还没有路径结果"
          description="这条路径的结果还没整理出来，稍后再试一次。"
        />
      </template>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/layout/AppShell.vue';
import CareerPathGraphSection from '@/components/path/CareerPathGraphSection.vue';
import AppButton from '@/components/ui/AppButton.vue';
import CardPanel from '@/components/ui/CardPanel.vue';
import EmptyState from '@/components/ui/EmptyState.vue';
import LoadingBlock from '@/components/ui/LoadingBlock.vue';
import PageHeader from '@/components/ui/PageHeader.vue';
import StepGuide from '@/components/ui/StepGuide.vue';
import MockPreviewOverlay from '@/components/ui/MockPreviewOverlay.vue';
import { useProfileStore } from '@/stores/profile';
import { useReportStore } from '@/stores/report';

const careerPathGuideSections = [
  {
    title: '职业路径围绕当前目标岗位展开',
    description: '这里不是生成一个固定答案，而是以当前选中的岗位方向为中心，判断你更适合从哪个阶段切入。',
    tone: 'accent',
  },
  {
    title: '更换目标后，路径也会一起变化',
    description: '如果你在岗位匹配页改了方向，或者重新确认了目标岗位，这里的路径图会跟着更新。',
    tone: 'warning',
  },
  {
    title: '先看路径，再决定是否生成报告',
    description: '这一步适合用来判断起点和成长节奏。方向确认后，再进入职业报告，后面的建议会更聚焦。',
  },
] as const;

const router = useRouter();
const profileStore = useProfileStore();
const reportStore = useReportStore();

const pathTargetJob = computed(() => reportStore.targetJob || profileStore.studentProfile?.job_preference.expected_position || '');
const targetSourceText = computed(() => reportStore.sourceJob
  ? '来自岗位匹配页当前选中的岗位方向'
  : reportStore.targetJob
    ? '来自你当前正在查看的目标岗位'
    : '来自学生画像里的期望岗位');

onMounted(async () => {
  if (!profileStore.studentProfile || !pathTargetJob.value) {
    return;
  }

  try {
    await reportStore.loadCareerPath(pathTargetJob.value, profileStore.studentProfile);
  } catch {
    // Let empty state render.
  }
});

onUnmounted(() => {
  // 重置所有 loading 状态，防止离开页面后按钮仍然被禁用
  reportStore.isLoading = false;
  reportStore.toolbarLoading = false;
});
</script>

<style scoped>
.path-page {
  gap: var(--space-6);
}

.profile-summary {
  display: grid;
  gap: var(--space-4);
}

.profile-summary__hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  flex-wrap: wrap;
  padding: 1rem 1.05rem;
  border-radius: 12px;
  border: 1px solid color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  background:
    linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 99%, white), color-mix(in oklab, var(--bg-muted) 72%, white)),
    radial-gradient(circle at top right, color-mix(in oklab, var(--brand-500) 5%, transparent), transparent 34%);
}

.profile-summary__grid {
  display: grid;
  gap: var(--space-4);
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.profile-summary__block,
.path-impact {
  display: grid;
  gap: 0.45rem;
  padding: 1rem 1.05rem;
  border-radius: 12px;
  border: 1px solid color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  background: linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 99%, white), color-mix(in oklab, var(--bg-muted) 74%, white));
}

.path-impact {
  border-color: color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-surface) 98%, white);
}

.profile-summary__label,
.path-impact__label {
  margin: 0;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.profile-summary__hero strong,
.profile-summary__hero p,
.profile-summary__block strong,
.profile-summary__block p,
.path-impact strong,
.path-impact p {
  margin: 0;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

/* Mock Preview Styles */
.mock-path-preview {
  display: grid;
  gap: var(--space-4);
}

.mock-path-timeline {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-4) 0;
}

.mock-path-node {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  position: relative;
}

.mock-path-node__content {
  display: flex;
  gap: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-subtle);
  background: linear-gradient(135deg, var(--bg-surface) 0%, color-mix(in oklab, var(--accent-orange) 5%, var(--bg-surface)) 100%);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
}

.mock-path-node__content:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: color-mix(in oklab, var(--accent-orange) 30%, var(--border-subtle));
}

.mock-path-node--branch .mock-path-node__content {
  background: linear-gradient(135deg, var(--bg-surface) 0%, color-mix(in oklab, var(--primary-blue) 5%, var(--bg-surface)) 100%);
}

.mock-path-node__level {
  display: flex;
  align-items: flex-start;
}

.mock-path-node__badge {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  background: var(--bg-muted);
  color: var(--text-primary);
  font-weight: 700;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.mock-path-node__badge--primary {
  background: color-mix(in oklab, var(--primary-blue) 15%, var(--bg-muted));
  color: var(--primary-blue);
}

.mock-path-node__badge--accent {
  background: color-mix(in oklab, var(--accent-orange) 15%, var(--bg-muted));
  color: var(--accent-orange);
}

.mock-path-node__badge--secondary {
  background: color-mix(in oklab, var(--brand-500) 15%, var(--bg-muted));
  color: var(--brand-500);
}

.mock-path-node__info {
  flex: 1;
  display: grid;
  gap: var(--space-2);
}

.mock-path-node__title {
  font-size: 1.1rem;
  color: var(--text-primary);
}

.mock-path-node__salary {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-1);
}

.mock-path-node__salary-label {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.mock-path-node__salary-value {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--accent-orange);
}

.mock-path-arrow {
  position: relative;
  height: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.mock-path-arrow__line {
  width: 2px;
  height: 100%;
  background: linear-gradient(180deg, var(--border-subtle) 0%, color-mix(in oklab, var(--accent-orange) 40%, var(--border-subtle)) 100%);
  position: relative;
  animation: flowPulse 2s ease-in-out infinite;
}

.mock-path-arrow__line::after {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-orange);
  animation: flowDot 2s ease-in-out infinite;
}

@keyframes flowPulse {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

@keyframes flowDot {
  0% {
    top: 0;
    opacity: 0;
  }
  20% {
    opacity: 1;
  }
  80% {
    opacity: 1;
  }
  100% {
    top: 100%;
    opacity: 0;
  }
}

.mock-path-arrow__dot {
  position: absolute;
  bottom: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent-orange);
}

.mock-path-branch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  margin-top: var(--space-2);
}

.mock-path-branch .mock-path-arrow {
  display: none;
}

/* Related Directions Styles */
.mock-related-directions {
  margin-top: var(--space-6);
  padding-top: var(--space-4);
  border-top: 1px dashed var(--border-subtle);
}

.mock-related-directions__label {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: var(--space-3);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.mock-related-directions__list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

.mock-related-card {
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-subtle);
  background: var(--bg-surface);
  transition: all var(--transition-base);
}

.mock-related-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: color-mix(in oklab, var(--primary-blue) 20%, var(--border-subtle));
}

.mock-related-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.mock-related-card__similarity {
  color: var(--primary-blue);
  font-weight: 600;
}

@media (max-width: 960px) {
  .profile-summary__grid {
    grid-template-columns: 1fr;
  }

  .mock-path-branch {
    grid-template-columns: 1fr;
  }

  .mock-related-directions__list {
    grid-template-columns: 1fr;
  }
}
</style>
