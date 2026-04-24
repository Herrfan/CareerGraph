<template>
  <AppShell>
    <section class="page-grid report-page">
      <PageHeader
        eyebrow="Report"
        title="职业报告中心"
        description="这里会把你的职业报告和后续行动建议放在一起，方便你边看边调整。"
      >
        <template #actions>
          <div class="report-header-actions">
            <button
              class="report-action-btn report-action-btn--secondary"
              :disabled="!profileStore.studentProfile"
              :title="!profileStore.studentProfile ? '上传简历后可下载完整报告' : ''"
            >
              📄 下载 PDF 报告
            </button>
            <button
              class="report-action-btn report-action-btn--primary"
              :disabled="!profileStore.studentProfile"
              :title="!profileStore.studentProfile ? '上传简历后可下载完整报告' : ''"
            >
              🔗 分享报告
            </button>
          </div>
          <StepGuide
            storage-key="career-report"
            title="职业报告会围绕当前目标岗位生成"
            description="这份报告不是泛泛而谈，而是基于当前学生画像和当前目标岗位整理出来的。如果目标变了，报告结论也会跟着变。"
            trigger-eyebrow="Next Step"
            trigger-title="职业报告说明"
            trigger-summary="看看报告是根据什么生成的，以及什么时候该回去改前面的选择。"
            note="如果你是从岗位匹配页进入这里，当前选中的岗位方向会直接影响报告内容和成长建议。想换方向，最好先回岗位匹配页切换目标，再重新生成报告。"
            :sections="reportGuideSections"
          />
        </template>
      </PageHeader>

      <MockPreviewOverlay
        v-if="!profileStore.studentProfile"
        @upload="router.push('/profile')"
        :title="'上传简历，生成你的专属职业竞争力报告'"
      >
        <div class="mock-report-preview">
          <CardPanel
            eyebrow="Report"
            title="示例职业报告"
            description="这是一个以Java开发为目标的示例报告，展示报告的基本结构和内容风格。"
          >
            <div class="mock-report-sections">
              <article class="mock-report-section mock-report-section--summary">
                <div class="mock-report-section__header">
                  <span class="mock-report-section__number">01</span>
                  <div>
                    <h3 class="mock-report-section__title">报告摘要</h3>
                    <p class="text-secondary">你的职业竞争力综合评估</p>
                  </div>
                </div>
                <div class="mock-report-section__content">
                  <div class="mock-summary-card">
                    <div class="mock-summary-card__score">
                      <div class="mock-summary-score__main">
                        <span class="mock-summary-score__number">78</span>
                        <span class="mock-summary-score__label">分</span>
                      </div>
                      <span class="mock-summary-score__grade">B+</span>
                    </div>
                    <div class="mock-summary-card__details">
                      <div class="mock-summary-item mock-summary-item--positive">
                        <span class="mock-summary-item__icon">✅</span>
                        <div>
                          <strong>核心优势</strong>
                          <p class="text-secondary">专业基础扎实 · 有2段项目经历</p>
                        </div>
                      </div>
                      <div class="mock-summary-item mock-summary-item--improve">
                        <span class="mock-summary-item__icon">📈</span>
                        <div>
                          <strong>待提升项</strong>
                          <p class="text-secondary">建议补充实习经历</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </article>

              <article class="mock-report-section">
                <div class="mock-report-section__header">
                  <span class="mock-report-section__number">02</span>
                  <div>
                    <h3 class="mock-report-section__title">技能匹配分析</h3>
                    <p class="text-secondary">你的技能与目标岗位的匹配度分析</p>
                  </div>
                </div>
                <div class="mock-report-section__content">
                  <div class="mock-radar-chart">
                    <svg viewBox="0 0 200 200" class="mock-radar-svg">
                      <polygon points="100,10 170,45 170,105 100,140 30,105 30,45" fill="none" stroke="var(--border-subtle)" stroke-width="2"/>
                      <polygon points="100,30 150,55 150,95 100,120 50,95 50,55" fill="none" stroke="var(--border-subtle)" stroke-width="1"/>
                      <polygon points="100,50 130,65 130,85 100,100 70,85 70,65" fill="none" stroke="var(--border-subtle)" stroke-width="1"/>
                      
                      <polygon points="100,40 145,60 145,90 100,110 55,90 55,60" fill="color-mix(in oklab, var(--accent-orange), white 80%)" opacity="0.3"/>
                      
                      <line x1="100" y1="10" x2="100" y2="140" stroke="var(--border-subtle)" stroke-width="1"/>
                      <line x1="30" y1="45" x2="170" y2="105" stroke="var(--border-subtle)" stroke-width="1"/>
                      <line x1="30" y1="105" x2="170" y2="45" stroke="var(--border-subtle)" stroke-width="1"/>
                    </svg>
                    <div class="mock-radar-placeholder-text">
                      <span class="mock-radar-icon">📊</span>
                      <p>上传简历后生成</p>
                    </div>
                  </div>
                </div>
              </article>

              <article class="mock-report-section">
                <div class="mock-report-section__header">
                  <span class="mock-report-section__number">03</span>
                  <div>
                    <h3 class="mock-report-section__title">行动建议</h3>
                    <p class="text-secondary">根据你的画像定制的成长建议</p>
                  </div>
                </div>
                <div class="mock-report-section__content">
                  <div class="mock-suggestions-list">
                    <div class="mock-suggestion-item">
                      <span class="mock-suggestion-item__number">01</span>
                      <div class="mock-suggestion-item__content">
                        <strong>建议学习 Spring Boot 框架</strong>
                        <p class="text-secondary">完成1-2个实践项目，熟悉企业级开发流程</p>
                      </div>
                    </div>
                    <div class="mock-suggestion-item">
                      <span class="mock-suggestion-item__number">02</span>
                      <div class="mock-suggestion-item__content">
                        <strong>推荐投递 10-15k 区间的初级岗位</strong>
                        <p class="text-secondary">重点关注杭州地区的互联网公司和创业公司</p>
                      </div>
                    </div>
                    <div class="mock-suggestion-item">
                      <span class="mock-suggestion-item__number">03</span>
                      <div class="mock-suggestion-item__content">
                        <strong>参与开源社区贡献</strong>
                        <p class="text-secondary">选择1-2个感兴趣的项目，提升协作能力</p>
                      </div>
                    </div>
                    <div class="mock-suggestion-item">
                      <span class="mock-suggestion-item__number">04</span>
                      <div class="mock-suggestion-item__content">
                        <strong>定期更新技术博客</strong>
                        <p class="text-secondary">总结学习内容，建立个人技术影响力</p>
                      </div>
                    </div>
                  </div>
                </div>
              </article>

              <article class="mock-report-section mock-report-section--highlight">
                <div class="mock-report-section__header">
                  <span class="mock-report-section__number">04</span>
                  <div>
                    <h3 class="mock-report-section__title">职业成长路径</h3>
                    <p class="text-secondary">展示你的职业发展可能性</p>
                  </div>
                </div>
                <div class="mock-report-section__content">
                  <div class="mock-path-mini">
                    <div class="mock-path-mini__item">
                      <div class="mock-path-mini__badge">初级</div>
                      <div class="mock-path-mini__info">
                        <strong>Java开发工程师</strong>
                        <span class="mock-path-mini__salary">8-12k</span>
                      </div>
                    </div>
                    <div class="mock-path-mini__arrow">→</div>
                    <div class="mock-path-mini__item mock-path-mini__item--current">
                      <div class="mock-path-mini__badge">中级</div>
                      <div class="mock-path-mini__info">
                        <strong>中级开发工程师</strong>
                        <span class="mock-path-mini__salary">15-25k</span>
                      </div>
                    </div>
                  </div>
                </div>
              </article>
            </div>
          </CardPanel>
        </div>
      </MockPreviewOverlay>

      <template v-else>
        <CardPanel
          eyebrow="Profile"
          title="当前学生画像"
          description="这里固定展示当前学生画像摘要，方便你在看报告前先确认依据是否准确。"
        >
          <div class="profile-preview-grid">
            <div class="profile-preview-hero">
              <div class="profile-preview-hero__main">
                <p class="report-context__label">画像总览</p>
                <strong>{{ profileStore.studentProfile.basic_info.name || '未填写姓名' }}</strong>
                <p class="text-secondary">
                  {{ profileStore.studentProfile.basic_info.school || '学校未填' }} |
                  {{ profileStore.studentProfile.basic_info.major || '专业未填' }} |
                  {{ profileStore.studentProfile.basic_info.education || '学历未填' }}
                </p>
              </div>
              <div class="tag-list">
                <span class="badge">{{ profileStore.studentProfile.job_preference.expected_position || '岗位待确认' }}</span>
                <span class="badge">技能 {{ profileStore.studentProfile.skills.length }}</span>
                <span class="badge">证书 {{ profileStore.studentProfile.certificates.length }}</span>
              </div>
            </div>

            <div class="profile-preview-summary-grid">
              <div class="profile-preview-block">
                <p class="report-context__label">基本信息</p>
                <div class="profile-preview-block__content">
                  <p>姓名：{{ profileStore.studentProfile.basic_info.name || '未填写' }}</p>
                  <p>学历：{{ profileStore.studentProfile.basic_info.education || '未填写' }}</p>
                  <p>专业：{{ profileStore.studentProfile.basic_info.major || '未填写' }}</p>
                  <p>学校：{{ profileStore.studentProfile.basic_info.school || '未填写' }}</p>
                </div>
              </div>

              <div class="profile-preview-block">
                <p class="report-context__label">求职偏好</p>
                <div class="profile-preview-block__content">
                  <p>岗位：{{ profileStore.studentProfile.job_preference.expected_position || '未指定' }}</p>
                  <p>薪资：{{ profileStore.studentProfile.job_preference.expected_salary || '未指定' }}</p>
                  <p>城市：{{ profileStore.studentProfile.job_preference.expected_city || '未指定' }}</p>
                </div>
              </div>
            </div>

            <div class="profile-preview-summary-grid">
              <div class="profile-preview-block">
                <p class="report-context__label">技能标签</p>
                <div class="tag-list">
                  <span v-for="skill in profileStore.studentProfile.skills" :key="skill" class="badge">{{ skill }}</span>
                  <span v-if="profileStore.studentProfile.skills.length === 0" class="text-secondary">暂时还没有明确技能</span>
                </div>
              </div>

              <div class="profile-preview-block">
                <p class="report-context__label">证书与荣誉</p>
                <div class="tag-list">
                  <span v-for="certificate in profileStore.studentProfile.certificates" :key="certificate" class="badge">{{ certificate }}</span>
                  <span v-if="profileStore.studentProfile.certificates.length === 0" class="text-secondary">暂时还没有明确证书或荣誉</span>
                </div>
              </div>
            </div>

            <div class="profile-preview-block">
              <p class="report-context__label">能力描述</p>
              <div class="profile-preview-ability-grid">
                <article class="profile-preview-ability-card profile-preview-ability-card--hard">
                  <strong>硬实力</strong>
                  <p>{{ profileStore.studentProfile.ability_descriptions?.professional_skill || '还没有形成明确的硬实力描述。' }}</p>
                </article>
                <article class="profile-preview-ability-card profile-preview-ability-card--soft">
                  <strong>软实力</strong>
                  <p>{{ formatSoftSkillDescription() || '还没有形成明确的软实力描述。' }}</p>
                </article>
              </div>
            </div>

            <div class="profile-preview-block">
              <div class="profile-preview-block__topline">
                <p class="report-context__label">实习经历</p>
                <span class="badge">{{ profileStore.studentProfile.internship_experiences?.length ?? 0 }}</span>
              </div>
              <div v-if="profileStore.studentProfile.internship_experiences?.length" class="profile-evidence-list">
                <article
                  v-for="(experience, index) in profileStore.studentProfile.internship_experiences"
                  :key="`report-internship-${index}`"
                  class="profile-evidence-item"
                >
                  <div class="profile-preview-block__topline">
                    <strong>{{ experience.company || '未填写单位' }}</strong>
                    <span class="badge">{{ experience.period || '时间未填' }}</span>
                  </div>
                  <p class="profile-evidence-item__subline">{{ experience.position || '岗位未填' }}</p>
                  <p>{{ experience.achievement || '暂无详细成果描述。' }}</p>
                </article>
              </div>
              <p v-else class="text-secondary">暂时还没有补充实习经历。</p>
            </div>

            <div class="profile-preview-block">
              <div class="profile-preview-block__topline">
                <p class="report-context__label">项目经历</p>
                <span class="badge">{{ profileStore.studentProfile.project_experiences?.length ?? 0 }}</span>
              </div>
              <div v-if="profileStore.studentProfile.project_experiences?.length" class="profile-evidence-list">
                <article
                  v-for="(project, index) in profileStore.studentProfile.project_experiences"
                  :key="`report-project-${index}`"
                  class="profile-evidence-item"
                >
                  <div class="profile-preview-block__topline">
                    <strong>{{ project.name || '未命名项目' }}</strong>
                    <span class="badge">{{ project.role || '角色未填' }}</span>
                  </div>
                  <p>{{ project.description || '暂无项目描述。' }}</p>
                  <div v-if="project.tech_stacks?.length" class="tag-list tag-list--compact">
                    <span v-for="stack in project.tech_stacks" :key="`${project.name}-${stack}`" class="badge">{{ stack }}</span>
                  </div>
                  <p v-if="project.highlight" class="profile-evidence-item__subline">亮点：{{ project.highlight }}</p>
                </article>
              </div>
              <p v-else class="text-secondary">暂时还没有补充项目经历。</p>
            </div>
          </div>
        </CardPanel>

        <CardPanel
          v-if="profileScoring"
          eyebrow="Analysis"
          title="就业能力评分"
          :description="`基于 ${profileScoring.target_job_title} 岗位画像进行评估`"
        >
          <div v-if="isScoringLoading" class="scoring-loading">
            <LoadingBlock />
          </div>
          <div v-else class="scoring-container">
            <div class="scoring-card-group">
              <div class="scoring-card scoring-card--completeness">
                <div class="scoring-card__header">
                  <span class="scoring-card__icon">📋</span>
                  <div>
                    <p class="scoring-card__label">完整度</p>
                    <p class="scoring-card__subtitle">画像完整性评估</p>
                  </div>
                </div>
                <div class="scoring-card__score">
                  <span class="scoring-card__score-value">{{ Math.round(profileScoring.completeness_score) }}</span>
                  <span class="scoring-card__score-unit">分</span>
                </div>
                <div class="scoring-card__progress">
                  <div class="scoring-card__progress-bar" :style="{ width: `${profileScoring.completeness_score}%` }"></div>
                </div>
              </div>
              <div class="scoring-card scoring-card--competitiveness">
                <div class="scoring-card__header">
                  <span class="scoring-card__icon">🏆</span>
                  <div>
                    <p class="scoring-card__label">竞争力</p>
                    <p class="scoring-card__subtitle">综合竞争力评估</p>
                  </div>
                </div>
                <div class="scoring-card__score">
                  <span class="scoring-card__score-value">{{ Math.round(profileScoring.competitiveness_score) }}</span>
                  <span class="scoring-card__score-unit">分</span>
                </div>
                <div class="scoring-card__progress">
                  <div class="scoring-card__progress-bar" :style="{ width: `${profileScoring.competitiveness_score}%` }"></div>
                </div>
              </div>
            </div>
            <div class="scoring-details">
              <div class="scoring-detail-section">
                <p class="scoring-detail__title">完整度说明</p>
                <ul class="scoring-detail-list">
                  <li v-for="(item, index) in profileScoring.completeness_breakdown" :key="`completeness-${index}`" class="scoring-detail-item">
                    {{ item }}
                  </li>
                </ul>
              </div>
              <div class="scoring-detail-section">
                <p class="scoring-detail__title">竞争力说明</p>
                <ul class="scoring-detail-list">
                  <li v-for="(item, index) in profileScoring.competitiveness_breakdown" :key="`competitiveness-${index}`" class="scoring-detail-item">
                    {{ item }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </CardPanel>

        <CardPanel
          eyebrow="Context"
          title="报告目标"
          description="先确认当前画像和目标方向，再开始整理这份更贴近你的职业报告。"
        >
          <div class="report-context">
            <div class="report-context__summary">
              <div>
                <p class="report-context__label">学生画像</p>
                <strong>{{ profileStore.studentProfile.basic_info.name || '未填写姓名' }}</strong>
                <p class="text-secondary">
                  {{ profileStore.studentProfile.basic_info.school || '学校未填' }} |
                  {{ profileStore.studentProfile.basic_info.major || '专业未填' }}
                </p>
              </div>
              <div>
                <p class="report-context__label">当前目标岗位</p>
                <strong>{{ currentTargetJob || '尚未指定目标岗位' }}</strong>
                <p class="text-secondary">后面的分析和建议都会围绕这个方向展开。</p>
              </div>
            </div>

            <div class="report-context__meta">
              <span class="badge">技能 {{ profileStore.studentProfile.skills.length }}</span>
              <span class="badge">证书 {{ profileStore.studentProfile.certificates.length }}</span>
            </div>

            <section class="report-context__impact">
              <div class="report-context__impact-icon">📌</div>
              <div class="report-context__impact-content">
                <p class="report-context__label">当前来源</p>
                <strong>{{ targetSourceText }}</strong>
                <p class="text-secondary">
                  如果你觉得报告方向不对，通常不是直接改正文，而是先回岗位匹配或学生画像页调整目标，再重新生成。
                </p>
              </div>
            </section>
          </div>
        </CardPanel>

        <div class="example-report-section">
          <div class="example-report-header">
            <div class="example-report-icon">✨</div>
            <div>
              <p class="example-report-title">想快速体验报告功能？</p>
              <p class="example-report-desc text-secondary">先看看示例报告，了解这个工具能为你提供什么价值</p>
            </div>
          </div>
          <div class="example-report-actions">
            <AppButton variant="primary" @click="useExampleReport()">使用示例报告</AppButton>
          </div>
        </div>

        <ReportToolbar
      :disabled="reportStore.toolbarLoading || reportStore.isLoading"
      @generate="handleGenerate"
      @check="handleCheck"
      @polish="handlePolish"
      @export="handleExport"
    />

        <CardPanel
          class="report-layout__main"
          title="职业报告"
          description="你可以先看系统生成的草稿，再按自己的理解慢慢修改和补充。"
        >
          <LoadingBlock v-if="reportStore.isLoading" label="正在生成职业报告..." />

          <div v-else class="report-markdown">
            <div v-if="reportStore.report?.markdown_content" class="report-markdown__toolbar">
              <AppButton :variant="isPreview ? 'secondary' : 'primary'" @click="isPreview = false">编辑</AppButton>
              <AppButton :variant="isPreview ? 'primary' : 'secondary'" @click="isPreview = true">预览</AppButton>
            </div>

            <div v-if="reportStore.report?.markdown_content && !isPreview" class="report-markdown__quick-actions">
              <AppButton variant="secondary" @click="insertHeading('## 新增分析小节')">插入小节</AppButton>
              <AppButton variant="secondary" @click="insertBulletList()">插入清单</AppButton>
              <AppButton variant="secondary" @click="insertActionBlock()">插入行动计划</AppButton>
            </div>

            <textarea
              v-if="reportStore.report?.markdown_content && !isPreview"
              v-model="editableMarkdown"
              class="textarea report-markdown__textarea"
              rows="20"
            />

            <article
              v-if="reportStore.report?.markdown_content && isPreview"
              class="report-markdown__preview markdown-body"
              v-html="renderedMarkdown"
            />

            <p v-else class="text-secondary">点击上方“生成报告”后，正文会显示在这里。</p>

            <div v-if="reportStore.report?.markdown_content && !isPreview" class="report-markdown__actions">
              <AppButton variant="secondary" @click="syncEditedReport()">保存编辑内容</AppButton>
            </div>
          </div>
        </CardPanel>

        <GrowthPlanSection :plan="reportStore.growthPlan" />
      </template>

      <div class="report-bottom-cta">
        <p class="text-secondary">
          也可以从岗位匹配页带着目标岗位进入 →
        </p>
        <AppButton variant="secondary" @click="router.push('/matching')">
          前往岗位匹配
        </AppButton>
      </div>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { marked } from 'marked';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/layout/AppShell.vue';
import GrowthPlanSection from '@/components/report/GrowthPlanSection.vue';
import ReportToolbar from '@/components/report/ReportToolbar.vue';
import AppButton from '@/components/ui/AppButton.vue';
import CardPanel from '@/components/ui/CardPanel.vue';
import EmptyState from '@/components/ui/EmptyState.vue';
import LoadingBlock from '@/components/ui/LoadingBlock.vue';
import PageHeader from '@/components/ui/PageHeader.vue';
import StepGuide from '@/components/ui/StepGuide.vue';
import MockPreviewOverlay from '@/components/ui/MockPreviewOverlay.vue';
import { useProfileStore } from '@/stores/profile';
import { useReportStore } from '@/stores/report';
import { scoreProfile } from '@/services/report';
import type { ProfileScoring } from '@/types/api';

const reportGuideSections = [
  {
    title: '报告内容围绕当前目标岗位生成',
    description: '当前目标来自岗位匹配页的选择，或来自学生画像里的期望岗位。目标不同，报告重点也会不同。',
    tone: 'accent',
  },
  {
    title: '方向不对时，优先回前面调整',
    description: '如果你发现报告方向偏了，通常应该先回岗位匹配或学生画像页改目标，而不是直接在正文里硬改结论。',
    tone: 'warning',
  },
  {
    title: '确认方向后再打磨内容',
    description: '先用这份报告看清能力差距和行动建议，方向确认后再做润色、补充和导出，会更省时间。',
  },
] as const;

const router = useRouter();
const profileStore = useProfileStore();
const reportStore = useReportStore();
const editableMarkdown = ref('');
const isPreview = ref(true);
const profileScoring = ref<ProfileScoring | null>(null);
const isScoringLoading = ref(false);

const currentTargetJob = computed(() => reportStore.targetJob || reportStore.sourceJob?.job.title || profileStore.studentProfile?.job_preference.expected_position || '');
const renderedMarkdown = computed(() => (marked.parse(editableMarkdown.value || '') as string));
const targetSourceText = computed(() => reportStore.sourceJob
  ? '来自岗位匹配页当前选中的岗位方向'
  : reportStore.targetJob
    ? '来自你当前保留的报告目标'
    : '来自学生画像里的期望岗位');

function formatSoftSkillDescription() {
  const descriptions = profileStore.studentProfile?.ability_descriptions;
  if (!descriptions) {
    return '';
  }
  if (descriptions.soft_skill?.trim()) {
    return descriptions.soft_skill.trim();
  }

  const parts = [
    descriptions.learning?.trim() ? `学习能力：${descriptions.learning.trim()}` : '',
    descriptions.communication?.trim() ? `沟通协作：${descriptions.communication.trim()}` : '',
    descriptions.stress_tolerance?.trim() ? `抗压稳定：${descriptions.stress_tolerance.trim()}` : '',
    descriptions.innovation?.trim() ? `创新进取：${descriptions.innovation.trim()}` : '',
  ].filter(Boolean);

  return parts.join('；');
}

async function loadProfileScoring() {
    if (!profileStore.studentProfile) {
      console.log('[ReportPage] No student profile for scoring');
      return;
    }
    
    console.log('[ReportPage] Loading profile scoring for target job:', currentTargetJob.value);
    isScoringLoading.value = true;
    try {
      profileScoring.value = await scoreProfile(profileStore.studentProfile, currentTargetJob.value);
      console.log('[ReportPage] Profile scoring loaded:', profileScoring.value);
    } catch (error) {
      console.error('[ReportPage] Failed to load profile scoring:', error);
    } finally {
      isScoringLoading.value = false;
    }
  }

  async function useExampleReport() {
    console.log('[ReportPage] Using example report');
    
    // 设置示例成长计划
    reportStore.growthPlan = {
      short_term_plan: {
        title: '接下来3个月',
        learning_path: [
          {
            title: '深入学习Spring Boot核心原理',
            detail: '重点掌握依赖注入、AOP、数据库操作、RESTful接口设计，通过阅读源码和动手实践来深化理解'
          },
          {
            title: '补充数据库设计知识',
            detail: '学习数据库规范化、索引优化、事务处理，通过设计2-3个实际项目的数据库来练习'
          },
          {
            title: '提高代码质量意识',
            detail: '阅读《Effective Java》，学习常用设计模式，养成写单元测试的习惯'
          }
        ],
        practice_arrangements: [
          {
            title: '完成1个高质量项目',
            detail: '从0到1完成一个有完整功能、有清晰文档、有单元测试覆盖的项目，重点展示自己的工程思维'
          },
          {
            title: '整理现有项目',
            detail: '把已有的项目重新整理，补充项目背景、技术选型理由、遇到的难点和解决方案、个人贡献等文档'
          }
        ],
        reminders: ['不要只堆技术名词，重点展示项目中自己的真实贡献和解决问题的能力']
      },
      mid_term_plan: {
        title: '接下来6-12个月',
        learning_path: [
          {
            title: '提高系统设计能力',
            detail: '学习分布式系统、缓存、消息队列、负载均衡等知识，通过阅读技术博客和参与开源项目来积累经验'
          },
          {
            title: '拓展技术广度',
            detail: '了解云计算、容器化、CI/CD等现代开发流程，保持对技术趋势的敏感度'
          }
        ],
        practice_arrangements: [
          {
            title: '争取1份有质量的实习',
            detail: '目标是进入有技术氛围的团队，在真实的业务场景中学习和成长，重点关注代码规范、工程实践和团队协作'
          }
        ],
        reminders: ['重点是在实践中理解，而不是在面试前临时背一堆概念']
      }
    }as any;
    
    // 设置示例报告内容
    reportStore.report = {
      success: true,
      markdown_content: `# 学生职业发展分析报告

## 画像总结

范子豪，浙江科技大学计算机科学与技术专业本科在读。从画像信息来看，你已经建立了良好的技术基础，掌握了Java、Python、MySQL、Vue、Git、Linux等技能，有项目和实习经验的积累，并且对Java开发方向有明确的求职意向，目标薪资8K-12K，期望在杭州发展。

你的优势包括：
- 技术栈比较全面，既有后端又有前端基础
- 有实际的项目经验和实习经历
- 对自己的职业方向有一定规划

## 目标岗位分析

**目标岗位族：Java开发**

从你的技能和经历来看，Java开发是一个非常适合你的方向。这个岗位通常需要：
- 扎实的Java基础和面向对象设计能力
- 熟悉Spring Boot等主流框架
- 数据库设计和SQL优化能力
- 良好的工程实践和代码规范意识
- 基本的前端知识（如Vue）

你目前的技能已经覆盖了其中很大一部分，接下来的重点是提高深度和完善度。

## 当前阶段判断

**更适合：入门/初级Java开发工程师**

你已经具备了入门的基本条件，但要进入下一阶段（中级），还需要在以下方面有所提升：
- 更扎实的Java基础和系统设计能力
- 更完整的项目经验和成果展示
- 对软件工程和代码质量的深入理解
- 更强的解决问题能力

## 项目与实践分析

你已有2个项目和1个实习经历，这是很好的基础。建议重点整理：

**在线学习平台项目**
- 清晰描述项目背景和解决的问题
- 明确你在其中的具体职责和贡献
- 突出技术难点和解决方案
- 补充项目成果和用户反馈（如果有）

**智能图书管理系统项目**
- 强调Python在数据分析中的应用场景
- 整理技术栈选择的理由
- 补充项目亮点和实际应用价值

**杭州某科技有限公司实习**
- 用数据和成果说话（如"优化了XX，提升了XX%"）
- 明确你在团队中的角色和协作方式
- 总结实习期间的主要收获和成长

## 下一步建议

### 短期（接下来3个月）

1. **深化Spring Boot学习**
   - 阅读《Spring Boot实战》或相关官方文档
   - 动手完成1-2个有完整功能的项目
   - 重点理解依赖注入、AOP、数据库操作等核心概念

2. **完善项目文档**
   - 为每个项目写清晰的README
   - 补充技术选型理由、架构设计图
   - 整理项目中遇到的问题和解决方案

3. **积累面试准备**
   - 系统复习Java基础（集合、并发、JVM等）
   - 刷一定量的算法题（重点是数组、链表、树等常见题型）
   - 准备项目介绍的话术

### 中期（接下来6-12个月）

1. **争取高质量实习机会**
   - 目标是进入技术氛围好的团队
   - 在实习中重点学习工程实践和团队协作
   - 争取做出有实际价值的贡献

2. **拓展技术视野**
   - 了解微服务、分布式系统等前沿技术
   - 学习云计算和容器化基础
   - 保持对新技术的敏感度

## 关键提醒

不要只追求技术名词的堆砌，重点是：
- 展示自己解决问题的能力
- 证明自己能写可维护的代码
- 体现团队协作和沟通能力
- 持续学习和成长的态度

你的基础不错，只要坚持积累和实践，一定能达到目标。加油！`
    };
    
    editableMarkdown.value = reportStore.report.markdown_content;
    isPreview.value = true;
    
    console.log('[ReportPage] Example report loaded');
  }

  async function bootstrap() {
    console.log('[ReportPage] Bootstrap starting...');
    
    if (!profileStore.studentProfile) {
      console.log('[ReportPage] Loading student profile...');
      await profileStore.loadMyProfile();
    }

    if (!profileStore.studentProfile) {
      console.log('[ReportPage] No student profile found');
      return;
    }

    console.log('[ReportPage] Student profile found:', profileStore.studentProfile);
    console.log('[ReportPage] Current target job:', currentTargetJob.value);

    if (!currentTargetJob.value) {
      console.log('[ReportPage] No target job, skipping report generation but loading scoring...');
      await loadProfileScoring();
      return;
    }

    reportStore.setTargetContext(currentTargetJob.value, reportStore.sourceJob);
    
    try {
      const snapshot = await reportStore.loadLatestSnapshot(profileStore.studentProfile, currentTargetJob.value);
      await reportStore.loadSnapshotHistory(profileStore.studentProfile);

      if (!snapshot?.growth_plan) {
        await Promise.allSettled([reportStore.loadGrowthPlan(profileStore.studentProfile, currentTargetJob.value)]);
      }

      if (!snapshot?.markdown_content) {
        await Promise.allSettled([reportStore.createReport(profileStore.studentProfile, reportStore.sourceJob?.job.job_id, currentTargetJob.value)]);
      }
    } catch (error) {
      console.error('[ReportPage] Error loading report:', error);
    }

    await loadProfileScoring();
  }

async function handleGenerate() {
  if (!profileStore.studentProfile) {
    console.log('[ReportPage] Cannot generate report: no student profile');
    return;
  }

  if (!currentTargetJob.value) {
    console.log('[ReportPage] No target job, using default target job for scoring only');
    await loadProfileScoring();
    return;
  }

  reportStore.setTargetContext(currentTargetJob.value, reportStore.sourceJob);
  try {
    await reportStore.createReport(profileStore.studentProfile, reportStore.sourceJob?.job.job_id, currentTargetJob.value);
    await reportStore.loadGrowthPlan(profileStore.studentProfile, currentTargetJob.value);
    await reportStore.persistSnapshot(profileStore.studentProfile);
  } catch (error) {
    console.error('[ReportPage] Error generating report:', error);
  }
}

async function syncEditedReport() {
  reportStore.setReportMarkdown(editableMarkdown.value);
  if (profileStore.studentProfile) {
    await reportStore.persistSnapshot(profileStore.studentProfile);
  }
}

async function handleCheck() {
  await reportStore.runCompletenessCheck(profileStore.studentProfile);
}

async function handlePolish() {
  const result = await reportStore.runPolish(profileStore.studentProfile, 'full_report', 'professional');
  const polished = result?.polished_content;
  if (typeof polished === 'string' && polished.trim()) {
    reportStore.setReportMarkdown(polished);
    if (profileStore.studentProfile) {
      await reportStore.persistSnapshot(profileStore.studentProfile);
    }
  }
}

async function handleExport() {
  const exportResult = await reportStore.runExport(profileStore.studentProfile, {
    exportFormat: 'html',
    exportSections: ['full_report'],
    pageStyle: 'formal',
    headerText: 'Career Agent Plus',
    footerText: 'Generated by Career Agent Plus',
    showPageNumbers: true,
  });

  if (!exportResult?.content) {
    return;
  }

  const blob = new Blob([exportResult.content], {
    type: exportResult.content_type || 'text/html;charset=UTF-8',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = exportResult.filename || 'career-report.html';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function appendBlock(block: string) {
  const prefix = editableMarkdown.value.trim().length ? '\n\n' : '';
  editableMarkdown.value = `${editableMarkdown.value}${prefix}${block}`.trim();
}

function insertHeading(title: string) {
  appendBlock(`${title}\n- 结论：\n- 依据：\n- 下一步：`);
}

function insertBulletList() {
  appendBlock(`- 关键观察：\n- 主要差距：\n- 风险提醒：`);
}

function insertActionBlock() {
  appendBlock(`## 下一步行动计划补充\n### 短期\n- \n### 中期\n- \n### 提醒\n- `);
}

onMounted(() => {
  void bootstrap();
});

onUnmounted(() => {
  // 重置所有 loading 状态，防止离开页面后按钮仍然被禁用
  reportStore.isLoading = false;
  reportStore.toolbarLoading = false;
});

watch(
  () => reportStore.report?.markdown_content,
  (value) => {
    editableMarkdown.value = value ?? '';
    isPreview.value = true;
  },
  { immediate: true },
);
</script>

<style scoped>
.report-page {
  gap: var(--space-6);
}

.report-context,
.report-context__summary,
.profile-preview-grid,
.profile-preview-summary-grid,
.report-markdown {
  display: grid;
  gap: var(--space-4);
}

.profile-preview-hero,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.profile-preview-hero,
.profile-preview-summary-grid {
  display: grid;
  gap: var(--space-3);
}

.profile-preview-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  padding: 1.5rem;
  border: 2px solid rgba(16, 185, 129, 0.15);
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.08), rgba(59, 130, 246, 0.04));
  position: relative;
  overflow: hidden;
}

.profile-preview-hero::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.1), transparent);
  border-radius: 50%;
}

.profile-preview-hero__main {
  display: grid;
  gap: 0.5rem;
  position: relative;
  z-index: 1;
}

.profile-preview-hero__main p,
.profile-preview-hero__main strong {
  margin: 0;
}

.profile-preview-summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.profile-preview-block,
.profile-preview-ability-card,
.profile-evidence-item {
  display: grid;
  gap: 0.75rem;
  padding: 1.25rem;
  border: 2px solid rgba(226, 232, 240, 0.8);
  border-radius: 18px;
  background: linear-gradient(135deg, #FFFFFF, rgba(248, 250, 252, 0.95));
  transition: all 0.3s ease;
}

.profile-preview-block:hover,
.profile-preview-ability-card:hover,
.profile-evidence-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px -12px rgba(0, 0, 0, 0.1);
  border-color: rgba(79, 70, 229, 0.2);
}

.profile-preview-ability-card--hard {
  border-color: rgba(79, 70, 229, 0.2);
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.06), white);
}

.profile-preview-ability-card--soft {
  border-color: rgba(245, 158, 11, 0.2);
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.06), white);
}

.profile-preview-block p,
.profile-preview-ability-card p,
.profile-evidence-item p {
  margin: 0;
}

.profile-preview-block__content {
  display: grid;
  gap: 0.5rem;
}

.profile-preview-block__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.profile-preview-ability-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-3);
}

.profile-evidence-list {
  display: grid;
  gap: var(--space-3);
}

.profile-evidence-item__subline {
  color: #64748b;
}

.tag-list--compact {
  gap: 0.5rem;
}

.report-context {
  padding: 1.5rem;
  border: 2px solid rgba(59, 130, 246, 0.15);
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05), rgba(16, 185, 129, 0.03));
}

.report-context__summary {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.report-context__label {
  margin: 0 0 0.5rem;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.report-context__summary p,
.report-context__impact p,
.report-context__impact strong {
  margin: 0;
}

.report-context__meta,
.report-markdown__toolbar,
.report-markdown__quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.report-context__impact {
  display: flex;
  gap: 1rem;
  padding: 1.25rem;
  border-radius: 18px;
  border: 2px dashed rgba(79, 70, 229, 0.15);
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.06), white);
  align-items: flex-start;
}

.report-context__impact-icon {
  font-size: 2rem;
  flex-shrink: 0;
}

.report-markdown__textarea {
  min-height: 540px;
}

.report-markdown__preview {
  min-height: 540px;
  padding: 1.5rem;
  border-radius: 18px;
  border: 2px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(135deg, white, rgba(248, 250, 252, 0.95));
  line-height: 1.8;
}

.report-markdown__actions {
  display: flex;
  justify-content: flex-end;
}

.scoring-loading {
  padding: 3rem 0;
}

.scoring-container {
  display: grid;
  gap: 2rem;
}

.scoring-card-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
}

.scoring-card {
  padding: 1.5rem;
  border-radius: 16px;
  border: 2px solid rgba(226, 232, 240, 0.9);
  background: white;
  display: grid;
  gap: 1rem;
}

.scoring-card--completeness {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08), white);
  border-color: rgba(59, 130, 246, 0.3);
}

.scoring-card--competitiveness {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.08), white);
  border-color: rgba(34, 197, 94, 0.3);
}

.scoring-card__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.scoring-card__icon {
  font-size: 1.5rem;
}

.scoring-card__label {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
}

.scoring-card__subtitle {
  margin: 0;
  font-size: 0.85rem;
  color: #64748b;
}

.scoring-card__score {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
}

.scoring-card__score-value {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1;
}

.scoring-card--completeness .scoring-card__score-value {
  color: #3b82f6;
}

.scoring-card--competitiveness .scoring-card__score-value {
  color: #22c55e;
}

.scoring-card__score-unit {
  font-size: 1rem;
  font-weight: 500;
  color: #64748b;
}

.scoring-card__progress {
  width: 100%;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.scoring-card__progress-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.scoring-card--completeness .scoring-card__progress-bar {
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
}

.scoring-card--competitiveness .scoring-card__progress-bar {
  background: linear-gradient(90deg, #22c55e, #4ade80);
}

.scoring-details {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
}

.scoring-detail-section {
  padding: 1rem;
  border-radius: 12px;
  background: #f8fafc;
}

.scoring-detail__title {
  margin: 0 0 0.75rem 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
}

.scoring-detail-list {
  display: grid;
  gap: 0.5rem;
  margin: 0;
  padding-left: 1.25rem;
}

.scoring-detail-item {
  font-size: 0.9rem;
  color: #475569;
  line-height: 1.5;
}

.example-report-section {
  display: grid;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.05), rgba(248, 250, 252, 0.95));
  border: 2px solid rgba(79, 70, 229, 0.15);
  transition: all 0.3s ease;
}

.example-report-section:hover {
  border-color: rgba(79, 70, 229, 0.3);
  box-shadow: 0 8px 24px -8px rgba(79, 70, 229, 0.2);
}

.example-report-header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.example-report-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: white;
  font-size: 1.5rem;
  box-shadow: 0 4px 12px -4px rgba(79, 70, 229, 0.4);
  flex-shrink: 0;
}

.example-report-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
}

.example-report-desc {
  margin: 0.25rem 0 0 0;
  font-size: 0.95rem;
}

.example-report-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 860px) {
  .report-context__summary,
  .profile-preview-summary-grid,
  .profile-preview-ability-grid,
  .profile-preview-hero,
  .scoring-card-group,
  .scoring-details {
    grid-template-columns: 1fr;
  }
}

.mock-report-preview {
  display: grid;
  gap: var(--space-4);
}

.mock-report-sections {
  display: grid;
  gap: var(--space-6);
}

.mock-report-section {
  display: grid;
  gap: var(--space-4);
  padding: 1.5rem;
  border-radius: 20px;
  border: 2px solid rgba(226, 232, 240, 0.9);
  background: white;
  transition: all 0.4s ease;
}

.mock-report-section:hover {
  border-color: rgba(79, 70, 229, 0.3);
  transform: translateY(-4px);
  box-shadow: 0 12px 32px -12px rgba(0, 0, 0, 0.1);
}

.mock-report-section--highlight {
  border-color: rgba(245, 158, 11, 0.3);
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08), rgba(248, 250, 252, 0.95));
}

.mock-report-section__header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.mock-report-section__number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: white;
  font-weight: 700;
  font-size: 1.1rem;
  flex-shrink: 0;
  box-shadow: 0 4px 12px -4px rgba(79, 70, 229, 0.4);
}

.mock-report-section--highlight .mock-report-section__number {
  background: linear-gradient(135deg, #f59e0b, #f97316);
  box-shadow: 0 4px 12px -4px rgba(245, 158, 11, 0.4);
}

.mock-report-section__title {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 700;
  color: #1e293b;
}

.mock-report-section__content {
  display: grid;
  gap: var(--space-4);
  margin-left: 0;
}

.mock-profile-summary {
  display: grid;
  gap: var(--space-4);
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: stretch;
}

.mock-summary-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: var(--space-5);
  align-items: center;
}

.mock-summary-card__score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: 1.5rem;
  background: white;
  border-radius: 20px;
  box-shadow: 0 8px 24px -8px rgba(0, 0, 0, 0.1);
  border: 2px solid rgba(79, 70, 229, 0.15);
}

.mock-summary-score__main {
  display: flex;
  align-items: baseline;
  gap: var(--space-1);
}

.mock-summary-score__number {
  font-size: 3rem;
  font-weight: 800;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.mock-summary-score__label {
  font-size: 1.25rem;
  color: #64748b;
}

.mock-summary-score__grade {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: white;
  font-size: 1.8rem;
  font-weight: 800;
  box-shadow: 0 8px 24px -8px rgba(245, 158, 11, 0.4);
}

.mock-summary-card__details {
  display: grid;
  gap: var(--space-3);
}

.mock-summary-item {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  background: white;
  border-radius: 16px;
  border-left: 4px solid #e2e8f0;
  transition: all 0.3s ease;
}

.mock-summary-item:hover {
  transform: translateX(4px);
}

.mock-summary-item--positive {
  border-left-color: #10b981;
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.08), white);
}

.mock-summary-item--improve {
  border-left-color: #f59e0b;
  background: linear-gradient(90deg, rgba(245, 158, 11, 0.08), white);
}

.mock-summary-item__icon {
  font-size: 1.75rem;
  flex-shrink: 0;
}

.mock-radar-chart {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: 1.5rem;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.04), white);
  border-radius: 20px;
  border: 2px solid rgba(79, 70, 229, 0.12);
  position: relative;
}

.mock-radar-svg {
  width: 100%;
  max-width: 250px;
  height: auto;
}

.mock-radar-placeholder-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.mock-radar-icon {
  font-size: 2.5rem;
}

.mock-radar-placeholder-text p {
  margin: 0;
  font-size: 0.95rem;
  color: #64748b;
}

.mock-suggestions-list {
  display: grid;
  gap: var(--space-3);
}

.mock-suggestion-item {
  display: flex;
  gap: var(--space-4);
  padding: 1.25rem 1.5rem;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.04), white);
  border-radius: 18px;
  border-left: 4px solid #4f46e5;
  transition: all 0.3s ease;
}

.mock-suggestion-item:hover {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), white);
  transform: translateX(6px);
  box-shadow: 0 4px 16px -4px rgba(79, 70, 229, 0.15);
}

.mock-suggestion-item__number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: white;
  font-size: 1rem;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 4px 12px -4px rgba(79, 70, 229, 0.3);
}

.mock-suggestion-item__content {
  display: grid;
  gap: var(--space-1);
}

.mock-suggestion-item__content strong {
  color: #1e293b;
  font-size: 1.1rem;
}

.mock-suggestion-item__content p {
  color: #64748b;
  line-height: 1.7;
}

.mock-path-mini {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  padding: 1.5rem;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.06), white);
  border-radius: 20px;
  border: 2px solid rgba(16, 185, 129, 0.15);
}

.mock-path-mini__item {
  display: grid;
  gap: var(--space-2);
  text-align: center;
  padding: 1.25rem;
  border-radius: 16px;
  background: white;
  border: 2px solid rgba(226, 232, 240, 0.9);
  transition: all 0.3s ease;
}

.mock-path-mini__item:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px -4px rgba(0, 0, 0, 0.1);
}

.mock-path-mini__item--current {
  border-color: rgba(79, 70, 229, 0.3);
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), white);
  transform: scale(1.08);
  box-shadow: 0 8px 24px -8px rgba(79, 70, 229, 0.25);
}

.mock-path-mini__badge {
  display: inline-flex;
  padding: var(--space-1) var(--space-3);
  border-radius: 50px;
  background: linear-gradient(135deg, rgba(226, 232, 240, 0.9), rgba(148, 163, 184, 0.1));
  color: #475569;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.mock-path-mini__item--current .mock-path-mini__badge {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: white;
  box-shadow: 0 4px 12px -4px rgba(79, 70, 229, 0.3);
}

.mock-path-mini__info {
  display: grid;
  gap: var(--space-1);
}

.mock-path-mini__info strong {
  color: #1e293b;
  font-size: 1.05rem;
  font-weight: 700;
}

.mock-path-mini__salary {
  font-size: 0.95rem;
  color: #f97316;
  font-weight: 700;
}

.mock-path-mini__arrow {
  font-size: 1.8rem;
  color: #94a38b;
  animation: bounceRight 2s ease-in-out infinite;
}

@keyframes bounceRight {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(6px); }
}

.report-header-actions {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.report-action-btn {
  padding: 0.875rem 1.5rem;
  border-radius: 14px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
}

.report-action-btn--primary {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: white;
  box-shadow: 0 4px 12px -4px rgba(79, 70, 229, 0.4);
}

.report-action-btn--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px -6px rgba(79, 70, 229, 0.5);
}

.report-action-btn--secondary {
  background: white;
  color: #475569;
  border: 2px solid rgba(226, 232, 240, 0.9);
}

.report-action-btn--secondary:hover {
  border-color: rgba(79, 70, 229, 0.3);
  transform: translateY(-2px);
}

.report-action-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px -4px rgba(0, 0, 0, 0.1);
}

.report-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f1f5f9;
  color: #94a38b;
}

.badge {
  padding: 0.4rem 0.8rem;
  border-radius: 50px;
  font-size: 0.85rem;
  font-weight: 600;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.12), rgba(79, 70, 229, 0.08));
  color: #4f46e5;
  border: 1px solid rgba(79, 70, 229, 0.2);
  transition: all 0.3s ease;
}

.report-bottom-cta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding: 1.5rem;
  margin-top: var(--space-4);
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.06), rgba(79, 70, 229, 0.03));
  border: 2px dashed rgba(79, 70, 229, 0.2);
}

.report-bottom-cta p {
  margin: 0;
  color: #475569;
}

@media (max-width: 768px) {
  .report-header-actions {
    flex-direction: column;
  }

  .mock-summary-card {
    grid-template-columns: 1fr;
  }

  .mock-path-mini {
    flex-direction: column;
    gap: var(--space-2);
  }

  .mock-path-mini__arrow {
    transform: rotate(90deg);
  }

  .report-bottom-cta {
    flex-direction: column;
    text-align: center;
  }
}
</style>
