import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import type { CategoryMatch, JobMatch, MatchResponse, StudentProfile } from '@/types/api';
import { batchMatch, calculateMatch, getCategories } from '@/services/match';

export const useMatchingStore = defineStore('matching', () => {
  const categories = ref<CategoryMatch[]>([]);
  const jobs = ref<JobMatch[]>([]);
  const overviewMatches = ref<JobMatch[]>([]);
  const selectedCategory = ref('');
  const selectedJob = ref<JobMatch | null>(null);
  const currentMatch = ref<MatchResponse | null>(null);
  const isLoadingCategories = ref(false);
  const isLoadingJobs = ref(false);
  const errorMessage = ref('');

  const hasResults = computed(() => categories.value.length > 0 || jobs.value.length > 0);

  async function loadMatchingOverview(studentProfile: StudentProfile) {
    isLoadingCategories.value = true;
    errorMessage.value = '';

    try {
      const batchResponse = await batchMatch(studentProfile, 10);
      overviewMatches.value = batchResponse.matches ?? [];

      const categoryResponse = await getCategories(studentProfile);
      categories.value = categoryResponse.categories ?? [];

      if (categories.value.length > 0) {
        await selectCategory(studentProfile, categories.value[0].category);
      } else {
        jobs.value = [];
        selectedCategory.value = '';
        selectedJob.value = null;
      }
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '加载岗位匹配概览失败';
      throw error;
    } finally {
      isLoadingCategories.value = false;
    }
  }

  async function selectCategory(studentProfile: StudentProfile, category: string) {
    selectedCategory.value = category;
    isLoadingJobs.value = true;
    errorMessage.value = '';

    try {
      const batchResponse = await batchMatch(studentProfile, 5, category);
      jobs.value = batchResponse.matches ?? [];
      selectedJob.value = jobs.value[0] ?? null;
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '加载岗位列表失败';
      jobs.value = [];
      selectedJob.value = null;
      throw error;
    } finally {
      isLoadingJobs.value = false;
    }
  }

  async function calculateSelectedJobMatch(studentProfile: StudentProfile, jobId?: string) {
    currentMatch.value = await calculateMatch(studentProfile, jobId);
    return currentMatch.value;
  }

  function setSelectedJob(job: JobMatch | null) {
    selectedJob.value = job;
  }

  function reset() {
    categories.value = [];
    jobs.value = [];
    overviewMatches.value = [];
    selectedCategory.value = '';
    selectedJob.value = null;
    currentMatch.value = null;
    errorMessage.value = '';
  }

  return {
    categories,
    jobs,
    overviewMatches,
    selectedCategory,
    selectedJob,
    currentMatch,
    isLoadingCategories,
    isLoadingJobs,
    errorMessage,
    hasResults,
    loadMatchingOverview,
    selectCategory,
    calculateSelectedJobMatch,
    setSelectedJob,
    reset,
  };
});
