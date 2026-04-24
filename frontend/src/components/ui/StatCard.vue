<template>
  <aside class="metric-card card-panel card-section">
    <p class="metric-card__label">{{ label }}</p>
    <strong class="metric-card__value">{{ value }}</strong>
    <p v-if="hint" class="metric-card__hint">{{ hint }}</p>
  </aside>
</template>

<script setup lang="ts">
defineProps<{
  label: string;
  value: string;
  hint?: string;
}>();
</script>

<style scoped>
.metric-card {
  display: grid;
  gap: 0.7rem;
  min-height: 100%;
  box-shadow: none;
  background: linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 99%, white), color-mix(in oklab, var(--bg-muted) 74%, white));
  border-color: color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
}

.metric-card__label {
  margin: 0;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.metric-card__value {
  font-family: var(--font-display);
  font-size: 1.3rem;
  line-height: 1.18;
  letter-spacing: -0.02em;
}

.metric-card__hint {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}
</style>
