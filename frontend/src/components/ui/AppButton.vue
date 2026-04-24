<template>
  <button :class="classes" :type="type" :disabled="disabled">
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'ghost';
    type?: 'button' | 'submit' | 'reset';
    disabled?: boolean;
    block?: boolean;
  }>(),
  {
    variant: 'primary',
    type: 'button',
    disabled: false,
    block: false,
  },
);

const classes = computed(() => [
  'app-button',
  `app-button--${props.variant}`,
  props.block && 'app-button--block',
]);
</script>

<style scoped>
.app-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  min-height: 44px;
  padding: 0.82rem 1.15rem;
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  font-weight: 700;
  letter-spacing: 0.01em;
  transition: border-color var(--transition-base), background var(--transition-base), color var(--transition-base), box-shadow var(--transition-base), transform var(--transition-base);
}

.app-button:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.app-button--block {
  width: 100%;
}

.app-button--primary {
  color: white;
  background: linear-gradient(180deg, color-mix(in oklab, var(--brand-500) 90%, white), var(--brand-600));
  border-color: color-mix(in oklab, var(--brand-700) 70%, black);
  box-shadow: 0 10px 24px color-mix(in oklab, var(--brand-700) 16%, transparent);
}

.app-button--primary:hover:not(:disabled) {
  background: linear-gradient(180deg, color-mix(in oklab, var(--brand-500) 82%, white), color-mix(in oklab, var(--brand-700) 94%, black));
  border-color: color-mix(in oklab, var(--brand-700) 84%, black);
  transform: translateY(-1px);
}

.app-button--secondary {
  color: var(--text-primary);
  background: linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 99%, white), color-mix(in oklab, var(--bg-muted) 76%, white));
  border-color: color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  box-shadow: inset 0 1px 0 color-mix(in oklab, white 80%, transparent);
}

.app-button--secondary:hover:not(:disabled) {
  background: color-mix(in oklab, var(--bg-muted) 88%, white);
  border-color: color-mix(in oklab, var(--brand-500) 20%, var(--border-strong));
}

.app-button--ghost {
  color: var(--text-secondary);
  background: transparent;
  border-color: color-mix(in oklab, var(--brand-700) 4%, transparent);
}

.app-button--ghost:hover:not(:disabled) {
  color: var(--text-primary);
  background: color-mix(in oklab, var(--brand-700) 4%, var(--bg-muted));
}
</style>
