import { ref } from 'vue';
import { defineStore } from 'pinia';
import type { ChatMessageDTO } from '@/types/api';

export type ChatRole = 'assistant' | 'user';

export type ChatMessageItem = {
  id: number;
  role: ChatRole;
  content: string;
  conversationKey?: string;
  createdAt?: string;
};

const DEFAULT_GREETING = '你好，我可以帮你分析岗位方向、技能差距和职业规划。';

function buildGreeting(): ChatMessageItem {
  return {
    id: 1,
    role: 'assistant',
    content: DEFAULT_GREETING,
  };
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessageItem[]>([buildGreeting()]);
  const sessionId = ref('');
  const loadedHistoryUserId = ref<number | null>(null);

  function ensureGreeting() {
    if (!messages.value.length) {
      messages.value = [buildGreeting()];
    }
  }

  function setSessionId(value: string) {
    sessionId.value = value || '';
  }

  function appendMessage(role: ChatRole, content: string, options?: { conversationKey?: string; createdAt?: string }) {
    const nextId = Date.now() + Math.floor(Math.random() * 1000);
    messages.value.push({
      id: nextId,
      role,
      content,
      conversationKey: options?.conversationKey,
      createdAt: options?.createdAt,
    });
  }

  function shouldLoadServerHistory(userId: number) {
    return loadedHistoryUserId.value !== userId && messages.value.length <= 1;
  }

  function markHistoryLoaded(userId: number) {
    loadedHistoryUserId.value = userId;
  }

  function hydrateFromHistory(chats: ChatMessageDTO[], userId: number) {
    const ordered = [...(chats ?? [])].sort((left, right) => {
      const leftTime = new Date(left.createdAt).getTime();
      const rightTime = new Date(right.createdAt).getTime();
      return leftTime - rightTime;
    });

    // If the user has already started chatting locally, do not overwrite local messages.
    if (messages.value.length > 1 && loadedHistoryUserId.value !== userId) {
      const latestConversationKey = [...ordered]
        .reverse()
        .map((item) => item.conversationKey)
        .find((value) => typeof value === 'string' && value.trim().length > 0);
      if (!sessionId.value && latestConversationKey) {
        sessionId.value = latestConversationKey;
      }
      loadedHistoryUserId.value = userId;
      return;
    }

    if (!ordered.length) {
      loadedHistoryUserId.value = userId;
      ensureGreeting();
      return;
    }

    messages.value = ordered.map((item, index) => ({
      id: index + 1,
      role: item.role === 'user' ? 'user' : 'assistant',
      content: item.content,
      conversationKey: item.conversationKey,
      createdAt: item.createdAt,
    }));

    const latestConversationKey = [...ordered]
      .reverse()
      .map((item) => item.conversationKey)
      .find((value) => typeof value === 'string' && value.trim().length > 0);
    sessionId.value = latestConversationKey || '';
    loadedHistoryUserId.value = userId;
  }

  function reset() {
    messages.value = [buildGreeting()];
    sessionId.value = '';
    loadedHistoryUserId.value = null;
  }

  return {
    messages,
    sessionId,
    loadedHistoryUserId,
    ensureGreeting,
    setSessionId,
    appendMessage,
    shouldLoadServerHistory,
    markHistoryLoaded,
    hydrateFromHistory,
    reset,
  };
});
