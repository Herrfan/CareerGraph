<template>
  <div class="app">
    <DemoBanner />
    <RouterView />
    <AICareBubble v-if="showAIBubble" :visible="aiVisible" @toggle="toggleAI" :auto-show="true" :delay="5000" :current-route="route.path" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useProfileStore } from '@/stores/profile';
import { useRoute } from 'vue-router';
import AICareBubble from '@/components/ui/AICareBubble.vue';
import DemoBanner from '@/components/ui/DemoBanner.vue';

const authStore = useAuthStore();
const profileStore = useProfileStore();
const route = useRoute();
const aiVisible = ref(true);

const showAIBubble = computed(() => {
  const allowedRoutes = ['/', '/profile', '/matching', '/career-path', '/report'];
  return allowedRoutes.includes(route.path);
});

const toggleAI = () => {
  aiVisible.value = !aiVisible.value;
};

onMounted(async () => {
  await authStore.bootstrap();
  if (authStore.isAuthenticated) {
    await profileStore.loadMyProfile();
  } else if (!authStore.isGuest) {
    profileStore.clearProfile();
  }
});
</script>

<style scoped>
.app {
  min-height: 100vh;
  position: relative;
}
</style>
