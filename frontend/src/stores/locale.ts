import { ref } from 'vue';
import { defineStore } from 'pinia';

const LOCALE_KEY = 'career-agent-plus.locale';

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref<'zh' | 'en'>((localStorage.getItem(LOCALE_KEY) as 'zh' | 'en') || 'zh');

  function setLocale(value: 'zh' | 'en') {
    locale.value = value;
    localStorage.setItem(LOCALE_KEY, value);
  }

  return {
    locale,
    setLocale,
  };
});
