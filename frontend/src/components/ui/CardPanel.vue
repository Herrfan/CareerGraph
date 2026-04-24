<template>
  <section class="card-panel card-section">
    <div v-if="title || description" class="section-heading">
      <span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span>
      <h2 v-if="title" class="title-lg card-panel__title">{{ title }}</h2>
      <p v-if="description" class="text-secondary card-panel__description">{{ description }}</p>
    </div>
    <div class="card-panel__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow?: string;
  title?: string;
  description?: string;
}>();
</script>

<style scoped>
.card-panel {
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: var(--space-5);
}

.card-panel__title {
  font-size: 1.16rem;
}

.card-panel__description {
  margin: 0;
  max-width: 66ch;
  line-height: 1.6;
}

.card-panel__body {
  display: grid;
  gap: var(--space-4);
}
</style>
