<template>
  <span class="status-badge" :class="statusClass">
    <slot />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue';

type StatusType = 'incomplete' | 'pending' | 'completed' | 'success' | 'warning' | 'error';

const props = defineProps<{
  type: StatusType;
}>();

const statusClass = computed(() => {
  switch (props.type) {
    case 'incomplete':
      return 'status-badge--incomplete';
    case 'pending':
      return 'status-badge--pending';
    case 'completed':
    case 'success':
      return 'status-badge--success';
    case 'warning':
      return 'status-badge--warning';
    case 'error':
      return 'status-badge--error';
    default:
      return '';
  }
});
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
}

.status-badge--incomplete {
  background-color: #FFF2E5;
  color: #FF7A45;
}

.status-badge--pending {
  background-color: oklch(0.94 0.03 100);
  color: oklch(0.65 0.1 100);
}

.status-badge--success {
  background-color: oklch(0.94 0.03 158);
  color: var(--success-500);
}

.status-badge--warning {
  background-color: oklch(0.94 0.04 84);
  color: var(--warning-500);
}

.status-badge--error {
  background-color: oklch(0.94 0.03 28);
  color: var(--danger-500);
}
</style>
