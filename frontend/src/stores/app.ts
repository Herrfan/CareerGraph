import { ref } from 'vue';
import { defineStore } from 'pinia';

export const useAppStore = defineStore('app', () => {
  const isDemoMode = ref(false);

  function enterDemoMode() {
    isDemoMode.value = true;
  }

  function exitDemoMode() {
    isDemoMode.value = false;
  }

  function toggleDemoMode() {
    isDemoMode.value = !isDemoMode.value;
  }

  return {
    isDemoMode,
    enterDemoMode,
    exitDemoMode,
    toggleDemoMode,
  };
});
