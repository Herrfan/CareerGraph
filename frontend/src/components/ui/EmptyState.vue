<template>
  <div class="empty-state card-panel card-section">
    <div class="empty-state__content">
      <p class="eyebrow">Workspace</p>
      <h3>{{ title }}</h3>
      <p class="text-secondary">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="empty-state__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
}>();
</script>

<style scoped>
.empty-state {
  display: grid;
  align-content: center;
  gap: var(--space-6);
  min-height: 260px;
  text-align: left;
  background:
    linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 99%, white), color-mix(in oklab, var(--bg-muted) 72%, white)),
    radial-gradient(circle at top right, color-mix(in oklab, var(--brand-500) 6%, transparent), transparent 34%);
}

.empty-state__content {
  display: grid;
  gap: var(--space-3);
  max-width: 40rem;
}

.empty-state h3,
.empty-state p {
  margin: 0;
}

.empty-state__actions {
  display: flex;
  justify-content: flex-start;
}
</style>
