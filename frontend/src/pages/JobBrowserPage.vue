<template>
  <AppShell>
    <section class="page-grid jobs-page">
      <PageHeader
        eyebrow="Jobs"
        title="岗位族浏览"
        description="如果你还在比较不同方向，可以先从这里看看每个岗位族大致在做什么、以后通常会怎么发展。"
      >
        <template #actions>
          <AppButton variant="secondary" @click="loadJobs">刷新岗位族</AppButton>
          <AppButton @click="handleImport">重新加载画像</AppButton>
        </template>
      </PageHeader>

      <!-- 顶部引导横幅 -->
      <div class="hero-banner">
        <div class="hero-banner__content">
          <div class="hero-banner__icon">
            <span class="hero-banner-icon-emoji">🧭</span>
          </div>
          <div class="hero-banner__text">
            <h2 class="hero-banner__title">🔍 探索岗位宇宙 · 11个方向等你解锁</h2>
            <p class="hero-banner__desc">还在纠结选什么？先看看不同岗位在做什么、怎么发展。</p>
          </div>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-buttons">
          <button
            v-for="category in filterCategories"
            :key="category.key"
            :class="['filter-button', activeFilter === category.key && 'filter-button--active']"
            @click="activeFilter = category.key"
          >
            {{ category.label }}
          </button>
        </div>
      </div>

      <!-- 岗位卡片瀑布流 -->
      <div class="cards-section">
        <LoadingBlock v-if="isLoading" label="正在加载岗位族..." />
        <p v-else-if="errorMessage" class="jobs-error">{{ errorMessage }}</p>
        <EmptyState
          v-else-if="jobs.length === 0"
          title="暂无岗位族数据"
          description="岗位画像还没准备好，稍后再试一次。"
        />
        <div v-else class="job-cards-grid">
          <div
            v-for="(job, index) in jobs.filter(j => isJobVisible(j.title, activeFilter))"
            :key="job.job_id"
            :class="['job-card', selectedJob?.job_id === job.job_id && 'job-card--active']"
            @click="selectJob(job)"
          >
            <!-- 顶部：名称 + 彩色小圆点 -->
            <div class="job-card__top">
              <div class="job-card__title-row">
                <div class="job-card__dot" :style="{ background: getJobColor(job.title) }"></div>
                <h3 class="job-card__name">{{ job.title }}</h3>
              </div>
            </div>

            <!-- 描述区 -->
            <p class="job-card__desc">{{ getJobShortDesc(job.title) }}</p>

            <!-- 标签区 -->
            <div class="job-card__tags">
              <span
                v-for="tag in getJobTags(job.title)"
                :key="tag"
                class="job-card__tag"
              >
                {{ tag }}
              </span>
            </div>

            <!-- 薪资和城市 -->
            <div class="job-card__bottom">
              <div class="job-card__salary">
                <span class="job-card__salary-value">{{ formatSalary(job.salary_range) }}</span>
              </div>
              <div class="job-card__city-tier">
                <span class="job-card__city-label">{{ job.city_tier || '全国' }}</span>
              </div>
            </div>

            <!-- 查看详情按钮 -->
            <div class="job-card__footer">
              <button class="job-card__view-btn" @click.stop="handleViewDetails(job)">
                <span>查看详情</span>
                <svg class="job-card__arrow" width="18" height="18" viewBox="0 0 18 18" fill="none">
                  <path d="M5.25 9h7.5M10.5 6.75 13.5 9l-3 2.25M5.25 9 8.25 6.75" stroke="#4F46E5" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 详情展示区 -->
      <div v-if="selectedJob" class="detail-panel" ref="detailPanelRef">
        <div class="detail-panel__header">
          <div class="detail-panel__title-section">
            <span class="detail-panel__icon">{{ getJobIcon(selectedJob.title) }}</span>
            <div class="detail-panel__title-wrap">
              <h2 class="detail-panel__title">{{ selectedJob.title }}</h2>
              <p class="detail-panel__subtitle">{{ selectedJob.description }}</p>
            </div>
          </div>
          <div class="detail-panel__actions">
            <button class="detail-panel__back-to-top" @click="scrollToTop">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                <path d="M9 15V3M3 9l6-6 6 6" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              回到顶部
            </button>
            <button class="detail-panel__close" @click="selectedJob = null">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M5 5l10 10M15 5l-10 10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 新布局：能力模型 + 职业阶梯 -->
        <div class="detail-panel__content">
          <!-- 左侧：能力模型 -->
          <div class="detail-panel__ability-section">
            <div class="detail-panel__section-header">
              <h3 class="detail-panel__section-title">💪 能力模型</h3>
              <p class="detail-panel__section-desc">该岗位族各项能力的权重占比</p>
            </div>
            <div class="ability-list">
              <div v-for="item in getAbilityItems(selectedJob)" :key="item.key" class="ability-item">
                <div class="ability-item__header">
                  <span class="ability-item__label">{{ item.label }}</span>
                  <span class="ability-item__score">{{ item.score }}%</span>
                </div>
                <div class="ability-item__bar">
                  <div
                    class="ability-item__bar-fill"
                    :style="{ width: `${item.score}%`, background: item.color }"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧：职业成长阶梯 -->
          <div class="detail-panel__timeline-section">
            <div class="detail-panel__section-header">
              <h3 class="detail-panel__section-title">🚀 职业成长阶梯</h3>
              <p class="detail-panel__section-desc">该岗位族的典型发展路径</p>
            </div>
            <LoadingBlock v-if="detailLoading" label="正在生成岗位族纵向路径..." />
            <div v-else-if="getTimelineItems(careerPath).length" class="timeline">
              <div
                v-for="(item, index) in getTimelineItems(careerPath)"
                :key="item.index"
                class="timeline-item"
                :class="{ 'timeline-item--last': index === getTimelineItems(careerPath).length - 1 }"
              >
                <div class="timeline-item__dot"></div>
                <div class="timeline-item__line"></div>
                <div class="timeline-item__content">
                  <div class="timeline-item__header">
                    <span class="timeline-item__phase">{{ item.phase }}</span>
                    <span class="timeline-item__salary">{{ item.salary }}</span>
                  </div>
                  <h4 class="timeline-item__title">{{ item.title }}</h4>
                  <p class="timeline-item__desc">{{ item.description }}</p>
                  <div v-if="item.skills.length" class="timeline-item__skills">
                    <span v-for="skill in item.skills" :key="skill" class="timeline-item__skill">
                      {{ skill }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <EmptyState
              v-else
              title="暂无纵向路径"
              description="这个方向的成长路径暂时还没有整理出来。"
            />
          </div>
        </div>
      </div>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '@/components/layout/AppShell.vue'
import AppButton from '@/components/ui/AppButton.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import LoadingBlock from '@/components/ui/LoadingBlock.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import { getJobs, importJobs } from '@/services/jobs'
import { generateCareerPath } from '@/services/retrieve'
import type { CareerPathResponse, JobProfile } from '@/types/api'

const jobs = ref<JobProfile[]>([])
const selectedJob = ref<JobProfile | null>(null)
const careerPath = ref<CareerPathResponse | null>(null)
const isLoading = ref(false)
const detailLoading = ref(false)
const errorMessage = ref('')
const detailPanelRef = ref<HTMLElement | null>(null)

// 筛选分类
const filterCategories = [
  { key: 'all', label: '全部' },
  { key: 'dev', label: '开发类' },
  { key: 'product', label: '产品类' },
  { key: 'test', label: '测试/运维类' }
]

const activeFilter = ref('all')

// 岗位族图标映射
const jobIcons: Record<string, string> = {
  'C/C++': '⚙️',
  'Java': '☕',
  '前端': '🎨',
  '后端': '🔧',
  '产品': '💡',
  '设计': '✨',
  '测试': '🧪',
  '运维': '🛠️',
  '数据分析': '📊',
  '算法': '🔢',
  '项目管理': '📋',
  '大数据': '📈'
}

// 岗位分类映射（用于筛选）
const jobCategoryMap: Record<string, string> = {
  'C/C++': 'dev',
  'Java': 'dev',
  '前端': 'dev',
  '后端': 'dev',
  '大数据': 'dev',
  '算法': 'dev',
  '产品': 'product',
  '设计': 'product',
  '项目管理': 'product',
  '测试': 'test',
  '运维': 'test'
}

// 小圆点颜色映射
const dotColors: Record<string, string> = {
  'C/C++': '#3B82F6',
  'Java': '#F97316',
  '产品': '#10B981',
  '前端': '#8B5CF6',
  '后端': '#0F172A',
  '测试': '#64748B',
  '运维': '#64748B',
  '设计': '#EC4899',
  '数据分析': '#EAB308',
  '算法': '#06B6D4',
  '项目管理': '#7C3AED',
  '大数据': '#22C55E'
}

function getJobColor(title: string): string {
  for (const [key, value] of Object.entries(dotColors)) {
    if (title.includes(key)) return value
  }
  return '#64748B'
}

function getJobCategory(title: string): string {
  for (const [key, value] of Object.entries(jobCategoryMap)) {
    if (title.includes(key)) return value
  }
  return 'dev'
}

function isJobVisible(title: string, filter: string): boolean {
  if (filter === 'all') return true
  return getJobCategory(title) === filter
}

// 岗位族简短描述
const jobShortDescs: Record<string, string> = {
  'C/C++': '深入底层，构建高性能系统',
  'Java': '企业级应用开发，稳健可靠',
  '前端': '用户界面，交互体验',
  '产品': '用户需求，产品规划',
  '设计': '视觉创意，用户体验',
  '测试': '质量保障，自动化',
  '运维': '系统稳定，高效运维',
  '数据分析': '数据洞察，商业智能',
  '算法': '智能核心，技术突破',
  '项目管理': '项目推进，团队协作',
  '大数据': '海量数据处理'
}

// 岗位标签
const jobTagMaps: Record<string, string[]> = {
  'C/C++': ['系统编程', '性能优化', '物联网'],
  'Java': ['Spring', '微服务', '企业应用'],
  '前端': ['Vue', 'React', '交互设计'],
  '产品': ['用户研究', '需求分析', '产品设计'],
  '设计': ['UI/UX', '视觉', '原型设计'],
  '测试': ['自动化', '性能测试', '质量保障'],
  '运维': ['DevOps', '容器化', 'CI/CD'],
  '数据分析': ['Python', 'SQL', '数据可视化'],
  '算法': ['机器学习', '深度学习', 'NLP'],
  '项目管理': ['敏捷开发', '风险控制', '团队协调'],
  '大数据': ['Hadoop', 'Spark', '实时计算']
}

function getJobIcon(title: string): string {
  for (const [key, value] of Object.entries(jobIcons)) {
    if (title.includes(key)) return value
  }
  return '💼'
}

function getJobShortDesc(title: string): string {
  for (const [key, value] of Object.entries(jobShortDescs)) {
    if (title.includes(key)) return value
  }
  return '专业技术岗位'
}

function getJobTags(title: string): string[] {
  for (const [key, value] of Object.entries(jobTagMaps)) {
    if (title.includes(key)) return value
  }
  return ['技术开发', '团队协作']
}

function formatSalary(salary: string | undefined): string {
  if (!salary) return '薪资面议'
  return salary.replace(/[Kk]/g, '000').replace('000000', '000K')
}

// 能力标签映射
const abilityLabels: Record<string, string> = {
  professional_skills: '专业技能',
  certificates: '证书要求',
  innovation: '创新能力',
  learning: '学习能力',
  stress_tolerance: '抗压能力',
  communication: '沟通能力',
  internship: '实践能力',
  teamwork: '团队协作',
  execution: '执行推进',
  problem_solving: '问题分析',
  responsibility: '责任意识'
}

// 能力颜色映射
const abilityColors: Record<string, string> = {
  professional_skills: '#7c3aed',
  certificates: '#0ea5e9',
  innovation: '#eab308',
  learning: '#22c55e',
  stress_tolerance: '#ec4899',
  communication: '#0d9488',
  internship: '#f97316',
  teamwork: '#8b5cf6',
  execution: '#64748b',
  problem_solving: '#0f172a',
  responsibility: '#7c2d12'
}

function getAbilityItems(job: JobProfile | null) {
  if (!job) {
    return []
  }

  // 岗位权重数据库
  const jobWeights: Record<string, Record<string, number>> = {
    'C/C++开发': {
      '专业技能': 25,
      '实习能力': 18,
      '学习能力': 15,
      '抗压能力': 12,
      '沟通能力': 10,
      '创新能力': 10,
      '证书要求': 10
    },
    'Java开发': {
      '专业技能': 24,
      '实习能力': 18,
      '学习能力': 16,
      '抗压能力': 11,
      '沟通能力': 11,
      '创新能力': 10,
      '证书要求': 10
    },
    '前端开发': {
      '专业技能': 22,
      '实习能力': 18,
      '学习能力': 16,
      '抗压能力': 10,
      '沟通能力': 13,
      '创新能力': 11,
      '证书要求': 10
    },
    '测试工程师': {
      '专业技能': 20,
      '实习能力': 17,
      '学习能力': 14,
      '抗压能力': 14,
      '沟通能力': 14,
      '创新能力': 11,
      '证书要求': 10
    },
    '硬件测试': {
      '专业技能': 22,
      '实习能力': 16,
      '学习能力': 13,
      '抗压能力': 14,
      '沟通能力': 12,
      '创新能力': 11,
      '证书要求': 12
    },
    '产品专员/助理': {
      '专业技能': 15,
      '实习能力': 18,
      '学习能力': 16,
      '抗压能力': 10,
      '沟通能力': 20,
      '创新能力': 13,
      '证书要求': 8
    },
    '技术支持工程师': {
      '专业技能': 18,
      '实习能力': 12,
      '学习能力': 14,
      '抗压能力': 16,
      '沟通能力': 20,
      '创新能力': 10,
      '证书要求': 10
    },
    '项目经理/主管': {
      '专业技能': 12,
      '实习能力': 12,
      '学习能力': 14,
      '抗压能力': 17,
      '沟通能力': 22,
      '创新能力': 12,
      '证书要求': 11
    },
    '科研人员': {
      '专业技能': 18,
      '实习能力': 10,
      '学习能力': 22,
      '抗压能力': 13,
      '沟通能力': 9,
      '创新能力': 20,
      '证书要求': 8
    },
    '运维工程师': {
      '专业技能': 22,
      '实习能力': 14,
      '学习能力': 15,
      '抗压能力': 18,
      '沟通能力': 12,
      '创新能力': 10,
      '证书要求': 9
    },
    'UI/交互设计': {
      '专业技能': 24,
      '实习能力': 18,
      '学习能力': 14,
      '抗压能力': 9,
      '沟通能力': 13,
      '创新能力': 14,
      '证书要求': 8
    }
  }

  // 能力项配置
  const abilityItems = [
    { key: 'professional_skills', label: '专业技能', color: '#7c3aed' },
    { key: 'certificates', label: '证书要求', color: '#0ea5e9' },
    { key: 'innovation', label: '创新能力', color: '#eab308' },
    { key: 'learning', label: '学习能力', color: '#22c55e' },
    { key: 'stress_tolerance', label: '抗压能力', color: '#ec4899' },
    { key: 'communication', label: '沟通能力', color: '#0d9488' },
    { key: 'internship', label: '实习能力', color: '#f97316' }
  ]

  // 查找对应岗位的权重
  let jobTitle = job.title
  let matchedJob = ''
  
  // 尝试匹配岗位名称
  for (const jobName in jobWeights) {
    if (jobTitle.includes(jobName)) {
      matchedJob = jobName
      break
    }
  }
  
  // 如果找到匹配的岗位，使用其权重
  if (matchedJob && jobWeights[matchedJob]) {
    const weights = jobWeights[matchedJob]
    return abilityItems.map(item => ({
      key: item.key,
      label: item.label,
      score: weights[item.label] || 0,
      color: item.color
    }))
  }
  
  // 默认权重分配，总和为100%
  return [
    { key: 'professional_skills', label: '专业技能', score: 25, color: '#7c3aed' },
    { key: 'certificates', label: '证书要求', score: 10, color: '#0ea5e9' },
    { key: 'innovation', label: '创新能力', score: 10, color: '#eab308' },
    { key: 'learning', label: '学习能力', score: 15, color: '#22c55e' },
    { key: 'stress_tolerance', label: '抗压能力', score: 12, color: '#ec4899' },
    { key: 'communication', label: '沟通能力', score: 13, color: '#0d9488' },
    { key: 'internship', label: '实习能力', score: 15, color: '#f97316' }
  ]
}

function getTimelineItems(careerPath: CareerPathResponse | null) {
  if (!careerPath?.vertical_path) {
    return []
  }

  const nodes = careerPath.vertical_path as Record<string, any>[]

  return nodes.map((node, index) => ({
    phase: node.phaseName || `阶段 ${index + 1}`,
    title: node.jobTitle || node.title || '未标注岗位',
    description: node.jobDescription || '当前阶段暂无补充说明。',
    salary: node.referenceSalary || node.salaryRange || '薪资面议',
    skills: Array.isArray(node.requiredSkills) ? node.requiredSkills.slice(0, 4) : [],
    index
  }))
}

async function loadJobs() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    jobs.value = await getJobs(0, 50)
    if (!jobs.value.length) {
      selectedJob.value = null
      careerPath.value = null
      return
    }

    const current = selectedJob.value
      ? jobs.value.find((item) => item.job_id === selectedJob.value?.job_id) ?? jobs.value[0]
      : jobs.value[0]
    await selectJob(current)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载岗位族失败'
  } finally {
    isLoading.value = false
  }
}

async function selectJob(job: JobProfile) {
  selectedJob.value = job
  detailLoading.value = true
  try {
    careerPath.value = await generateCareerPath(job.title, null)
  } catch {
    careerPath.value = null
  } finally {
    detailLoading.value = false
  }
}

async function handleViewDetails(job: JobProfile) {
  await selectJob(job)
  // 等待 DOM 更新
  setTimeout(() => {
    if (detailPanelRef.value) {
      detailPanelRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, 100)
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function handleImport() {
  await importJobs()
  await loadJobs()
}

onMounted(() => {
  void loadJobs()
})
</script>

<style scoped>
.jobs-page {
  gap: var(--space-5);
}

/* 顶部引导横幅样式 */
.hero-banner {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.12) 0%, rgba(6, 182, 212, 0.08) 100%);
  border: 1px solid rgba(124, 58, 237, 0.15);
  border-radius: 20px;
  padding: var(--space-6);
  backdrop-filter: blur(15px);
  position: relative;
  overflow: hidden;
}

.hero-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(124, 58, 237, 0.2) 0%, transparent 70%);
  filter: blur(60px);
}

.hero-banner__content {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  position: relative;
  z-index: 1;
}

.hero-banner__icon {
  flex-shrink: 0;
}

.hero-banner-icon-emoji {
  font-size: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 16px;
  backdrop-filter: blur(8px);
}

.hero-banner__text {
  flex: 1;
}

.hero-banner__title {
  margin: 0 0 var(--space-2) 0;
  font-family: var(--font-display);
  font-size: clamp(1.4rem, 3vw, 1.8rem);
  line-height: 1.2;
  background: linear-gradient(135deg, #0A66C2 0%, #7C3AED 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-banner__desc {
  margin: 0;
  font-size: 1.05rem;
  color: var(--text-secondary);
  font-weight: 500;
}

/* 筛选栏 */
.filter-bar {
  width: 100%;
  margin-bottom: var(--space-5);
}

.filter-buttons {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.filter-button {
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid #E2E8F0;
  background: white;
  color: #64748B;
  font-weight: 500;
  font-size: 0.92rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-button:hover {
  border-color: #4F46E5;
  color: #4F46E5;
}

.filter-button--active {
  background: linear-gradient(135deg, #4F46E5 0%, #6366F1 100%);
  border-color: transparent;
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

/* 岗位卡片瀑布流 */
.cards-section {
  width: 100%;
}

.job-cards-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-5);
}

/* 岗位卡片 */
.job-card {
  background: #FFFFFF;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #F1F5F9;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.2, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  padding: var(--space-4);
}

.job-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border-color: #E2E8F0;
}

.job-card--active {
  border-color: #4F46E5;
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.15), 0 10px 25px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

/* 顶部：名称 + 彩色小圆点 */
.job-card__top {
  margin-bottom: var(--space-3);
}

.job-card__title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.job-card__dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.job-card__name {
  margin: 0;
  color: #1E293B;
  font-family: var(--font-display);
  font-size: 1.15rem;
  font-weight: 700;
  line-height: 1.3;
}

/* 描述区 */
.job-card__desc {
  margin: 0 0 var(--space-3);
  color: #64748B;
  font-size: 0.9rem;
  line-height: 1.5;
}

/* 标签区 */
.job-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.job-card__tag {
  background: #F1F5F9;
  color: #475569;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
}

/* 薪资和城市 */
.job-card__bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.job-card__salary {
  display: flex;
  align-items: baseline;
}

.job-card__salary-value {
  color: #4F46E5;
  font-weight: 700;
  font-size: 1.15rem;
}

.job-card__city-label {
  background: #F1F5F9;
  color: #64748B;
  padding: 4px 10px;
  border-radius: 999px;
  font-weight: 500;
  font-size: 0.85rem;
}

/* 查看详情按钮 */
.job-card__footer {
  border-top: 1px solid #F1F5F9;
  padding-top: var(--space-3);
}

.job-card__view-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-2);
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #4F46E5;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.job-card__view-btn:hover {
  color: #4338CA;
  background: #EEF2FF;
}

.job-card__view-btn:hover .job-card__arrow {
  transform: translateX(3px);
}

.job-card__arrow {
  transition: transform 0.2s ease;
}

/* 详情面板 */
.detail-panel {
  background: white;
  border-radius: 20px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.detail-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-5);
  border-bottom: 1px solid #f1f5f9;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.detail-panel__title-section {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.detail-panel__actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.detail-panel__back-to-top {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #4F46E5 0%, #6366F1 100%);
  color: white;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.detail-panel__back-to-top:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.detail-panel__icon {
  font-size: 2.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #7c3aed 0%, #0ea5e9 100%);
  color: white;
  border-radius: 14px;
}

.detail-panel__title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-panel__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.5rem;
  color: #0f172a;
}

.detail-panel__subtitle {
  margin: 0;
  color: #64748b;
  font-size: 0.95rem;
}

.detail-panel__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: none;
  background: white;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.detail-panel__close:hover {
  background: #e2e8f0;
}

.detail-panel__content {
  display: grid;
  grid-template-columns: minmax(0, 0.85fr) minmax(0, 1.15fr);
  gap: var(--space-5);
  padding: var(--space-5);
}

.detail-panel__section-header {
  margin-bottom: var(--space-4);
}

.detail-panel__section-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.15rem;
  color: #0f172a;
  font-weight: 700;
}

.detail-panel__section-desc {
  margin: var(--space-1) 0 0;
  font-size: 0.88rem;
  color: #64748b;
}

/* 左侧：能力模型 */
.detail-panel__ability-section {
  padding: var(--space-4);
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #f8fafc 0%, white 100%);
}

.ability-list {
  display: grid;
  gap: var(--space-3);
}

.ability-item {
  display: grid;
  gap: var(--space-2);
}

.ability-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ability-item__label {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.9rem;
}

.ability-item__score {
  font-weight: 700;
  color: #7c3aed;
  font-size: 0.9rem;
}

.ability-item__bar {
  height: 10px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.ability-item__bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.8s cubic-bezier(0.2, 0, 0.2, 1);
}

/* 右侧：职业成长阶梯 */
.detail-panel__timeline-section {
  padding: var(--space-4);
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #f8fafc 0%, white 100%);
}

.timeline {
  position: relative;
  padding-left: 32px;
}

.timeline-item {
  position: relative;
  padding-bottom: var(--space-5);
}

.timeline-item--last {
  padding-bottom: 0;
}

.timeline-item__dot {
  position: absolute;
  left: -32px;
  top: 4px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7c3aed 0%, #0ea5e9 100%);
  box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.1);
}

.timeline-item__line {
  position: absolute;
  left: -25px;
  top: 24px;
  bottom: 0;
  width: 2px;
  background: linear-gradient(180deg, rgba(124, 58, 237, 0.4), rgba(124, 58, 237, 0.1));
}

.timeline-item--last .timeline-item__line {
  display: none;
}

.timeline-item__content {
  display: grid;
  gap: var(--space-2);
  background: white;
  padding: var(--space-3);
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.timeline-item__content:hover {
  border-color: rgba(124, 58, 237, 0.3);
  box-shadow: 0 4px 16px rgba(124, 58, 237, 0.08);
}

.timeline-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.timeline-item__phase {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  background: linear-gradient(135deg, #7c3aed 0%, #0ea5e9 100%);
  color: white;
}

.timeline-item__salary {
  font-weight: 700;
  color: #f97316;
  font-size: 0.9rem;
}

.timeline-item__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: #1e293b;
}

.timeline-item__desc {
  margin: 0;
  font-size: 0.88rem;
  color: #64748b;
  line-height: 1.6;
}

.timeline-item__skills {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-1);
}

.timeline-item__skill {
  display: inline-block;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
  background: #f1f5f9;
  color: #475569;
}

.jobs-error {
  margin: 0;
  color: var(--danger-600);
}

@media (max-width: 1024px) {
  .job-cards-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .job-cards-grid {
    grid-template-columns: 1fr;
  }

  .hero-banner__content {
    flex-direction: column;
    text-align: center;
  }

  .detail-panel__content {
    grid-template-columns: 1fr;
    gap: var(--space-4);
  }
}
</style>
