<template>
  <div class="loading-block card-panel card-section">
    <div class="loading-block__spinner" />
    <div class="loading-block__content">
      <p class="eyebrow">Loading</p>
      <p class="text-secondary">{{ label }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string;
  }>(),
  {
    label: '正在加载数据…',
  },
);
</script>

<style scoped>
.loading-block {
  display: grid;
  place-items: center;
  gap: var(--space-4);
  min-height: 220px;
}

.loading-block__content {
  display: grid;
  gap: var(--space-2);
  text-align: center;
}

.loading-block__content p {
  margin: 0;
}

.loading-block__spinner {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  border: 2px solid var(--border-subtle);
  border-top-color: var(--brand-600);
  animation: spin 0.85s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
