<template>
  <div class="report-toolbar card-panel card-section">
    <div>
      <p class="eyebrow">报告操作</p>
      <h3 class="title-lg">生成、检查与导出</h3>
    </div>
    <div class="report-toolbar__actions">
      <AppButton :disabled="disabled" @click="$emit('generate')">生成报告</AppButton>
      <AppButton variant="secondary" :disabled="disabled" @click="$emit('check')">检查完整性</AppButton>
      <AppButton variant="secondary" :disabled="disabled" @click="$emit('polish')">润色表达</AppButton>
      <AppButton variant="secondary" :disabled="disabled" @click="$emit('export')">导出报告</AppButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue';

defineProps<{
  disabled?: boolean;
}>();

defineEmits<{
  generate: [];
  check: [];
  polish: [];
  export: [];
}>();
</script>

<style scoped>
.report-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.report-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

@media (max-width: 860px) {
  .report-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
