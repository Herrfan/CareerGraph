<template>
  <AppShell>
    <section class="page-grid home-page">
      <section class="hero-grid">
        <CardPanel class="hero-card hero-card--primary" eyebrow="Career Agent" title="更清楚地认识自己，更稳地选择岗位。" description="先完成学生画像，再看岗位匹配和职业报告，让每一步都有依据。">
          <div class="hero-card__body">
            <div class="hero-card__actions">
              <AppButton class="pulse-button" @click="router.push('/profile')">开始生成画像</AppButton>
              <AppButton variant="secondary" @click="router.push('/matching')">查看岗位匹配</AppButton>
            </div>
          </div>
        </CardPanel>

        <CardPanel class="hero-card hero-card--stats" eyebrow="Overview" title="当前使用状态" description="根据当前进度，继续进入下一步分析。">
          <div class="status-grid">
            <div class="status-item status-item--profile">
              <div class="status-item__icon">📊</div>
              <p class="status-item__label">学生画像</p>
              <strong>{{ profileStore.studentProfile ? '已完成' : '未完成' }}</strong>
              <p class="text-secondary">{{ profileStore.studentProfile ? '可以继续做岗位匹配和报告分析。' : '建议先上传简历或手动录入画像。' }}</p>
            </div>
            <div class="status-item status-item--flow">
              <div class="status-item__icon">📈</div>
              <p class="status-item__label">推荐流程</p>
              <strong>画像 → 匹配 → 报告</strong>
              <p class="text-secondary">按这个顺序使用，结果会更完整，也更容易理解。</p>
            </div>
          </div>
        </CardPanel>
      </section>

      <section class="workspace-grid">
        <CardPanel class="chat-card" title="智能体对话" description="可以直接咨询岗位方向、技能差距、求职准备和职业规划问题。">
          <div class="chat-panel">
            <div class="chat-thread">
              <div v-for="message in messages" :key="message.id" :class="['chat-row', message.role]">
                <div :class="['chat-avatar', message.role]">
                  <span v-if="message.role === 'user'" class="avatar-icon">👤</span>
                  <span v-else class="avatar-icon">🤖</span>
                </div>
                <div
                  :class="['chat-bubble', 'markdown-body', message.role === 'user' ? 'chat-bubble--user' : 'chat-bubble--assistant']"
                  v-html="renderMessageHtml(message)"
                />
              </div>
              <div v-if="isThinking" class="chat-row assistant">
                <div class="chat-avatar assistant">
                  <span class="avatar-icon">🤖</span>
                </div>
                <div class="thinking-indicator">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="text">正在思考中...</span>
                </div>
              </div>
            </div>

            <div class="chat-input-wrapper">
              <form class="chat-input-row" @submit.prevent="handleAsk">
                <div class="input-container">
                  <input v-model="question" class="chat-input" placeholder="例如：我想找前端方向的岗位，现在最该补什么？" />
                </div>
                <AppButton type="submit" :disabled="isThinking || !question.trim()" class="send-button">
                  <span class="btn-text">发送</span>
                  <span class="arrow-icon">→</span>
                </AppButton>
              </form>
            </div>
          </div>
        </CardPanel>
      </section>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { marked } from 'marked';
import { onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/layout/AppShell.vue';
import AppButton from '@/components/ui/AppButton.vue';
import CardPanel from '@/components/ui/CardPanel.vue';
import { getMyHistory } from '@/services/history';
import { useAuthStore } from '@/stores/auth';
import { useChatStore } from '@/stores/chat';
import { useProfileStore } from '@/stores/profile';
import { chatWithAi } from '@/services/ai';

const router = useRouter();
const authStore = useAuthStore();
const chatStore = useChatStore();
const profileStore = useProfileStore();
const question = ref('');
const isThinking = ref(false);
type ChatRole = 'assistant' | 'user';
type ChatMessage = {
  id: number;
  role: ChatRole;
  content: string;
};

const messages = chatStore.messages;

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function normalizeAssistantMarkdown(value: string) {
  let text = value.replace(/\r\n?/g, '\n').trim();
  text = text.replace(/([：:])\s*(\d+\.)/g, '$1\n$2');
  text = text.replace(/\s+(\d+\.)\s+/g, '\n$1 ');
  text = text.replace(/([：:])\s*-\s+/g, '$1\n- ');
  text = text.replace(/\s+-\s+/g, '\n- ');
  return text;
}

function renderMessageHtml(message: ChatMessage) {
  const raw = message.role === 'assistant' ? normalizeAssistantMarkdown(message.content) : message.content;
  const safeMarkdown = escapeHtml(raw);
  return marked.parse(safeMarkdown, { gfm: true, breaks: true }) as string;
}

async function handleAsk() {
  const input = question.value.trim();
  if (!input || isThinking.value) {
    return;
  }

  chatStore.appendMessage('user', input);
  question.value = '';
  isThinking.value = true;

  try {
    const context = profileStore.studentProfile
      ? JSON.stringify(
          {
            basicInfo: profileStore.studentProfile.basic_info,
            skills: profileStore.studentProfile.skills,
            jobPreference: profileStore.studentProfile.job_preference,
          },
          null,
          2,
        )
      : '';
    const response = await chatWithAi(input, chatStore.sessionId, context);
    chatStore.setSessionId(response.session_id);
    chatStore.appendMessage('assistant', response.reply || '当前没有返回内容，请稍后重试。', {
      conversationKey: response.session_id,
      createdAt: new Date().toISOString(),
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : '请求失败，请稍后重试';
    chatStore.appendMessage('assistant', `调用失败：${message}`);
  } finally {
    isThinking.value = false;
  }
}

async function bootstrapChatHistory() {
  if (!authStore.token) {
    chatStore.ensureGreeting();
    return;
  }
  const userKey = authStore.currentUser?.id ?? -1;
  if (!chatStore.shouldLoadServerHistory(userKey)) {
    return;
  }
  try {
    const history = await getMyHistory();
    chatStore.hydrateFromHistory(history.chats ?? [], userKey);
  } catch {
    chatStore.markHistoryLoaded(userKey);
    chatStore.ensureGreeting();
  }
}

onMounted(() => {
  void bootstrapChatHistory();
});

watch(
  () => `${authStore.token || ''}:${authStore.currentUser?.id ?? ''}`,
  () => {
    void bootstrapChatHistory();
  },
);
</script>

<style scoped>
.home-page,
.hero-grid,
.workspace-grid,
.status-grid,
.entry-grid {
  display: grid;
  gap: var(--space-4);
}

.hero-grid,
.workspace-grid,
.status-grid {
  display: grid;
  gap: var(--space-4);
}

.hero-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
}

.workspace-grid {
  grid-template-columns: 1fr;
}

.hero-card {
  min-height: 100%;
}

.hero-card--primary :deep(.card-panel) {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  min-height: 320px;
  padding-top: 3.5rem;
}

.hero-card--primary :deep(.card-panel__body) {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding-top: 0;
}

.hero-card--primary :deep(.section-heading) {
  margin-bottom: 0;
}

.hero-card__body {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  flex: 1;
  padding-bottom: 4rem;
}

.hero-card--primary {
  background:
    linear-gradient(135deg, rgba(16, 185, 129, 0.08) 0%, rgba(5, 150, 105, 0.04) 50%, rgba(245, 158, 11, 0.06) 100%),
    radial-gradient(circle at 10% 20%, rgba(59, 130, 246, 0.08), transparent 50%),
    radial-gradient(circle at 90% 80%, rgba(16, 185, 129, 0.06), transparent 50%),
    white;
}

.hero-card--primary :deep(.section-heading) {
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1.25rem;
  margin-bottom: 1.5rem;
}

.hero-card--primary :deep(.card-panel__title) {
  line-height: 1.4;
}

.hero-card--primary :deep(.card-panel__description) {
  max-width: none;
  line-height: 1.8;
  margin-top: 0.5rem;
}

.hero-card--stats {
  background:
    linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(37, 99, 235, 0.03) 100%),
    white;
}

.hero-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
  margin-top: 0;
}

.pulse-button {
  position: relative;
}

.pulse-button::before {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: inherit;
  background: linear-gradient(135deg, #4F46E5, #0EA5E9);
  z-index: -1;
  opacity: 0;
  filter: blur(8px);
  transition: opacity 0.3s ease;
}

.pulse-button:hover::before {
  opacity: 0.4;
  animation: pulse-border 2s ease-in-out infinite;
}

@keyframes pulse-border {
  0%, 100% { transform: scale(1); opacity: 0.4; }
  50% { transform: scale(1.05); opacity: 0.2; }
}

.status-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.status-item {
  display: grid;
  gap: 0.55rem;
  padding: 1rem 1.05rem;
  border-radius: 16px;
  border: 1px solid rgba(79, 70, 229, 0.1);
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.9), rgba(241, 245, 249, 0.7));
  transition: all 0.3s ease;
}

.status-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px -10px rgba(79, 70, 229, 0.2);
  border-color: rgba(79, 70, 229, 0.2);
}

.status-item--profile:hover {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.05), rgba(248, 250, 252, 0.9));
}

.status-item--flow:hover {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.05), rgba(248, 250, 252, 0.9));
}

.status-item__icon {
  font-size: 1.8rem;
  margin-bottom: 0.25rem;
}

.status-item p,
.entry-card p {
  margin: 0;
}

.status-item__label {
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.chat-card {
  background:
    linear-gradient(180deg, rgba(79, 70, 229, 0.02) 0%, rgba(6, 182, 212, 0.01) 100%),
    white;
}

.chat-panel {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: var(--space-4);
  min-height: 0;
}

.chat-thread {
  height: clamp(320px, 42vh, 520px);
  max-height: 520px;
  padding: 0.5rem;
  padding-right: 0.5rem;
  overflow: auto;
  display: grid;
  align-content: start;
  gap: 1.25rem;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.5) 0%, rgba(241, 245, 249, 0.3) 100%);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.chat-thread::-webkit-scrollbar {
  width: 8px;
}

.chat-thread::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.4);
  border-radius: 4px;
  margin: 8px 0;
}

.chat-thread::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #4F46E5, #6366F1);
  border-radius: 4px;
  transition: all 0.3s ease;
}

.chat-thread::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #4338CA, #4F46E5);
}

.chat-row {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.chat-row.user {
  flex-direction: row-reverse;
}

.chat-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  flex-shrink: 0;
  font-size: 1.25rem;
}

.chat-avatar.user {
  background: linear-gradient(135deg, #4F46E5, #6366F1);
  box-shadow: 0 4px 12px -4px rgba(79, 70, 229, 0.3);
}

.chat-avatar.assistant {
  background: linear-gradient(135deg, #10B981, #059669);
  box-shadow: 0 4px 12px -4px rgba(16, 185, 129, 0.3);
}

.chat-bubble {
  margin: 0;
  max-width: 75%;
  padding: 1rem 1.25rem;
  border-radius: 18px;
  line-height: 1.7;
  transition: all 0.3s ease;
  position: relative;
}

.chat-bubble:hover {
  transform: translateY(-2px);
}

.chat-bubble--user {
  color: white;
  background: linear-gradient(135deg, #4F46E5, #6366F1);
  border: none;
  box-shadow: 0 6px 20px -6px rgba(79, 70, 229, 0.4);
  border-bottom-right-radius: 6px;
}

.chat-bubble--assistant {
  background: linear-gradient(135deg, #FFFFFF, #F8FAFC);
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 4px 12px -4px rgba(0, 0, 0, 0.08);
  border-bottom-left-radius: 6px;
}

.chat-bubble :deep(p),
.chat-bubble :deep(ul),
.chat-bubble :deep(ol) {
  margin: 0 0 0.5rem;
}

.chat-bubble :deep(li),
.chat-bubble :deep(p) {
  line-height: 1.75;
}

.chat-bubble :deep(ul),
.chat-bubble :deep(ol) {
  padding-left: 1.3rem;
}

.chat-bubble :deep(*:last-child) {
  margin-bottom: 0;
}

.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: linear-gradient(135deg, #FFFFFF, #F8FAFC);
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 18px;
  border-bottom-left-radius: 6px;
  box-shadow: 0 4px 12px -4px rgba(0, 0, 0, 0.08);
}

.thinking-indicator .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4F46E5, #6366F1);
  animation: bounce-dot 1.4s ease-in-out infinite;
  box-shadow: 0 2px 8px -4px rgba(79, 70, 229, 0.4);
}

.thinking-indicator .dot:nth-child(2) {
  animation-delay: -0.2s;
}

.thinking-indicator .dot:nth-child(3) {
  animation-delay: -0.4s;
}

.thinking-indicator .text {
  color: #64748B;
  font-size: 0.95rem;
}

@keyframes bounce-dot {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.5; }
  40% { transform: translateY(-10px); opacity: 1; }
}

.chat-input-wrapper {
  padding-top: var(--space-4);
  border-top: 1px solid rgba(226, 232, 240, 0.6);
}

.chat-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-3);
}

.input-container {
  position: relative;
}

.chat-input {
  width: 100%;
  padding: 1rem 1.25rem;
  border: 2px solid rgba(226, 232, 240, 0.8);
  border-radius: 16px;
  font-size: 1rem;
  background: linear-gradient(135deg, #FFFFFF, #F8FAFC);
  transition: all 0.3s ease;
  outline: none;
}

.chat-input:focus {
  border-color: rgba(79, 70, 229, 0.4);
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1), 0 4px 12px -4px rgba(79, 70, 229, 0.2);
}

.chat-input::placeholder {
  color: #94A3B8;
}

.send-button {
  position: relative;
  overflow: hidden;
  padding-inline: 1.5rem;
  min-width: 100px;
}

.send-button .btn-text {
  margin-right: 0.25rem;
}

.send-button .arrow-icon {
  display: inline-block;
  transition: transform 0.3s ease;
}

.send-button:hover .arrow-icon {
  transform: translateX(4px);
}

@media (max-width: 1040px) {
  .hero-grid,
  .workspace-grid,
  .status-grid {
    grid-template-columns: 1fr;
  }

  .chat-thread {
    height: 360px;
    max-height: 420px;
  }

  .hero-card__illustration {
    position: relative;
    right: auto;
    top: auto;
    justify-content: center;
  }
}

@media (max-width: 720px) {
  .chat-input-row {
    grid-template-columns: 1fr;
  }

  .chat-bubble {
    max-width: 85%;
  }

  .chat-avatar {
    width: 32px;
    height: 32px;
    font-size: 1.1rem;
  }

  .send-button {
    width: 100%;
  }
}
</style>
