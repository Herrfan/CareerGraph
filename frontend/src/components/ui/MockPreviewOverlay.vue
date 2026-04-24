<template>
  <div class="mock-preview-wrapper" :style="{ position: 'relative' }">
    <slot />
    <div v-if="!appStore.isDemoMode" class="mock-overlay">
      <div class="mock-overlay__content">
        <div class="mock-icon">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="24" fill="#FFF2E5" />
            <path d="M24 14C26.2 14 28 15.8 28 18C28 19.2 27.5 20.3 26.7 21C27.5 21.7 28 22.8 28 24V26H20V24C20 22.8 20.5 21.7 21.3 21C20.5 20.3 20 19.2 20 18C20 15.8 21.8 14 24 14Z" fill="#FF7A45" />
            <circle cx="24" cy="34" r="2" fill="#FF7A45" />
          </svg>
        </div>
        <div class="mock-text">
          <h3 class="mock-title">{{ title || '这是预览效果' }}</h3>
          <p class="mock-description">上传您的简历后，AI 将为您生成专属职业画像</p>
        </div>
        <div class="mock-actions">
          <button class="mock-btn-primary" @click="$emit('upload')">
            <span>📄</span>
            上传简历
          </button>
          <button class="mock-btn-secondary" @click="handleDemo">
            <span>🎯</span>
            体验 Demo
          </button>
        </div>
        <div class="mock-tag">预览模式</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/app';

withDefaults(defineProps<{
  title?: string;
}>(), {
  title: '',
});

const emit = defineEmits<{
  upload: [];
  demo: [];
}>();

const appStore = useAppStore();

function handleDemo() {
  appStore.enterDemoMode();
  emit('demo');
}
</script>

<style scoped>
.mock-preview-wrapper {
  position: relative;
}

.mock-overlay {
  position: absolute;
  inset: 0;
  background: rgba(249, 250, 251, 0.92);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  border-radius: var(--radius-lg);
}

.mock-overlay__content {
  text-align: center;
  padding: var(--space-8);
  max-width: 420px;
}

.mock-icon {
  margin-bottom: var(--space-6);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.mock-text {
  margin-bottom: var(--space-8);
}

.mock-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 var(--space-3) 0;
  line-height: 1.3;
}

.mock-description {
  font-size: 1rem;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}

.mock-actions {
  display: flex;
  gap: var(--space-4);
  justify-content: center;
  flex-wrap: wrap;
}

.mock-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-6);
  background: var(--primary-blue);
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-smooth);
}

.mock-btn-primary:hover {
  background: var(--brand-600);
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

.mock-btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-6);
  background: var(--bg-surface);
  color: var(--text-primary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-smooth);
}

.mock-btn-secondary:hover {
  border-color: var(--primary-blue);
  color: var(--primary-blue);
  background: var(--bg-accent-soft);
}

.mock-tag {
  display: inline-block;
  margin-top: var(--space-6);
  padding: var(--space-2) var(--space-4);
  background: var(--bg-orange-soft);
  color: var(--accent-orange);
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: var(--radius-md);
}
</style>
