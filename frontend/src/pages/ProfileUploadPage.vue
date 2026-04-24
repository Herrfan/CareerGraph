<template>
  <AppShell>
    <section class="page-grid profile-page">
      <PageHeader
        eyebrow="Profile"
        title="学生画像生成"
        description="上传简历或手动录入基础信息后，系统会先生成学生画像。期望岗位、薪资和城市都可以留空，但主动填写会影响后续推荐方向。"
      >
        <template #actions>
          <StepGuide
            storage-key="profile-upload"
            title="先把画像补齐，后面的匹配和报告才有依据"
            description="学生画像是后续岗位匹配、职业路径和职业报告的基础。这里填得越清楚，后面的结果越贴近你的实际情况。"
            trigger-eyebrow="Next Step"
            trigger-title="学生画像说明"
            trigger-summary="看看哪些信息可以留空，哪些信息会影响后续判断。"
            note="如果你填写了期望岗位、薪资或城市，系统会优先按这些偏好理解你的求职方向；如果不填，系统会尽量从简历或手动信息中推断。"
            :sections="profileGuideSections"
          />
        </template>
      </PageHeader>

      <div class="metric-grid">
        <StatCard
          label="当前状态"
          class="stat-card-status"
          value="waiting"
        >
          <template #content>
            <div class="stat-card-status__content">
              <div v-if="profileStore.studentProfile">
                <span class="stat-card-status__value">画像已生成</span>
                <StatusBadge type="success">已完成</StatusBadge>
              </div>
              <div v-else>
                <span class="stat-card-status__value">等待生成</span>
                <StatusBadge type="incomplete">未完成</StatusBadge>
              </div>
            </div>
          </template>
          <template #hint>
            建议先完成学生画像，再继续岗位匹配、职业路径和职业报告。
          </template>
        </StatCard>
        <StatCard
          label="完整度"
          class="stat-card-progress"
          value="100"
        >
          <template #content>
            <CircularProgress
              :value="profileStore.evaluation?.completeness_score ?? (showMockData ? 65 : 0)"
              label="完整度"
              :type="profileStore.evaluation ? 'primary' : 'accent'"
            />
          </template>
          <template #hint>
            反映画像字段是否足够完整。
          </template>
        </StatCard>
        <StatCard
          label="竞争力"
          class="stat-card-progress"
          value="100"
        >
          <template #content>
            <div v-if="profileStore.evaluation" class="stat-card-competitiveness">
              <span class="stat-card-competitiveness__value">{{ profileStore.evaluation.competitiveness_score }}</span>
            </div>
            <div v-else class="stat-card-competitiveness">
              <span class="stat-card-competitiveness__value stat-card-competitiveness__value--mock">B+</span>
            </div>
          </template>
          <template #hint>
            结合技能、证书和实践经历估算的竞争力指标。
          </template>
        </StatCard>
      </div>

      <div class="profile-layout">
        <CardPanel
          class="profile-layout__column profile-layout__column--input"
          eyebrow="Input"
          title="录入学生信息"
          description="优先推荐上传简历自动解析；如果简历暂时不方便上传，也可以直接手动录入。"
        >
          <div class="mode-toggle">
            <button :class="['mode-toggle__item', mode === 'upload' && 'mode-toggle__item--active']" type="button" @click="mode = 'upload'">
              简历上传
            </button>
            <button :class="['mode-toggle__item', mode === 'manual' && 'mode-toggle__item--active']" type="button" @click="mode = 'manual'">
              手动录入
            </button>
          </div>

          <form v-if="mode === 'upload'" class="form-grid" @submit.prevent="handleSubmit">
            <label class="upload-box upload-box--highlight">
              <input type="file" accept=".pdf,.docx" hidden @change="handleFileChange" />
              <span class="upload-box__eyebrow">简历文件</span>
              <strong>{{ selectedFile ? selectedFile.name : '选择 PDF / DOCX 简历文件' }}</strong>
              <span class="text-secondary">
                {{ selectedFile ? `${(selectedFile.size / 1024 / 1024).toFixed(2)} MB` : '点击后从本地选择简历文件，系统会自动完成解析。' }}
              </span>
            </label>

            <div class="form-grid columns-3">
              <select v-model="form.expectedPosition" class="select">
                <option value="">期望岗位可留空</option>
                <option v-for="option in positionOptions" :key="option" :value="option">{{ option }}</option>
              </select>
              <select v-model="form.expectedSalary" class="select">
                <option value="">期望薪资可留空</option>
                <option v-for="option in salaryOptions" :key="option" :value="option">{{ option }}</option>
              </select>
              <input v-model="form.expectedCity" class="input" placeholder="期望城市（可选），例如杭州" />
            </div>

            <section class="preference-impact">
              <p class="preference-impact__label">这三项都不是必填</p>
              <p class="text-secondary">
                不填时，系统会尽量从简历里推断；如果你主动填写，岗位匹配、职业路径和职业报告会更贴近这些偏好。
              </p>
            </section>

            <p v-if="profileStore.errorMessage" class="form-error">{{ profileStore.errorMessage }}</p>

            <div v-if="profileStore.isSubmitting" class="upload-progress">
              <div class="upload-progress__track">
                <span class="upload-progress__fill" :style="{ width: `${profileStore.uploadProgress}%` }"></span>
              </div>
              <span class="upload-progress__text">{{ profileStore.uploadProgress }}%</span>
              <span class="upload-progress__meta">
                {{ profileStore.uploadStage === 'processing' ? '正在解析简历...' : '正在上传文件...' }}
              </span>
            </div>

            <div class="submit-row">
              <AppButton type="submit" :disabled="profileStore.isSubmitting">
                {{ profileStore.isSubmitting ? '正在生成画像...' : '开始生成画像' }}
              </AppButton>
            </div>

            <div class="demo-link">
              <button type="button" @click="useExampleProfile" class="demo-link__btn">
                <span>✨</span>
                不想上传？先看看示例画像
              </button>
            </div>
          </form>

          <form v-else class="form-grid" @submit.prevent="handleManualSubmit">
            <div class="form-grid columns-2">
              <input v-model="manualForm.name" class="input" placeholder="姓名" required />
              <input v-model="manualForm.school" class="input" placeholder="学校" required />
            </div>
            <div class="form-grid columns-2">
              <input v-model="manualForm.education" class="input" placeholder="学历，例如本科" required />
              <input v-model="manualForm.major" class="input" placeholder="专业，例如软件工程" required />
            </div>
            <div class="form-grid columns-3">
              <select v-model="manualForm.expectedPosition" class="select">
                <option value="">期望岗位可留空</option>
                <option v-for="option in positionOptions" :key="option" :value="option">{{ option }}</option>
              </select>
              <select v-model="manualForm.expectedSalary" class="select">
                <option value="">期望薪资可留空</option>
                <option v-for="option in salaryOptions" :key="option" :value="option">{{ option }}</option>
              </select>
              <input v-model="manualForm.expectedCity" class="input" placeholder="期望城市（可选）" />
            </div>

            <section class="preference-impact">
              <p class="preference-impact__label">期望岗位、薪资、城市会直接影响后续推荐</p>
              <p class="text-secondary">
                已经有明确方向就尽量填清楚；还在探索也没关系，可以先留空，后面再回来修改。
              </p>
            </section>

            <textarea v-model="manualForm.skillsText" class="textarea" rows="3" placeholder="技能，请用中文逗号分隔，例如 Java，Spring Boot，MySQL" />
            <textarea v-model="manualForm.certificatesText" class="textarea" rows="2" placeholder="证书或荣誉，请用中文逗号分隔" />

            <section class="form-section">
              <div class="form-section__header">
                <div>
                  <p class="form-section__eyebrow">能力描述</p>
                  <strong>补充硬实力和软实力</strong>
                </div>
              </div>
              <div class="form-grid columns-2">
                <textarea
                  v-model="manualForm.hardSkillDescription"
                  class="textarea"
                  rows="4"
                  placeholder="硬实力描述，例如掌握哪些技术栈、能独立完成哪些开发/测试/分析任务。"
                />
                <textarea
                  v-model="manualForm.softSkillDescription"
                  class="textarea"
                  rows="4"
                  placeholder="软实力描述，例如学习速度、沟通协作、抗压能力、执行推进能力。"
                />
              </div>
            </section>

            <section class="form-section">
              <div class="form-section__header">
                <div>
                  <p class="form-section__eyebrow">实习经历</p>
                  <strong>至少填一段最能代表当前水平的经历</strong>
                </div>
                <button class="inline-action" type="button" @click="addInternshipExperience">新增一段</button>
              </div>
              <div class="entry-list">
                <article v-for="(experience, index) in manualForm.internshipExperiences" :key="`internship-${index}`" class="entry-card">
                  <div class="entry-card__header">
                    <strong>实习经历 {{ index + 1 }}</strong>
                    <button
                      v-if="manualForm.internshipExperiences.length > 1"
                      class="inline-action inline-action--danger"
                      type="button"
                      @click="removeInternshipExperience(index)"
                    >
                      删除
                    </button>
                  </div>
                  <div class="form-grid columns-2">
                    <input v-model="experience.company" class="input" placeholder="公司 / 组织" />
                    <input v-model="experience.position" class="input" placeholder="岗位 / 职责" />
                  </div>
                  <input v-model="experience.period" class="input" placeholder="时间，例如 2025.06 - 2025.09" />
                  <textarea
                    v-model="experience.achievement"
                    class="textarea"
                    rows="3"
                    placeholder="写清楚你做了什么、负责什么、结果怎样，尽量带结果和产出。"
                  />
                </article>
              </div>
            </section>

            <section class="form-section">
              <div class="form-section__header">
                <div>
                  <p class="form-section__eyebrow">项目经历</p>
                  <strong>把最有代表性的项目写出来</strong>
                </div>
                <button class="inline-action" type="button" @click="addProjectExperience">新增项目</button>
              </div>
              <div class="entry-list">
                <article v-for="(project, index) in manualForm.projectExperiences" :key="`project-${index}`" class="entry-card">
                  <div class="entry-card__header">
                    <strong>项目经历 {{ index + 1 }}</strong>
                    <button
                      v-if="manualForm.projectExperiences.length > 1"
                      class="inline-action inline-action--danger"
                      type="button"
                      @click="removeProjectExperience(index)"
                    >
                      删除
                    </button>
                  </div>
                  <div class="form-grid columns-2">
                    <input v-model="project.name" class="input" placeholder="项目名称" />
                    <input v-model="project.role" class="input" placeholder="担任角色" />
                  </div>
                  <input v-model="project.techStacksText" class="input" placeholder="技术栈，用中文逗号分隔，例如 Vue，TypeScript，Spring Boot" />
                  <textarea v-model="project.description" class="textarea" rows="3" placeholder="项目背景、目标和你负责的部分。" />
                  <textarea v-model="project.highlight" class="textarea" rows="3" placeholder="项目亮点、难点、结果或量化成果。" />
                </article>
              </div>
            </section>

            <p v-if="profileStore.errorMessage" class="form-error">{{ profileStore.errorMessage }}</p>

            <div class="submit-row">
              <AppButton type="submit" :disabled="profileStore.isSubmitting">
                {{ profileStore.isSubmitting ? '正在生成画像...' : '提交手动画像' }}
              </AppButton>
            </div>

            <div class="demo-link">
              <button type="button" @click="useExampleProfile" class="demo-link__btn">
                <span>✨</span>
                不想上传？先看看示例画像
              </button>
            </div>
          </form>
        </CardPanel>

        <MockPreviewOverlay
          v-if="!profileStore.studentProfile"
          @upload="mode = 'upload'"
          @demo="useExampleProfile"
          title="上传简历，生成你的专属职业画像"
        >
          <CardPanel
            class="profile-layout__column profile-layout__column--preview"
            eyebrow="Preview"
            title="当前学生画像"
            description="生成成功后，这里会固定展示画像摘要，方便你继续进入岗位匹配和后续报告。"
          >
            <div v-if="showMockData" class="preview-grid">
              <div class="preview-hero preview-hero--mock">
                <div class="preview-hero__main">
                  <p class="preview-block__label">画像总览</p>
                  <strong>张三</strong>
                  <p class="text-secondary">
                    浙江工业大学 | 计算机科学与技术 | 2026届
                  </p>
                </div>
                <div class="tag-list">
                  <span class="badge">Java开发</span>
                  <span class="badge badge--soft">技能 6</span>
                  <span class="badge badge--soft">证书 3</span>
                </div>
              </div>

              <div class="preview-summary-grid">
                <div class="preview-block preview-block--mock">
                  <p class="preview-block__label">基本信息</p>
                  <div class="preview-block__content">
                    <p>姓名：张三</p>
                    <p>学历：本科</p>
                    <p>专业：计算机科学与技术</p>
                    <p>学校：浙江工业大学</p>
                  </div>
                </div>

                <div class="preview-block preview-block--mock">
                  <p class="preview-block__label">求职偏好</p>
                  <div class="preview-block__content">
                    <p>岗位：Java开发</p>
                    <p>薪资：8K-12K</p>
                    <p>城市：杭州</p>
                  </div>
                </div>
              </div>

              <div class="preview-summary-grid">
                <div class="preview-block preview-block--mock">
                  <p class="preview-block__label">技能标签</p>
                  <div class="tag-list">
                    <span class="badge">Java</span>
                    <span class="badge">Spring Boot</span>
                    <span class="badge">MySQL</span>
                    <span class="badge">Vue</span>
                    <span class="badge">TypeScript</span>
                    <span class="badge">Git</span>
                  </div>
                </div>

                <div class="preview-block preview-block--mock">
                  <p class="preview-block__label">证书与荣誉</p>
                  <div class="tag-list">
                    <span class="badge badge--soft">计算机二级</span>
                    <span class="badge badge--soft">校奖学金</span>
                    <span class="badge badge--soft">英语六级</span>
                  </div>
                </div>
              </div>

              <div class="preview-block preview-block--mock">
                <p class="preview-block__label">能力描述</p>
                <div class="preview-ability-grid">
                  <article class="preview-ability-card">
                    <strong>硬实力</strong>
                    <p>熟练使用Java和Spring Boot，能独立完成Web后端开发，了解MySQL数据库设计，有一定的代码规范意识。</p>
                  </article>
                  <article class="preview-ability-card">
                    <strong>软实力</strong>
                    <p>学习能力强，能快速掌握新技术，沟通协作顺畅，有团队意识，能独立推进任务。</p>
                  </article>
                </div>
              </div>

              <div class="preview-block preview-block--mock">
                <div class="preview-block__topline">
                  <p class="preview-block__label">实习经历</p>
                  <span class="preview-block__count">1</span>
                </div>
                <div class="experience-list">
                  <article class="experience-item">
                    <div class="experience-item__topline">
                      <strong>XX科技有限公司</strong>
                      <span class="badge badge--neutral">2025.06 - 2025.09</span>
                    </div>
                    <p class="experience-item__role">Java开发实习生</p>
                    <p class="experience-item__detail">参与公司内部管理系统的后端开发，负责API接口设计与实现，使用Spring Boot + MySQL技术栈，独立完成用户模块和权限模块的开发。</p>
                  </article>
                </div>
              </div>

              <div class="preview-block preview-block--mock">
                <div class="preview-block__topline">
                  <p class="preview-block__label">项目经历</p>
                  <span class="preview-block__count">2</span>
                </div>
                <div class="experience-list">
                  <article class="experience-item">
                    <div class="experience-item__topline">
                      <strong>校园招聘系统</strong>
                      <span class="badge badge--neutral">后端开发</span>
                    </div>
                    <p class="experience-item__detail">作为团队核心成员，负责后端API接口开发和数据库设计，使用Spring Boot + MySQL，支持简历上传、职位匹配等核心功能。</p>
                    <div class="tag-list tag-list--compact">
                      <span class="badge badge--soft">Java</span>
                      <span class="badge badge--soft">Spring Boot</span>
                      <span class="badge badge--soft">MySQL</span>
                    </div>
                    <p class="experience-item__highlight">亮点：系统用户量达1000+，日活300+，获得校优秀项目奖</p>
                  </article>
                  <article class="experience-item">
                    <div class="experience-item__topline">
                      <strong>个人博客系统</strong>
                      <span class="badge badge--neutral">全栈开发</span>
                    </div>
                    <p class="experience-item__detail">独立完成的个人项目，包含用户登录、文章管理、评论功能等，使用Spring Boot + Vue技术栈。</p>
                    <div class="tag-list tag-list--compact">
                      <span class="badge badge--soft">Vue</span>
                      <span class="badge badge--soft">Spring Boot</span>
                      <span class="badge badge--soft">MySQL</span>
                    </div>
                  </article>
                </div>
              </div>

              <section class="preview-impact preview-impact--mock">
                <p class="preview-impact__label">注意</p>
                <p class="text-secondary">
                  这是模拟数据，仅用于预览页面效果。上传你的简历后，系统会生成专属于你的画像与职业分析。
                </p>
              </section>

              <div class="preview-actions">
                <AppButton variant="secondary" @click="appStore.exitDemoMode()">关闭预览</AppButton>
              </div>
            </div>

            <EmptyState
              v-else
              title="这里会显示画像结果"
              description="生成完成后，右侧会固定展示学生画像摘要，方便你继续做岗位匹配和报告分析。"
            >
              <template #actions>
                <AppButton variant="secondary" disabled>等待生成完成</AppButton>
              </template>
            </EmptyState>
          </CardPanel>
        </MockPreviewOverlay>

        <template v-else>
          <CardPanel
            class="profile-layout__column profile-layout__column--preview"
            eyebrow="Preview"
            title="当前学生画像"
            description="生成成功后，这里会固定展示画像摘要，方便你继续进入岗位匹配和后续报告。"
          >
            <div class="preview-grid">
              <div class="preview-hero">
                <div class="preview-hero__main">
                  <p class="preview-block__label">画像总览</p>
                  <strong>{{ profileStore.studentProfile.basic_info.name || '未填写姓名' }}</strong>
                  <p class="text-secondary">
                    {{ profileStore.studentProfile.basic_info.school || '学校未填' }} |
                    {{ profileStore.studentProfile.basic_info.major || '专业未填' }} |
                    {{ profileStore.studentProfile.basic_info.education || '学历未填' }}
                  </p>
                </div>
                <div class="tag-list">
                  <span class="badge">{{ profileStore.studentProfile.job_preference.expected_position || '岗位待确认' }}</span>
                  <span class="badge badge--soft">技能 {{ profileStore.studentProfile.skills.length }}</span>
                  <span class="badge badge--soft">证书 {{ profileStore.studentProfile.certificates.length }}</span>
                </div>
              </div>

              <!-- 评分展示 -->
              <div class="preview-scoring">
                <div class="preview-scoring__card">
                  <div class="preview-scoring__header">
                    <p class="preview-scoring__label">当前状态</p>
                  </div>
                  <div class="preview-scoring__content">
                    <span class="preview-scoring__value">画像已生成</span>
                    <StatusBadge type="success">已完成</StatusBadge>
                  </div>
                </div>
                <div class="preview-scoring__card">
                  <div class="preview-scoring__header">
                    <p class="preview-scoring__label">完整度</p>
                  </div>
                  <div class="preview-scoring__content">
                    <span class="preview-scoring__value">{{ profileScoring?.completeness_score ? Math.round(profileScoring.completeness_score) : 'N/A' }}</span>
                    <span class="preview-scoring__unit">分</span>
                  </div>
                </div>
                <div class="preview-scoring__card">
                  <div class="preview-scoring__header">
                    <p class="preview-scoring__label">竞争力</p>
                  </div>
                  <div class="preview-scoring__content">
                    <span class="preview-scoring__value">{{ profileScoring?.competitiveness_score ? Math.round(profileScoring.competitiveness_score) : 'N/A' }}</span>
                    <span class="preview-scoring__unit">分</span>
                  </div>
                </div>
              </div>

              <div class="preview-summary-grid">
                <div class="preview-block">
                  <p class="preview-block__label">基本信息</p>
                  <div class="preview-block__content">
                    <p>姓名：{{ profileStore.studentProfile.basic_info.name || '未填写' }}</p>
                    <p>学历：{{ profileStore.studentProfile.basic_info.education || '未填写' }}</p>
                    <p>专业：{{ profileStore.studentProfile.basic_info.major || '未填写' }}</p>
                    <p>学校：{{ profileStore.studentProfile.basic_info.school || '未填写' }}</p>
                  </div>
                </div>

                <div class="preview-block">
                  <p class="preview-block__label">求职偏好</p>
                  <div class="preview-block__content">
                    <p>岗位：{{ profileStore.studentProfile.job_preference.expected_position || '未指定' }}</p>
                    <p>薪资：{{ profileStore.studentProfile.job_preference.expected_salary || '未指定' }}</p>
                    <p>城市：{{ profileStore.studentProfile.job_preference.expected_city || '未指定' }}</p>
                  </div>
                </div>
              </div>

              <div class="preview-summary-grid">
                <div class="preview-block">
                  <p class="preview-block__label">技能标签</p>
                  <div class="tag-list">
                    <span v-for="skill in profileStore.studentProfile.skills" :key="skill" class="badge">{{ skill }}</span>
                    <span v-if="profileStore.studentProfile.skills.length === 0" class="text-secondary">暂时还没有明确技能</span>
                  </div>
                </div>

                <div class="preview-block">
                  <p class="preview-block__label">证书与荣誉</p>
                  <div class="tag-list">
                    <span v-for="certificate in profileStore.studentProfile.certificates" :key="certificate" class="badge badge--soft">{{ certificate }}</span>
                    <span v-if="profileStore.studentProfile.certificates.length === 0" class="text-secondary">暂时还没有明确证书或荣誉</span>
                  </div>
                </div>
              </div>

              <div class="preview-block">
                <p class="preview-block__label">能力描述</p>
                <div class="preview-ability-grid">
                  <article class="preview-ability-card">
                    <strong>硬实力</strong>
                    <p>{{ profileStore.studentProfile.ability_descriptions?.professional_skill || '还没有补充硬实力描述。' }}</p>
                  </article>
                  <article class="preview-ability-card">
                    <strong>软实力</strong>
                    <p>{{ formatSoftSkillDescription(profileStore.studentProfile.ability_descriptions) || '还没有补充软实力描述。' }}</p>
                  </article>
                </div>
              </div>

              <div class="preview-block">
                <div class="preview-block__topline">
                  <p class="preview-block__label">实习经历</p>
                  <span class="preview-block__count">{{ profileStore.studentProfile.internship_experiences?.length ?? 0 }}</span>
                </div>
                <div v-if="profileStore.studentProfile.internship_experiences?.length" class="experience-list">
                  <article v-for="(experience, index) in profileStore.studentProfile.internship_experiences" :key="`preview-internship-${index}`" class="experience-item">
                    <div class="experience-item__topline">
                      <strong>{{ experience.company || '未填写单位' }}</strong>
                      <span class="badge badge--neutral">{{ experience.period || '时间未填' }}</span>
                    </div>
                    <p class="experience-item__role">{{ experience.position || '岗位未填' }}</p>
                    <p class="experience-item__detail">{{ experience.achievement || '暂无详细成果描述。' }}</p>
                  </article>
                </div>
                <p v-else class="text-secondary">暂时还没有补充实习经历。</p>
              </div>

              <div class="preview-block">
                <div class="preview-block__topline">
                  <p class="preview-block__label">项目经历</p>
                  <span class="preview-block__count">{{ profileStore.studentProfile.project_experiences?.length ?? 0 }}</span>
                </div>
                <div v-if="profileStore.studentProfile.project_experiences?.length" class="experience-list">
                  <article v-for="(project, index) in profileStore.studentProfile.project_experiences" :key="`preview-project-${index}`" class="experience-item">
                    <div class="experience-item__topline">
                      <strong>{{ project.name || '未命名项目' }}</strong>
                      <span class="badge badge--neutral">{{ project.role || '角色未填' }}</span>
                    </div>
                    <p class="experience-item__detail">{{ project.description || '暂无项目描述。' }}</p>
                    <div v-if="project.tech_stacks?.length" class="tag-list tag-list--compact">
                      <span v-for="stack in project.tech_stacks" :key="`${project.name}-${stack}`" class="badge badge--soft">{{ stack }}</span>
                    </div>
                    <p v-if="project.highlight" class="experience-item__highlight">亮点：{{ project.highlight }}</p>
                  </article>
                </div>
                <p v-else class="text-secondary">暂时还没有补充项目经历。</p>
              </div>

              <section class="preview-impact">
                <p class="preview-impact__label">后续影响</p>
                <p class="text-secondary">
                  接下来岗位匹配、职业路径和职业报告都会读取这份画像。如果你发现结果偏了，优先回来修改这里的信息。
                </p>
              </section>

              <div class="preview-actions">
                <AppButton @click="router.push('/matching')">进入岗位匹配</AppButton>
              </div>
            </div>
          </CardPanel>
        </template>
      </div>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/layout/AppShell.vue';
import AppButton from '@/components/ui/AppButton.vue';
import CardPanel from '@/components/ui/CardPanel.vue';
import EmptyState from '@/components/ui/EmptyState.vue';
import PageHeader from '@/components/ui/PageHeader.vue';
import StatCard from '@/components/ui/StatCard.vue';
import StepGuide from '@/components/ui/StepGuide.vue';
import MockPreviewOverlay from '@/components/ui/MockPreviewOverlay.vue';
import StatusBadge from '@/components/ui/StatusBadge.vue';
import CircularProgress from '@/components/ui/CircularProgress.vue';
import { useProfileStore } from '@/stores/profile';
import { useReportStore } from '@/stores/report';
import { useAppStore } from '@/stores/app';
import { scoreProfile } from '@/services/report';
import type { AbilityDescriptions, InternshipExperience, ProjectExperience, StudentProfile, ProfileScoring } from '@/types/api';

const profileGuideSections = [
  {
    title: '建议先完成这一步',
    description: '学生画像会作为后续岗位匹配、职业路径和职业报告的输入基础，先把这里补齐，后面的结果才更可信。',
    tone: 'accent',
  },
  {
    title: '期望岗位、薪资、城市都可以留空',
    description: '不填不会阻止你继续使用系统，系统会尽量从简历或手动信息中推断这些偏好。',
  },
  {
    title: '主动填写会改变后续判断',
    description: '如果你主动填了期望岗位、薪资或城市，系统会优先按你的选择理解求职方向，后续建议会更贴近这些偏好。',
    tone: 'warning',
  },
] as const;

const router = useRouter();
const profileStore = useProfileStore();
const reportStore = useReportStore();
const appStore = useAppStore();
const selectedFile = ref<File | null>(null);
const mode = ref<'upload' | 'manual'>('upload');
const profileScoring = ref<ProfileScoring | null>(null);
const isScoringLoading = ref(false);

// 页面初始化时检查并加载评分
onMounted(async () => {
  if (profileStore.studentProfile) {
    await loadProfileScoring(profileStore.studentProfile);
  }
});

const showMockData = computed(() => appStore.isDemoMode);

const positionOptions = [
  'Java开发',
  '前端开发',
  'C/C++开发',
  'Python开发',
  '数据分析',
  '算法工程师',
  '软件测试',
  '硬件测试',
  '实施工程师',
  '技术支持工程师',
  '运维/DevOps',
  '产品/项目经理',
  'APP推广/运营',
  '科研人员',
  '综合开发',
];

const salaryOptions = ['3K-5K', '5K-8K', '8K-12K', '12K-18K', '15K-25K', '20K-30K', '30K+'];

// 示例学生画像数据
const exampleProfile: StudentProfile = {
  student_id: 'example-001',
  basic_info: {
    name: '范子豪',
    education: '本科',
    major: '计算机科学与技术',
    school: '浙江科技大学',
  },
  skills: ['Java', 'Python', 'MySQL', 'Vue', 'Git', 'Linux'],
  certificates: ['英语六级', '计算机二级', '校级一等奖学金'],
  soft_abilities: {
    innovation: 75,
    learning: 80,
    stress_tolerance: 70,
    communication: 75,
    professional_skills: 80,
    certificates: 70,
    internship: 65,
  },
  job_preference: {
    expected_position: 'Java开发',
    expected_salary: '8K-12K',
    expected_city: '杭州',
  },
  ability_descriptions: {
    professional_skill: '熟练掌握Java编程语言，熟悉Spring Boot、MyBatis等后端框架，有良好的数据库设计和SQL编写能力，对系统架构和性能优化有一定了解。',
    soft_skill: '具备良好的团队协作能力和沟通能力，能够快速学习新技术，在压力下保持高效工作，有较强的责任心和执行力。',
    internship: '曾在杭州某科技公司实习，参与项目开发，负责后端接口开发和数据库设计，积累了实际项目经验。',
  },
  internship_experiences: [
    {
      company: '杭州某科技有限公司',
      position: 'Java开发实习生',
      period: '2024.06-2024.09',
      achievement: '参与公司内部管理系统开发，负责用户模块、权限模块的后端接口开发，使用Spring Boot框架，完成数据库设计和SQL优化，提升了系统响应速度。',
    },
  ],
  project_experiences: [
    {
      name: '在线学习平台',
      role: '后端开发',
      description: '基于Spring Boot+Vue的在线学习平台，实现用户管理、课程管理、学习进度跟踪等功能。',
      tech_stacks: ['Java', 'Spring Boot', 'MySQL', 'Vue', 'Redis'],
      highlight: '项目获得校级优秀项目，用户量达500+',
    },
    {
      name: '智能图书管理系统',
      role: '全栈开发',
      description: '实现图书的借还、预约、查询等功能，使用Python进行数据分析。',
      tech_stacks: ['Python', 'Flask', 'MySQL', 'JavaScript'],
      highlight: '提升了图书馆管理效率30%',
    },
  ],
};

// 使用示例数据创建画像
async function useExampleProfile() {
  try {
    const createdProfile = await profileStore.createManualProfile(exampleProfile);
    await preloadReportContext(createdProfile);
  } catch (error) {
    console.error('使用示例数据失败:', error);
  }
}

function createInternshipDraft() {
  return {
    company: '',
    position: '',
    period: '',
    achievement: '',
  };
}

function createProjectDraft() {
  return {
    name: '',
    role: '',
    description: '',
    techStacksText: '',
    highlight: '',
  };
}

const form = reactive({
  expectedPosition: profileStore.studentProfile?.job_preference.expected_position ?? '',
  expectedSalary: profileStore.studentProfile?.job_preference.expected_salary ?? '',
  expectedCity: profileStore.studentProfile?.job_preference.expected_city ?? '',
});

const manualForm = reactive({
  name: '',
  education: '',
  major: '',
  school: '',
  expectedPosition: '',
  expectedSalary: '',
  expectedCity: '',
  skillsText: '',
  certificatesText: '',
  hardSkillDescription: '',
  softSkillDescription: '',
  internshipExperiences: [createInternshipDraft()],
  projectExperiences: [createProjectDraft()],
});

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  selectedFile.value = target.files?.[0] ?? null;
}

function addInternshipExperience() {
  manualForm.internshipExperiences.push(createInternshipDraft());
}

function removeInternshipExperience(index: number) {
  manualForm.internshipExperiences.splice(index, 1);
}

function addProjectExperience() {
  manualForm.projectExperiences.push(createProjectDraft());
}

function removeProjectExperience(index: number) {
  manualForm.projectExperiences.splice(index, 1);
}

function formatSoftSkillDescription(descriptions?: AbilityDescriptions) {
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

async function loadProfileScoring(studentProfile: StudentProfile) {
  console.log('[ProfileUploadPage] Loading profile scoring...');
  console.log('[ProfileUploadPage] Student profile:', studentProfile);
  console.log('[ProfileUploadPage] Target job:', studentProfile.job_preference.expected_position);
  
  isScoringLoading.value = true;
  try {
    profileScoring.value = await scoreProfile(studentProfile, studentProfile.job_preference.expected_position);
    console.log('[ProfileUploadPage] Profile scoring loaded:', profileScoring.value);
  } catch (error) {
    console.error('[ProfileUploadPage] Failed to load profile scoring:', error);
  } finally {
    isScoringLoading.value = false;
  }
}

async function preloadReportContext(createdProfile: StudentProfile) {
  const targetJob = createdProfile.job_preference.expected_position;

  reportStore.setTargetContext(targetJob || '', null);
  await Promise.allSettled([
    reportStore.createReport(createdProfile, undefined, targetJob),
    loadProfileScoring(createdProfile)
  ]);
}

async function handleSubmit() {
  if (!selectedFile.value) {
    profileStore.errorMessage = '请先选择 PDF 或 DOCX 简历文件。';
    return;
  }

  try {
    const createdProfile = await profileStore.createProfile({
      resume: selectedFile.value,
      expectedPosition: form.expectedPosition,
      expectedSalary: form.expectedSalary,
      expectedCity: form.expectedCity,
    });
    await preloadReportContext(createdProfile);
  } catch {
    // Error handled by store.
  }
}

async function handleManualSubmit() {
  const toArray = (value: string) =>
    value
      .split(/[，、,;；\n]+/)
      .map((item) => item.trim())
      .filter(Boolean);

  const internshipExperiences: InternshipExperience[] = manualForm.internshipExperiences
    .map((experience) => ({
      company: experience.company.trim(),
      position: experience.position.trim(),
      period: experience.period.trim(),
      achievement: experience.achievement.trim(),
    }))
    .filter((experience) => Object.values(experience).some((item) => item));

  const projectExperiences: ProjectExperience[] = manualForm.projectExperiences
    .map((project) => ({
      name: project.name.trim(),
      role: project.role.trim(),
      description: project.description.trim(),
      tech_stacks: toArray(project.techStacksText),
      highlight: project.highlight.trim(),
    }))
    .filter((project) => Object.values({ ...project, tech_stacks: project.tech_stacks.join(' ') }).some((item) => item));

  const hardSkillDescription = manualForm.hardSkillDescription.trim();
  const softSkillDescription = manualForm.softSkillDescription.trim();
  const internshipSummary = internshipExperiences
    .map((experience) => [experience.company, experience.position, experience.achievement].filter(Boolean).join(' / '))
    .filter(Boolean)
    .join('；');

  const manualProfile: StudentProfile = {
    student_id: '',
    basic_info: {
      name: manualForm.name,
      education: manualForm.education,
      major: manualForm.major,
      school: manualForm.school,
    },
    skills: toArray(manualForm.skillsText),
    certificates: toArray(manualForm.certificatesText),
    soft_abilities: {
      innovation: 60,
      learning: 60,
      stress_tolerance: 60,
      communication: 60,
      professional_skills: 60,
      certificates: 60,
      internship: 60,
    },
    job_preference: {
      expected_position: manualForm.expectedPosition,
      expected_salary: manualForm.expectedSalary,
      expected_city: manualForm.expectedCity,
    },
    ability_descriptions: {
      professional_skill: hardSkillDescription,
      soft_skill: softSkillDescription,
      internship: internshipSummary,
    },
    internship_experiences: internshipExperiences,
    project_experiences: projectExperiences,
  };

  try {
    const createdProfile = await profileStore.createManualProfile(manualProfile);
    await preloadReportContext(createdProfile);
  } catch {
    // Error handled by store.
  }
}
</script>

<style scoped>
.profile-page {
  gap: var(--space-5);
}

.profile-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-5);
  align-items: start;
}

.profile-layout__column {
  width: 100%;
}

.profile-layout__column,
.preview-grid {
  display: grid;
  gap: var(--space-4);
  min-height: 100%;
}

.profile-layout__column--input :deep(.section-heading) {
  gap: 0.35rem;
  margin-bottom: var(--space-3);
}

.profile-layout__column--input :deep(.card-panel__body) {
  gap: var(--space-2);
}

.profile-layout__column--preview :deep(.section-heading) {
  gap: 0.35rem;
  margin-bottom: var(--space-3);
}

.profile-layout__column--preview :deep(.card-panel__body) {
  gap: var(--space-3);
}

.mode-toggle {
  display: inline-flex;
  padding: 0.2rem;
  border: 1px solid color-mix(in oklab, var(--brand-500) 12%, var(--border-subtle));
  border-radius: 10px;
  background: color-mix(in oklab, var(--bg-surface) 96%, white);
}

.mode-toggle__item {
  min-width: 96px;
  padding: 0.45rem 0.8rem;
  border-radius: 8px;
  color: var(--text-secondary);
  font-weight: 600;
}

.mode-toggle__item--active {
  color: var(--brand-700);
  background: color-mix(in oklab, var(--brand-500) 10%, white);
}

.upload-box {
  display: grid;
  gap: 0.55rem;
  padding: 1rem 1.05rem;
  border: 1px dashed color-mix(in oklab, var(--brand-500) 24%, var(--border-strong));
  border-radius: 14px;
  background: color-mix(in oklab, var(--bg-surface) 98%, white);
}

.upload-box__eyebrow,
.preview-block__label,
.preference-impact__label,
.preview-impact__label {
  margin: 0;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.preference-impact,
.preview-impact {
  display: grid;
  gap: 0.45rem;
  padding: 1rem 1.05rem;
  border-radius: 14px;
}

.preference-impact {
  border: 1px dashed color-mix(in oklab, var(--warning-500) 42%, var(--brand-500));
  background: linear-gradient(180deg, color-mix(in oklab, var(--warning-500) 9%, white), color-mix(in oklab, var(--bg-surface) 98%, white));
}

.preview-impact {
  border: 1px solid color-mix(in oklab, var(--brand-500) 16%, var(--border-subtle));
  background: color-mix(in oklab, var(--brand-500) 6%, white);
}

.preference-impact p,
.preview-impact p {
  margin: 0;
}

.form-section {
  display: grid;
  gap: var(--space-3);
  padding: 0.95rem 1rem;
  border: 1px solid color-mix(in oklab, var(--brand-500) 10%, var(--border-subtle));
  border-radius: 14px;
  background: color-mix(in oklab, var(--bg-surface) 99%, white);
}

.form-section__header,
.entry-card__header,
.preview-block__topline,
.experience-item__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.form-section__eyebrow {
  margin: 0 0 0.15rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.inline-action {
  padding: 0.35rem 0.7rem;
  border: 1px solid color-mix(in oklab, var(--brand-500) 18%, var(--border-subtle));
  border-radius: 999px;
  color: var(--brand-700);
  font-size: 0.84rem;
  font-weight: 600;
  background: color-mix(in oklab, var(--brand-500) 8%, white);
}

.inline-action--danger {
  color: var(--danger-500);
  border-color: color-mix(in oklab, var(--danger-500) 22%, var(--border-subtle));
  background: color-mix(in oklab, var(--danger-500) 8%, white);
}

.entry-list,
.experience-list {
  display: grid;
  gap: var(--space-3);
}

.entry-card,
.experience-item,
.preview-ability-card {
  display: grid;
  gap: var(--space-2);
  padding: 0.85rem 0.95rem;
  border: 1px solid color-mix(in oklab, var(--brand-500) 10%, var(--border-subtle));
  border-radius: 12px;
  background: color-mix(in oklab, var(--bg-muted) 72%, white);
}

.form-grid.columns-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.submit-row {
  display: flex;
  justify-content: flex-start;
  gap: var(--space-3);
}

.upload-progress {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: var(--space-3);
  align-items: center;
}

.upload-progress__track {
  height: 8px;
  border-radius: 999px;
  background: color-mix(in oklab, var(--bg-muted) 88%, white);
  border: 1px solid color-mix(in oklab, var(--brand-500) 10%, var(--border-subtle));
  overflow: hidden;
}

.upload-progress__fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--brand-500), var(--brand-600));
  transition: width 260ms ease;
}

.upload-progress__text {
  font-size: 0.86rem;
  font-weight: 700;
  color: var(--brand-700);
}

.upload-progress__meta {
  font-size: 0.82rem;
  color: var(--text-muted);
}

.preview-block {
  display: grid;
  gap: var(--space-3);
  padding: 0.95rem 1rem;
  border: 1px solid color-mix(in oklab, var(--brand-500) 10%, var(--border-subtle));
  border-radius: 14px;
  background: color-mix(in oklab, var(--bg-surface) 99%, white);
}

.preview-hero,
.preview-summary-grid {
  display: grid;
  gap: var(--space-3);
}

.preview-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  padding: 1rem 1.05rem;
  border: 1px solid color-mix(in oklab, var(--brand-500) 12%, var(--border-subtle));
  border-radius: 16px;
  background: linear-gradient(180deg, color-mix(in oklab, var(--brand-500) 7%, white), color-mix(in oklab, var(--bg-surface) 98%, white));
}

.preview-hero__main {
  display: grid;
  gap: 0.35rem;
}

.preview-hero__main p,
.preview-hero__main strong {
  margin: 0;
}

.preview-summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.preview-block__content {
  display: grid;
  gap: 0.45rem;
}

.preview-block__content p {
  margin: 0;
}

.preview-block__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.75rem;
  height: 1.75rem;
  padding: 0 0.5rem;
  border-radius: 999px;
  color: var(--brand-700);
  font-size: 0.82rem;
  font-weight: 700;
  background: color-mix(in oklab, var(--brand-500) 12%, white);
}

.preview-ability-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-3);
}

.preview-ability-card strong,
.experience-item strong {
  font-size: 0.96rem;
}

.preview-ability-card p,
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

.profile-layout__column--input .form-section {
  gap: var(--space-2);
  padding: 0.82rem 0.9rem;
  border-color: color-mix(in oklab, var(--brand-500) 8%, var(--border-subtle));
  background: linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 99%, white), color-mix(in oklab, var(--bg-muted) 40%, white));
}

.profile-layout__column--input .form-section__header strong {
  font-size: 0.96rem;
  font-weight: 600;
}

.profile-layout__column--input .form-section__eyebrow {
  font-size: 0.74rem;
  letter-spacing: 0.04em;
}

.profile-layout__column--input .entry-list {
  gap: var(--space-2);
}

.profile-layout__column--input .entry-card {
  gap: 0.65rem;
  padding: 0.78rem 0.85rem;
  border-style: dashed;
  border-color: color-mix(in oklab, var(--brand-500) 12%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-surface) 99%, white);
}

.profile-layout__column--input .inline-action {
  padding: 0.28rem 0.62rem;
  border-color: color-mix(in oklab, var(--brand-500) 12%, var(--border-subtle));
  color: var(--text-secondary);
  background: color-mix(in oklab, var(--bg-surface) 97%, white);
}

.profile-layout__column--input .inline-action--danger {
  color: var(--danger-500);
  border-color: color-mix(in oklab, var(--danger-500) 18%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-surface) 97%, white);
}

.badge--soft {
  background: color-mix(in oklab, var(--warning-500) 10%, white);
  color: color-mix(in oklab, var(--warning-700) 72%, black);
}

.badge--neutral {
  background: color-mix(in oklab, var(--bg-muted) 92%, white);
  color: var(--text-secondary);
}

.preview-actions {
  display: flex;
  justify-content: flex-start;
}

/* 评分展示样式 */
.preview-scoring {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-3);
  margin: var(--space-4) 0;
}

.preview-scoring__card {
  padding: 1rem 1.1rem;
  border: 1px solid rgba(124, 58, 237, 0.15);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: all var(--transition-base);
  text-align: center;
}

.preview-scoring__card:hover {
  border-color: rgba(124, 58, 237, 0.25);
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.1);
}

.preview-scoring__label {
  margin: 0 0 0.5rem 0;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.preview-scoring__content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.preview-scoring__value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}

.preview-scoring__unit {
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.form-error {
  margin: 0;
  color: var(--danger-500);
}

/* New styles for optimized features */
.upload-box {
  display: grid;
  gap: 0.55rem;
  padding: 1.5rem 1.5rem;
  border: 2px dashed rgba(124, 58, 237, 0.4);
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.05) 0%, rgba(6, 182, 212, 0.03) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  cursor: pointer;
  transition: all var(--transition-smooth);
  animation: pulse-soft 3s ease-in-out infinite;
}

.upload-box:hover {
  border-color: var(--accent-purple);
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.08) 0%, rgba(6, 182, 212, 0.05) 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(124, 58, 237, 0.15);
}

@keyframes pulse-soft {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(124, 58, 237, 0.1);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(124, 58, 237, 0);
  }
}

.upload-box--highlight {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.1) 0%, rgba(6, 182, 212, 0.08) 100%);
  border-color: var(--accent-purple);
  border-width: 2px;
  animation: pulse-strong 2s ease-in-out infinite;
}

@keyframes pulse-strong {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(124, 58, 237, 0.2);
  }
  50% {
    box-shadow: 0 0 0 15px rgba(124, 58, 237, 0);
  }
}

.mode-toggle {
  display: inline-flex;
  padding: 0.25rem;
  border: 1px solid rgba(124, 58, 237, 0.2);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.mode-toggle__item {
  min-width: 100px;
  padding: 0.6rem 1rem;
  border-radius: 10px;
  color: var(--text-secondary);
  font-weight: 600;
  transition: all var(--transition-base);
}

.mode-toggle__item:hover {
  background: rgba(124, 58, 237, 0.05);
}

.mode-toggle__item--active {
  color: white;
  background: linear-gradient(135deg, var(--primary-blue) 0%, var(--accent-purple) 100%);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.25);
}

.form-section {
  display: grid;
  gap: var(--space-3);
  padding: 1.2rem 1.2rem;
  border: 1px solid rgba(124, 58, 237, 0.15);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: all var(--transition-base);
}

.form-section:hover {
  border-color: rgba(124, 58, 237, 0.25);
  background: rgba(255, 255, 255, 0.7);
}

.entry-card {
  display: grid;
  gap: var(--space-2);
  padding: 1rem 1.1rem;
  border: 1px dashed rgba(124, 58, 237, 0.2);
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.03) 0%, rgba(6, 182, 212, 0.02) 100%);
  transition: all var(--transition-base);
}

.entry-card:hover {
  border-style: solid;
  border-color: rgba(124, 58, 237, 0.3);
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.05) 0%, rgba(6, 182, 212, 0.03) 100%);
}

.preview-block {
  display: grid;
  gap: var(--space-3);
  padding: 1.1rem 1.2rem;
  border: 1px solid rgba(124, 58, 237, 0.15);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: all var(--transition-base);
}

.preview-block:hover {
  border-color: rgba(124, 58, 237, 0.25);
  background: rgba(255, 255, 255, 0.75);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.1);
}

.preview-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  padding: 1.2rem 1.2rem;
  border: 1px solid rgba(124, 58, 237, 0.2);
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.08) 0%, rgba(6, 182, 212, 0.05) 100%);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
}

.demo-link {
  display: flex;
  justify-content: center;
  margin-top: var(--space-3);
}

.demo-link__btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-5);
  background: linear-gradient(135deg, rgba(255, 122, 69, 0.08) 0%, rgba(124, 58, 237, 0.06) 100%);
  border: 1px solid rgba(255, 122, 69, 0.2);
  border-radius: 12px;
  color: var(--accent-orange);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-smooth);
}

.demo-link__btn:hover {
  background: linear-gradient(135deg, rgba(255, 122, 69, 0.12) 0%, rgba(124, 58, 237, 0.08) 100%);
  border-color: rgba(255, 122, 69, 0.35);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 122, 69, 0.15);
}

/* Quick preference chips */
.quick-preference {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  padding: 1rem;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.05) 0%, rgba(6, 182, 212, 0.03) 100%);
  border-radius: 14px;
  border: 1px solid rgba(124, 58, 237, 0.15);
  margin-top: var(--space-3);
}

.quick-preference__chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(124, 58, 237, 0.2);
  border-radius: 999px;
  font-size: 0.9rem;
  transition: all var(--transition-base);
}

.quick-preference__chip:hover {
  border-color: rgba(124, 58, 237, 0.35);
  background: rgba(255, 255, 255, 0.85);
}

.quick-preference__edit-icon {
  opacity: 0.7;
  transition: opacity var(--transition-base);
}

.quick-preference__chip:hover .quick-preference__edit-icon {
  opacity: 1;
}

/* Status card styles */
.stat-card-status {
  display: grid;
  gap: var(--space-3);
}

.stat-card-status__content {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.stat-card-status__value {
  font-weight: 600;
  color: var(--text-primary);
}

.stat-card-progress {
  display: grid;
  place-items: center;
  gap: var(--space-3);
}

.stat-card-competitiveness {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.stat-card-competitiveness__value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary-blue);
}

.stat-card-competitiveness__value--mock {
  color: var(--accent-orange);
}

/* Mock preview specific styles */
.preview-hero--mock {
  background: linear-gradient(180deg, color-mix(in oklab, var(--accent-orange) 10%, white), color-mix(in oklab, var(--bg-surface) 98%, white));
  border-color: color-mix(in oklab, var(--accent-orange) 30%, var(--border-subtle));
}

.preview-block--mock {
  border-color: color-mix(in oklab, var(--accent-orange) 20%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-orange-soft) 30%, var(--bg-surface));
}

.preview-impact--mock {
  border: 1px dashed var(--accent-orange);
  background: var(--bg-orange-soft);
}

.preview-impact--mock .preview-impact__label {
  color: var(--accent-orange);
}

@media (max-width: 960px) {
  .form-grid.columns-2,
  .upload-progress,
  .preview-ability-grid,
  .preview-summary-grid,
  .preview-hero {
    grid-template-columns: 1fr;
  }
}
</style>
