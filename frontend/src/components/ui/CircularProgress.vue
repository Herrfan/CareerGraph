<template>
  <div class="circular-progress">
    <svg class="circular-progress__svg" viewBox="0 0 36 36">
      <circle
        class="circular-progress__bg"
        cx="18"
        cy="18"
        r="15"
        stroke-width="3"
      />
      <circle
        class="circular-progress__progress"
        :class="progressColor"
        cx="18"
        cy="18"
        r="15"
        stroke-width="3"
        stroke-linecap="round"
        :stroke-dasharray="`${progress} 100`"
        stroke-dashoffset="25"
        transform="rotate(-90 18 18)"
      />
    </svg>
    <div class="circular-progress__text">
      <span class="circular-progress__value">{{ displayValue }}</span>
      <span class="circular-progress__label">{{ label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  value: number | string;
  label: string;
  type?: 'primary' | 'success' | 'warning' | 'accent';
}>();

const progress = computed(() => {
  const num = Number(props.value);
  return Math.min(Math.max(num, 0), 100);
});

const displayValue = computed(() => {
  const num = Number(props.value);
  return `${Math.round(num)}%`;
});

const progressColor = computed(() => {
  const type = props.type || 'primary';
  if (type === 'accent') return 'circular-progress__progress--accent';
  if (type === 'success') return 'circular-progress__progress--success';
  if (type === 'warning') return 'circular-progress__progress--warning';
  return 'circular-progress__progress--primary';
});
</script>

<style scoped>
.circular-progress {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.circular-progress__svg {
  width: 80px;
  height: 80px;
  transform: rotate(-90deg);
}

.circular-progress__bg {
  fill: none;
  stroke: var(--border-subtle);
}

.circular-progress__progress {
  fill: none;
  transition: stroke-dasharray 0.8s ease-out;
}

.circular-progress__progress--primary {
  stroke: var(--primary-blue);
}

.circular-progress__progress--accent {
  stroke: var(--accent-orange);
}

.circular-progress__progress--success {
  stroke: var(--success-500);
}

.circular-progress__progress--warning {
  stroke: var(--warning-500);
}

.circular-progress__text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.circular-progress__value {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.circular-progress__label {
  font-size: 0.75rem;
  color: var(--text-secondary);
  font-weight: 500;
}
</style>
