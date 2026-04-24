<template>
  <div v-if="isDemoMode" class="demo-banner">
    <div class="demo-banner__content">
      <span class="demo-banner__icon">💡</span>
      <p class="demo-banner__text">
        你正在体验演示模式。数据为示例内容。上传简历，获得真实分析报告。
      </p>
      <div class="demo-banner__actions">
        <button class="demo-banner__btn demo-banner__btn--secondary" @click="handleExitDemo">
          退出演示
        </button>
        <button class="demo-banner__btn demo-banner__btn--primary" @click="handleUpload">
          上传简历
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useAppStore } from '@/stores/app';

const appStore = useAppStore();
const router = useRouter();

const isDemoMode = appStore.isDemoMode;

function handleExitDemo() {
  appStore.exitDemoMode();
}

function handleUpload() {
  appStore.exitDemoMode();
  router.push('/profile');
}
</script>

<style scoped>
.demo-banner {
  position: sticky;
  top: 0;
  z-index: 100;
  width: 100%;
  background: linear-gradient(90deg, #FFF7E5 0%, #FFFBE5 50%, #FFF7E5 100%);
  border-bottom: 1px solid #FFD9A3;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    transform: translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.demo-banner__content {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-3) var(--space-5);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.demo-banner__icon {
  font-size: 1.25rem;
  flex-shrink: 0;
}

.demo-banner__text {
  margin: 0;
  flex: 1;
  font-size: 0.95rem;
  color: #8B5A00;
  font-weight: 500;
}

.demo-banner__actions {
  display: flex;
  gap: var(--space-3);
  flex-shrink: 0;
}

.demo-banner__btn {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
  border: none;
}

.demo-banner__btn--primary {
  background: #0A66C2;
  color: white;
}

.demo-banner__btn--primary:hover {
  background: #0855A0;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(10, 102, 194, 0.25);
}

.demo-banner__btn--secondary {
  background: white;
  color: #666;
  border: 1px solid #DDD;
}

.demo-banner__btn--secondary:hover {
  background: #F5F5F5;
  border-color: #AAA;
}

@media (max-width: 768px) {
  .demo-banner__content {
    padding: var(--space-2) var(--space-3);
    flex-direction: column;
    text-align: center;
  }

  .demo-banner__text {
    font-size: 0.9rem;
  }

  .demo-banner__actions {
    width: 100%;
    justify-content: center;
  }
}
</style>
