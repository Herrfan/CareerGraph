<template>
  <div class="step-guide">
    <button
      type="button"
      :class="['step-guide__trigger', !hasViewed && 'step-guide__trigger--highlight', hasViewed && 'step-guide__trigger--seen']"
      @click="openGuide"
    >
      <span class="step-guide__eyebrow">{{ triggerEyebrow }}</span>
      <strong>{{ triggerTitle }}</strong>
      <span class="step-guide__summary">{{ triggerSummary }}</span>
      <span :class="['step-guide__status', hasViewed && 'step-guide__status--seen']">
        {{ hasViewed ? '已查看说明' : '建议先看' }}
      </span>
    </button>

    <Teleport to="body">
      <div v-if="isOpen" class="step-guide__overlay" @click.self="closeGuide">
        <section class="step-guide__dialog card-panel card-section" role="dialog" aria-modal="true" :aria-label="title">
          <div class="step-guide__dialog-header">
            <div class="step-guide__dialog-copy">
              <p class="eyebrow">下一步说明</p>
              <h2 class="title-lg">{{ title }}</h2>
              <p v-if="description" class="text-secondary">{{ description }}</p>
            </div>
            <button type="button" class="step-guide__close" @click="closeGuide" aria-label="关闭说明">关闭</button>
          </div>

          <div class="step-guide__sections">
            <article
              v-for="section in sections"
              :key="section.title"
              class="step-guide__section"
            >
              <p class="step-guide__section-title">{{ section.title }}</p>
              <p class="text-secondary">{{ section.description }}</p>
            </article>
          </div>

          <div v-if="note" class="step-guide__note">
            <p class="step-guide__note-label">影响提醒</p>
            <p>{{ note }}</p>
          </div>

          <div class="step-guide__actions">
            <AppButton variant="secondary" @click="closeGuide">我知道了</AppButton>
          </div>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, Teleport } from 'vue';
import AppButton from '@/components/ui/AppButton.vue';

type GuideSection = {
  title: string;
  description: string;
  tone?: 'accent' | 'warning';
};

const STORAGE_PREFIX = 'career-agent-plus.step-guide.';

const props = withDefaults(defineProps<{
  storageKey: string;
  title: string;
  description?: string;
  triggerEyebrow?: string;
  triggerTitle?: string;
  triggerSummary?: string;
  note?: string;
  sections: ReadonlyArray<GuideSection>;
}>(), {
  description: '',
  triggerEyebrow: 'Guide',
  triggerTitle: '查看下一步说明',
  triggerSummary: '看看当前该怎么做，以及这一步会如何影响后续页面。',
  note: '',
});

const isOpen = ref(false);
const hasViewed = ref(false);
const storageToken = computed(() => `${STORAGE_PREFIX}${props.storageKey}`);

function loadViewedState() {
  try {
    hasViewed.value = localStorage.getItem(storageToken.value) === '1';
  } catch {
    hasViewed.value = false;
  }
}

function markViewed() {
  hasViewed.value = true;
  try {
    localStorage.setItem(storageToken.value, '1');
  } catch {
    // Ignore storage errors and keep the guide usable.
  }
}

function openGuide() {
  isOpen.value = true;
  if (!hasViewed.value) {
    markViewed();
  }
}

function closeGuide() {
  isOpen.value = false;
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && isOpen.value) {
    closeGuide();
  }
}

onMounted(() => {
  loadViewedState();
  window.addEventListener('keydown', handleKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
});
</script>

<style scoped>
.step-guide {
  width: 100%;
}

.step-guide__trigger {
  position: relative;
  display: grid;
  gap: 0.4rem;
  width: min(100%, 24rem);
  min-width: 18rem;
  padding: 1rem 1.05rem;
  border-radius: 14px;
  border: 1px solid color-mix(in oklab, var(--brand-700) 8%, var(--border-subtle));
  background:
    linear-gradient(180deg, color-mix(in oklab, var(--bg-surface) 99%, white), color-mix(in oklab, var(--bg-muted) 76%, white)),
    radial-gradient(circle at top right, color-mix(in oklab, var(--brand-500) 5%, transparent) 0, transparent 34%);
  box-shadow:
    0 12px 24px color-mix(in oklab, var(--brand-700) 6%, transparent),
    inset 0 1px 0 color-mix(in oklab, white 78%, transparent);
  text-align: left;
  color: var(--text-primary);
}

.step-guide__trigger--highlight {
  border-color: color-mix(in oklab, var(--brand-500) 20%, var(--border-strong));
  box-shadow:
    0 14px 30px color-mix(in oklab, var(--brand-700) 8%, transparent),
    0 0 0 3px color-mix(in oklab, var(--brand-500) 8%, transparent),
    inset 0 1px 0 color-mix(in oklab, white 78%, transparent);
}

.step-guide__trigger--seen {
  border-color: color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  box-shadow:
    0 10px 20px color-mix(in oklab, var(--brand-700) 5%, transparent),
    inset 0 1px 0 color-mix(in oklab, white 72%, transparent);
}

.step-guide__eyebrow {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: color-mix(in oklab, var(--brand-700) 72%, var(--text-primary));
}

.step-guide__summary {
  font-size: 0.9rem;
  line-height: 1.55;
  color: color-mix(in oklab, var(--text-secondary) 88%, var(--text-primary));
}

.step-guide__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  margin-top: 0.25rem;
  padding: 0.24rem 0.65rem;
  border-radius: 999px;
  background: color-mix(in oklab, var(--brand-500) 10%, white);
  color: color-mix(in oklab, var(--brand-700) 72%, var(--text-primary));
  font-size: 0.76rem;
  font-weight: 700;
}

.step-guide__status--seen {
  background: color-mix(in oklab, var(--bg-muted) 92%, white);
  color: var(--text-secondary);
}

.step-guide__overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: var(--space-5);
  background: color-mix(in oklab, var(--text-primary) 26%, transparent);
  backdrop-filter: blur(10px);
}

.step-guide__dialog {
  display: grid;
  gap: var(--space-5);
  width: min(100%, 760px);
  max-height: min(88vh, 860px);
  overflow: auto;
  border-color: color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  box-shadow: 0 28px 58px color-mix(in oklab, var(--text-primary) 14%, transparent);
}

.step-guide__dialog-header {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  align-items: flex-start;
}

.step-guide__dialog-copy {
  display: grid;
  gap: var(--space-2);
}

.step-guide__dialog-copy p,
.step-guide__dialog-copy h2 {
  margin: 0;
}

.step-guide__close {
  min-width: 68px;
  padding: 0.55rem 0.85rem;
  border-radius: 999px;
  border: 1px solid color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-muted) 82%, white);
  color: var(--text-secondary);
}

.step-guide__sections {
  display: grid;
  gap: var(--space-3);
}

.step-guide__section {
  display: grid;
  gap: 0.5rem;
  padding: 1rem 1.05rem;
  border-radius: 12px;
  border: 1px solid color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-surface) 98%, white);
}

.step-guide__section-title,
.step-guide__section p,
.step-guide__note-label,
.step-guide__note p {
  margin: 0;
}

.step-guide__section-title,
.step-guide__note-label {
  font-size: 0.84rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.step-guide__note {
  display: grid;
  gap: 0.45rem;
  padding: 1rem 1.05rem;
  border-radius: 12px;
  border: 1px solid color-mix(in oklab, var(--brand-700) 6%, var(--border-subtle));
  background: color-mix(in oklab, var(--bg-surface) 98%, white);
}

.step-guide__actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .step-guide__trigger {
    width: 100%;
    min-width: 0;
  }

  .step-guide__dialog-header {
    display: grid;
  }

  .step-guide__actions {
    justify-content: stretch;
  }
}
</style>
