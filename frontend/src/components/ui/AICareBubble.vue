<template>
  <div>
    <div v-if="visibleProp" class="ai-care-bubble" :class="{ 'ai-care-bubble--entering': entering }">
      <div class="ai-care-bubble__avatar">
        <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
          <circle cx="16" cy="16" r="16" fill="#FF7A45" />
          <path d="M12 14C12 12.8954 12.8954 12 14 12H18C19.1046 12 20 12.8954 20 14V18C20 19.1046 19.1046 20 18 20H14C12.8954 20 12 19.1046 12 18V14Z" fill="white" />
          <circle cx="14" cy="15.5" r="1" fill="#FF7A45" />
          <circle cx="18" cy="15.5" r="1" fill="#FF7A45" />
        </svg>
      </div>
      <div class="ai-care-bubble__content" @click="handleClick">
        <div class="ai-care-bubble__header">
          <span class="ai-care-bubble__name">职业助手</span>
          <span class="ai-care-bubble__time">{{ time }}</span>
        </div>
        <p class="ai-care-bubble__text">{{ message }}</p>
      </div>
      <button class="ai-care-bubble__close" @click="hideBubble" aria-label="隐藏">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M12 4L4 12M4 4L12 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
        </svg>
      </button>
    </div>
    <button v-else class="ai-care-bubble__toggle" @click="showBubble" aria-label="显示助手">
      <svg width="28" height="28" viewBox="0 0 32 32" fill="none">
        <circle cx="16" cy="16" r="16" fill="#FF7A45" />
        <path d="M12 14C12 12.8954 12.8954 12 14 12H18C19.1046 12 20 12.8954 20 14V18C20 19.1046 19.1046 20 18 20H14C12.8954 20 12 19.1046 12 18V14Z" fill="white" />
        <circle cx="14" cy="15.5" r="1" fill="#FF7A45" />
        <circle cx="18" cy="15.5" r="1" fill="#FF7A45" />
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAppStore } from '@/stores/app';
import { useProfileStore } from '@/stores/profile';

const props = withDefaults(defineProps<{
  autoShow?: boolean;
  delay?: number;
  currentRoute?: string;
  visible?: boolean;
}>(), {
  autoShow: true,
  delay: 3000,
  currentRoute: '/',
  visible: true,
});

const emit = defineEmits<{
  close: [];
  click: [];
  toggle: [];
}>();

const appStore = useAppStore();
const profileStore = useProfileStore();
const router = useRouter();

const visibleInternal = ref(true);
const entering = ref(false);
const startTime = ref<number>(0);
let timer: number | null = null;
let checkTimer: number | null = null;

const visibleProp = computed(() => {
  return props.visible !== undefined ? props.visible : visibleInternal.value;
});

const time = computed(() => {
  const now = new Date();
  return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
});

const getContextualMessage = (): string => {
  const currentTime = Date.now();
  const timeOnPage = (currentTime - startTime.value) / 1000;

  if (appStore.isDemoMode && !profileStore.hasProfile) {
    if (props.currentRoute === '/career-path' && timeOnPage > 10) {
      return '🚀 这只是示例路径哦。上传简历，看看你的真实发展路线。';
    }
    
    if (props.currentRoute === '/report') {
      return '📊 想知道你的真实竞争力评分吗？上传简历马上生成。';
    }
    
    if (props.currentRoute === '/matching') {
      return '🎯 这些是示例匹配岗位。上传简历，找到真正适合你的机会！';
    }

    if (props.currentRoute === '/profile') {
      return '📝 这是示例画像。上传你的简历，生成专属职业分析吧！';
    }
  }

  const defaultMessages = [
    '👋 欢迎！需要帮助分析您的职业方向吗？',
    '💡 试试上传简历，AI 可以为您生成专属画像',
    '🎯 岗位匹配功能很有意思，要不要试试看？',
    '📊 职业路径分析可以帮您规划未来发展',
    '✨ 我一直在这，有任何问题随时问我！'
  ];
  return defaultMessages[Math.floor(Math.random() * defaultMessages.length)];
};

const message = ref(getContextualMessage());

const showBubble = () => {
  emit('toggle');
  message.value = getContextualMessage();
  entering.value = true;
  setTimeout(() => {
    entering.value = false;
  }, 100);
};

const hideBubble = () => {
  entering.value = true;
  setTimeout(() => {
    visibleInternal.value = false;
    entering.value = false;
  }, 200);
  emit('toggle');
};

const handleClick = () => {
  router.push('/');
  emit('click');
};

const checkAndUpdateMessage = () => {
  if (visibleProp.value) {
    const newMessage = getContextualMessage();
    if (newMessage !== message.value) {
      message.value = newMessage;
    }
  }
};

watch(() => props.currentRoute, () => {
  startTime.value = Date.now();
  if (visibleProp.value) {
    message.value = getContextualMessage();
  }
});

watch([() => appStore.isDemoMode, () => profileStore.hasProfile], () => {
  if (visibleProp.value) {
    message.value = getContextualMessage();
  }
});

onMounted(() => {
  startTime.value = Date.now();
  
  if (props.autoShow !== false) {
    const delay = props.delay || 3000;
    timer = window.setTimeout(() => {
      message.value = getContextualMessage();
      entering.value = true;
      visibleInternal.value = true;
      setTimeout(() => {
        entering.value = false;
      }, 100);
    }, delay);
  }

  checkTimer = window.setInterval(checkAndUpdateMessage, 3000);
});

onUnmounted(() => {
  if (timer) {
    clearTimeout(timer);
  }
  if (checkTimer) {
    clearInterval(checkTimer);
  }
});

defineExpose({
  show: showBubble,
  close: hideBubble,
});
</script>

<style scoped>
.ai-care-bubble {
  position: fixed;
  bottom: 24px;
  right: 24px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 20px;
  background: var(--bg-surface);
  border-radius: var(--radius-xl);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08);
  max-width: 360px;
  z-index: 1000;
  animation: slideIn 0.3s ease-out;
  border: 1px solid var(--border-subtle);
}

.ai-care-bubble--entering {
  opacity: 0;
  transform: translateY(20px);
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-care-bubble__avatar {
  flex-shrink: 0;
}

.ai-care-bubble__content {
  flex: 1;
  min-width: 0;
  cursor: pointer;
  transition: all var(--transition-base);
}

.ai-care-bubble__content:hover {
  opacity: 0.9;
}

.ai-care-bubble__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.ai-care-bubble__name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
}

.ai-care-bubble__time {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.ai-care-bubble__text {
  margin: 0;
  font-size: 0.9375rem;
  line-height: 1.5;
  color: var(--text-secondary);
}

.ai-care-bubble__close {
  flex-shrink: 0;
  padding: 4px;
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-base);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-care-bubble__close:hover {
  background: var(--bg-subtle);
  color: var(--text-primary);
}

.ai-care-bubble__toggle {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #FF7A45 0%, #FF4500 100%);
  border: none;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(255, 122, 69, 0.4), 0 2px 8px rgba(255, 122, 69, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  transition: all 0.3s ease;
  animation: pulse 2s infinite;
}

.ai-care-bubble__toggle:hover {
  transform: scale(1.1);
  box-shadow: 0 12px 32px rgba(255, 122, 69, 0.5), 0 4px 12px rgba(255, 122, 69, 0.3);
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 8px 24px rgba(255, 122, 69, 0.4), 0 2px 8px rgba(255, 122, 69, 0.2);
  }
  50% {
    box-shadow: 0 8px 24px rgba(255, 122, 69, 0.6), 0 2px 8px rgba(255, 122, 69, 0.4);
  }
}

@media (max-width: 480px) {
  .ai-care-bubble {
    left: 16px;
    right: 16px;
    max-width: none;
    bottom: 100px;
  }
  
  .ai-care-bubble__toggle {
    width: 52px;
    height: 52px;
    bottom: 16px;
    right: 16px;
  }
}
</style>
