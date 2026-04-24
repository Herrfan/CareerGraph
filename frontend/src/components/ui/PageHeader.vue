<template>
  <div class="page-header">
    <div class="page-header__main">
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1 class="title-lg">{{ title }}</h1>
      <p v-if="description" class="text-secondary page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow?: string;
  title: string;
  description?: string;
}>();
</script>

<style scoped>
.page-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-6);
  align-items: start;
  padding: 0 0 var(--space-6);
  border-bottom: 1px solid color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
}

.page-header__main {
  display: grid;
  gap: var(--space-3);
  max-width: 62rem;
}

.page-header__description {
  margin: 0;
  max-width: 68ch;
  line-height: 1.75;
}

.page-header__actions {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 0.75rem;
  flex-wrap: wrap;
  align-self: start;
}

@media (max-width: 720px) {
  .page-header {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .page-header__actions {
    justify-content: flex-start;
  }
}
</style>
