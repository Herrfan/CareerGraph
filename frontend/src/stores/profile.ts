import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import type { StudentProfile, StudentProfileEvaluation } from '@/types/api';
import { evaluateStudentProfile, getMyStudentProfile, submitManualStudentProfile, uploadStudentProfile } from '@/services/student';

export const useProfileStore = defineStore('profile', () => {
  const studentProfile = ref<StudentProfile | null>(null);
  const isSubmitting = ref(false);
  const uploadProgress = ref(0);
  const uploadLoaded = ref(0);
  const uploadTotal = ref(0);
  const uploadStage = ref<'idle' | 'uploading' | 'processing'>('idle');
  const errorMessage = ref('');
  const evaluation = ref<StudentProfileEvaluation | null>(null);
  let processingTimer: ReturnType<typeof setInterval> | null = null;

  const hasProfile = computed(() => !!studentProfile.value);

  function stopProcessingTimer() {
    if (processingTimer) {
      clearInterval(processingTimer);
      processingTimer = null;
    }
  }

  function startProcessingProgress() {
    stopProcessingTimer();
    uploadStage.value = 'processing';
    processingTimer = setInterval(() => {
      if (uploadProgress.value < 99) {
        uploadProgress.value += uploadProgress.value < 90 ? 2 : 1;
      } else {
        stopProcessingTimer();
      }
    }, 400);
  }

  async function loadMyProfile() {
    try {
      studentProfile.value = await getMyStudentProfile();
      evaluation.value = await evaluateStudentProfile(studentProfile.value);
      return studentProfile.value;
    } catch {
      studentProfile.value = null;
      evaluation.value = null;
      return null;
    }
  }

  async function createProfile(payload: {
    resume: File;
    expectedPosition: string;
    expectedSalary: string;
    expectedCity: string;
  }) {
    isSubmitting.value = true;
    uploadProgress.value = 0;
    uploadLoaded.value = 0;
    uploadTotal.value = 0;
    uploadStage.value = 'uploading';
    errorMessage.value = '';

    try {
      studentProfile.value = await uploadStudentProfile(
        payload.resume,
        payload.expectedPosition,
        payload.expectedSalary,
        payload.expectedCity,
        ({ percent, loaded, total, uploadCompleted }) => {
          uploadProgress.value = Math.min(percent, 85);
          uploadLoaded.value = loaded;
          uploadTotal.value = total;
          if (uploadCompleted && uploadStage.value !== 'processing') {
            startProcessingProgress();
          }
        },
      );
      stopProcessingTimer();
      uploadProgress.value = 100;
      uploadStage.value = 'idle';
      evaluation.value = await evaluateStudentProfile(studentProfile.value);
      return studentProfile.value;
    } catch (error) {
      stopProcessingTimer();
      uploadStage.value = 'idle';
      errorMessage.value = error instanceof Error ? error.message : '简历解析失败';
      throw error;
    } finally {
      stopProcessingTimer();
      uploadStage.value = 'idle';
      isSubmitting.value = false;
    }
  }

  function setProfile(profile: StudentProfile | null) {
    studentProfile.value = profile;
  }

  async function createManualProfile(profile: StudentProfile) {
    isSubmitting.value = true;
    uploadProgress.value = 0;
    uploadLoaded.value = 0;
    uploadTotal.value = 0;
    uploadStage.value = 'processing';
    errorMessage.value = '';

    try {
      startProcessingProgress();
      studentProfile.value = await submitManualStudentProfile(profile);
      stopProcessingTimer();
      uploadProgress.value = 100;
      uploadStage.value = 'idle';
      evaluation.value = await evaluateStudentProfile(studentProfile.value);
      return studentProfile.value;
    } catch (error) {
      stopProcessingTimer();
      uploadStage.value = 'idle';
      errorMessage.value = error instanceof Error ? error.message : '手动画像提交失败';
      throw error;
    } finally {
      stopProcessingTimer();
      uploadStage.value = 'idle';
      isSubmitting.value = false;
    }
  }

  function clearProfile() {
    stopProcessingTimer();
    studentProfile.value = null;
    errorMessage.value = '';
    evaluation.value = null;
    uploadProgress.value = 0;
    uploadLoaded.value = 0;
    uploadTotal.value = 0;
    uploadStage.value = 'idle';
  }

  return {
    studentProfile,
    isSubmitting,
    uploadProgress,
    uploadLoaded,
    uploadTotal,
    uploadStage,
    errorMessage,
    evaluation,
    hasProfile,
    loadMyProfile,
    createProfile,
    createManualProfile,
    setProfile,
    clearProfile,
  };
});
