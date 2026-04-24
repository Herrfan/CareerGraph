<template>
  <div class="mock-preview-content">
    <div class="mock-preview-content__header">
      <div class="mock-preview-content__info">
        <p class="mock-preview-content__label">预览数据</p>
        <strong>张三</strong>
        <p class="text-secondary">浙江工业大学 | 软件工程 | 本科</p>
      </div>
      <div class="tag-list">
        <span class="badge">前端开发</span>
        <span class="badge badge--soft">技能 6</span>
        <span class="badge badge--soft">证书 3</span>
      </div>
    </div>

    <div class="preview-summary-grid">
      <div class="mock-preview-content__block">
      <p class="mock-preview-content__label">基本信息</p>
      <div class="mock-preview-content__content">
        <p>姓名：张三</p>
        <p>学历：本科</p>
        <p>专业：软件工程</p>
        <p>学校：浙江工业大学</p>
      </div>
    </div>

    <div class="mock-preview-content__block">
      <p class="mock-preview-content__label">求职偏好</p>
      <div class="mock-preview-content__content">
        <p>岗位：前端开发</p>
        <p>薪资：8K-12K</p>
        <p>城市：杭州</p>
      </div>
    </div>
  </div>

    <div class="preview-summary-grid">
      <div class="mock-preview-content__block">
        <p class="mock-preview-content__label">技能标签</p>
        <div class="tag-list">
          <span class="badge">Vue</span>
          <span class="badge">TypeScript</span>
          <span class="badge">React</span>
          <span class="badge">HTML/CSS</span>
          <span class="badge">Node.js</span>
          <span class="badge">Git</span>
        </div>
      </div>

      <div class="mock-preview-content__block">
        <p class="mock-preview-content__label">证书与荣誉</p>
        <div class="tag-list">
          <span class="badge badge--soft">软考初级</span>
          <span class="badge badge--soft">校奖学金</span>
        </div>
      </div>
    </div>

    <div class="mock-preview-content__block">
      <p class="mock-preview-content__label">能力描述</p>
      <div class="mock-ability-grid">
        <article class="mock-ability-card">
          <strong>硬实力</strong>
          <p>熟练使用 Vue3 + TypeScript，能够独立完成前端页面开发与基本的状态管理与组件封装，了解前端工程化与性能优化。</p>
        </article>
        <article class="mock-ability-card">
          <strong>软实力</strong>
          <p>学习能力强，能够快速掌握新技术；沟通协作顺畅，与团队协作；能够独立推进项目。</p>
        </article>
      </div>
    </div>

    <div class="mock-preview-content__block">
      <div class="mock-preview-content__topline">
        <p class="mock-preview-content__label">实习经历</p>
        <span class="mock-preview-content__count">1</span>
      </div>
      <div class="experience-list">
        <article class="experience-item">
          <div class="experience-item__topline">
            <strong>某科技公司</strong>
            <span class="badge badge--neutral">2025.06 - 2025.09</span>
          </div>
          <p class="experience-item__role">前端开发实习生</p>
          <p class="experience-item__detail">参与公司内部管理系统的前端开发，负责模块页面开发与基本的状态管理与组件封装，独立完成了用户管理模块的前后端开发工作。</p>
        </article>
      </div>
    </div>

    <div class="mock-preview-content__block">
      <div class="mock-preview-content__topline">
        <p class="mock-preview-content__label">项目经历</p>
        <span class="mock-preview-content__count">2</span>
      </div>
      <div class="experience-list">
        <article class="experience-item">
          <div class="experience-item__topline">
            <strong>个人简历解析系统</strong>
            <span class="badge badge--neutral">独立开发者</span>
          </div>
          <p class="experience-item__detail">基于 Vue3 + TypeScript + Spring Boot 的个人项目，实现了基本的简历解析与简历管理等功能。</p>
          <div class="tag-list tag-list--compact">
            <span class="badge badge--soft">Vue</span>
            <span class="badge badge--soft">TypeScript</span>
            <span class="badge badge--soft">Spring Boot</span>
          </div>
        </article>
        <article class="experience-item">
          <div class="experience-item__topline">
            <strong>校园二手交易平台</strong>
            <span class="badge badge--neutral">前端开发</span>
          </div>
          <p class="experience-item__detail">负责前端页面开发与基本的状态管理与组件封装，使用 Vue3 + TypeScript，使用 Element Plus 组件库，独立完成了用户管理模块的前后端开发工作。</p>
          <div class="tag-list tag-list--compact">
            <span class="badge badge--soft">Vue</span>
            <span class="badge badge--soft">TypeScript</span>
            <span class="badge badge--soft">Element Plus</span>
          </div>
        </article>
      </div>
    </div>

    <section class="mock-preview-impact">
      <p class="mock-preview-impact__label">注意</p>
      <p class="text-secondary">这是模拟数据，仅用于预览页面效果。上传你的简历后，系统会生成专属于你的画像与职业分析。</p>
    </section>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.mock-preview-content {
  display: grid;
  gap: var(--space-4);
}

.mock-preview-content__header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  padding: 1rem 1.05rem;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, var(--bg-orange-soft), var(--bg-surface));
}

.mock-preview-content__label {
  margin: 0;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.mock-preview-content__info {
  display: grid;
  gap: 0.35rem;
}

.mock-preview-content__info strong {
  font-size: 1.25rem;
}

.mock-preview-content__block {
  display: grid;
  gap: var(--space-3);
  padding: 0.95rem 1rem;
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
  background: var(--bg-surface);
}

.mock-preview-content__content {
  display: grid;
  gap: 0.45rem;
}

.mock-preview-content__content p {
  margin: 0;
}

.mock-preview-content__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.mock-preview-content__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.75rem;
  height: 1.75rem;
  padding: 0 0.5rem;
  border-radius: 999px;
  color: var(--accent-orange);
  font-size: 0.82rem;
  font-weight: 700;
  background: var(--bg-orange-hover);
}

.mock-preview-summary-grid {
  display: grid;
  gap: var(--space-3);
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mock-ability-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-3);
}

.mock-ability-card {
  display: grid;
  gap: var(--space-2);
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  background: var(--bg-muted);
}

.mock-preview-impact {
  display: grid;
  gap: 0.45rem;
  padding: 1rem 1.05rem;
  border-radius: 14px;
  border: 1px dashed var(--border-orange);
  background: var(--bg-orange-soft);
}

.mock-preview-impact p {
  margin: 0;
}

.mock-preview-impact__label {
  margin: 0;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--accent-orange);
}

.badge--soft {
  background: var(--bg-orange-soft);
  color: var(--accent-orange);
}

.badge--neutral {
  background: var(--bg-muted);
  color: var(--text-secondary);
}

.experience-list {
  display: grid;
  gap: var(--space-3);
}

.experience-item {
  display: grid;
  gap: var(--space-2);
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  background: var(--bg-muted);
}

.experience-item strong {
  font-size: 0.96rem;
}

.experience-item p {
  margin: 0;
}

.experience-item__role {
  color: var(--text-secondary);
  font-weight: 600;
}

.experience-item__detail {
  color: var(--text-primary);
  line-height: 1.6;
}

.experience-item__highlight {
  color: var(--text-secondary);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.tag-list--compact {
  gap: 0.45rem;
}

@media (max-width: 960px) {
  .mock-preview-summary-grid,
  .mock-ability-grid,
  .mock-preview-content__header {
    grid-template-columns: 1fr;
  }
}
</style>
