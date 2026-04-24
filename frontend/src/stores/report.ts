import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import type {
  CareerPathResponse,
  CompletenessCheckResponse,
  ExportReportResponse,
  GeneratedReportResponse,
  GraphRoleContext,
  GrowthPlanResponse,
  JobMatch,
  KnowledgeSnippet,
  ReportSnapshot,
  SkillGapAnalysis,
  StudentProfile,
} from '@/types/api';
import {
  checkCompleteness,
  exportReport,
  generateGrowthPlan,
  generateReport,
  getLatestReportSnapshot,
  getReportSnapshotHistory,
  saveReportSnapshot,
  polishReport,
} from '@/services/report';
import { generateCareerPath, getCareerPath } from '@/services/retrieve';

export const useReportStore = defineStore('report', () => {
  const targetJob = ref('');
  const sourceJob = ref<JobMatch | null>(null);
  const report = ref<GeneratedReportResponse | null>(null);
  const growthPlan = ref<GrowthPlanResponse | null>(null);
  const careerPath = ref<CareerPathResponse | null>(null);
  const skillGap = ref<SkillGapAnalysis | null>(null);
  const roleContext = ref<GraphRoleContext | null>(null);
  const knowledgeSnippets = ref<KnowledgeSnippet[]>([]);
  const completeness = ref<CompletenessCheckResponse | null>(null);
  const exportResult = ref<ExportReportResponse | null>(null);
  const latestSnapshot = ref<ReportSnapshot | null>(null);
  const snapshotHistory = ref<ReportSnapshot[]>([]);
  const isLoading = ref(false);
  const toolbarLoading = ref(false);
  const errorMessage = ref('');

  const hasReportData = computed(() => !!report.value || !!growthPlan.value || !!careerPath.value);

  function buildReportContent(studentProfile: StudentProfile | null) {
    return {
      student_profile: studentProfile,
      target_job: targetJob.value,
      matched_job: sourceJob.value?.job,
      career_path: careerPath.value,
      growth_plan: growthPlan.value,
      markdown_content: report.value?.markdown_content,
    };
  }

  function setTargetContext(jobTitle: string, jobMatch?: JobMatch | null) {
    targetJob.value = jobTitle;
    sourceJob.value = jobMatch ?? null;
  }

  function setReportMarkdown(markdown: string) {
    if (!report.value) {
      report.value = { success: true, markdown_content: markdown };
      return;
    }
    report.value = { ...report.value, markdown_content: markdown };
  }

  async function persistSnapshot(studentProfile: StudentProfile) {
    if (!studentProfile?.student_id || !targetJob.value) {
      return null;
    }

    latestSnapshot.value = await saveReportSnapshot({
      studentProfile,
      targetJob: targetJob.value,
      matchedJobId: sourceJob.value?.job.job_id,
      matchedJobTitle: sourceJob.value?.job.title,
      markdownContent: report.value?.markdown_content,
      growthPlan: growthPlan.value,
    });
    return latestSnapshot.value;
  }

  async function loadCareerPath(jobTitle: string, studentProfile?: StudentProfile | null) {
    isLoading.value = true;
    errorMessage.value = '';

    try {
      targetJob.value = jobTitle;
      try {
        careerPath.value = await generateCareerPath(jobTitle, studentProfile);
      } catch {
        careerPath.value = await getCareerPath(jobTitle, 4, 5);
      }
      return careerPath.value;
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '加载职业路径失败';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function loadSkillGap(studentProfile: StudentProfile, jobTitle?: string) {
    void studentProfile;
    void jobTitle;
    skillGap.value = null;
    return null;
  }

  async function loadRoleContext(jobTitle?: string) {
    void jobTitle;
    roleContext.value = null;
    return null;
  }

  async function loadKnowledgeSnippets(query: string, jobTitle?: string) {
    void query;
    void jobTitle;
    knowledgeSnippets.value = [];
    return [];
  }

  async function loadGrowthPlan(studentProfile: StudentProfile, jobTitle?: string) {
    isLoading.value = true;
    errorMessage.value = '';

    try {
      growthPlan.value = await generateGrowthPlan(studentProfile, jobTitle ?? targetJob.value);
      return growthPlan.value;
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '加载成长计划失败';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function loadLatestSnapshot(studentProfile: StudentProfile, jobTitle?: string) {
    const actualTargetJob = jobTitle ?? targetJob.value;
    if (!studentProfile?.student_id || !actualTargetJob) {
      latestSnapshot.value = null;
      return null;
    }

    try {
      latestSnapshot.value = await getLatestReportSnapshot(studentProfile.student_id, actualTargetJob);
      if (latestSnapshot.value.markdown_content) {
        report.value = {
          success: true,
          markdown_content: latestSnapshot.value.markdown_content,
        };
      }
      if (latestSnapshot.value.growth_plan) {
        growthPlan.value = latestSnapshot.value.growth_plan;
      }
      return latestSnapshot.value;
    } catch {
      latestSnapshot.value = null;
      return null;
    }
  }

  async function loadSnapshotHistory(studentProfile: StudentProfile) {
    if (!studentProfile?.student_id) {
      snapshotHistory.value = [];
      return [];
    }

    try {
      snapshotHistory.value = await getReportSnapshotHistory(studentProfile.student_id);
      return snapshotHistory.value;
    } catch {
      snapshotHistory.value = [];
      return [];
    }
  }

  function applySnapshot(snapshot: ReportSnapshot) {
    latestSnapshot.value = snapshot;
    targetJob.value = snapshot.target_job;
    if (snapshot.markdown_content) {
      report.value = {
        success: true,
        markdown_content: snapshot.markdown_content,
      };
    }
    if (snapshot.growth_plan) {
      growthPlan.value = snapshot.growth_plan;
    }
  }

  async function createReport(studentProfile: StudentProfile, jobId?: string, jobTitle?: string) {
    isLoading.value = true;
    errorMessage.value = '';

    try {
      report.value = await generateReport(studentProfile, jobId, jobTitle ?? targetJob.value);
      return report.value;
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '生成报告失败';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function runCompletenessCheck(studentProfile: StudentProfile | null) {
    toolbarLoading.value = true;
    try {
      completeness.value = await checkCompleteness(buildReportContent(studentProfile));
      return completeness.value;
    } finally {
      toolbarLoading.value = false;
    }
  }

  async function runPolish(studentProfile: StudentProfile | null, scope: string, style: string) {
    toolbarLoading.value = true;
    try {
      return await polishReport(buildReportContent(studentProfile), scope, style);
    } finally {
      toolbarLoading.value = false;
    }
  }

  async function runExport(studentProfile: StudentProfile | null, options: {
    exportFormat: string;
    exportSections: string[];
    pageStyle: string;
    headerText: string;
    footerText: string;
    showPageNumbers: boolean;
    watermark?: Record<string, unknown>;
  }) {
    toolbarLoading.value = true;
    try {
      exportResult.value = await exportReport({
        reportContent: buildReportContent(studentProfile),
        ...options,
      });
      return exportResult.value;
    } finally {
      toolbarLoading.value = false;
    }
  }

  function reset() {
    targetJob.value = '';
    sourceJob.value = null;
    report.value = null;
    growthPlan.value = null;
    careerPath.value = null;
    skillGap.value = null;
    roleContext.value = null;
    knowledgeSnippets.value = [];
    completeness.value = null;
    exportResult.value = null;
    latestSnapshot.value = null;
    snapshotHistory.value = [];
    errorMessage.value = '';
  }

  return {
    targetJob,
    sourceJob,
    report,
    growthPlan,
    careerPath,
    skillGap,
    roleContext,
    knowledgeSnippets,
    completeness,
    exportResult,
    latestSnapshot,
    snapshotHistory,
    isLoading,
    toolbarLoading,
    errorMessage,
    hasReportData,
    buildReportContent,
    setTargetContext,
    setReportMarkdown,
    persistSnapshot,
    loadSnapshotHistory,
    applySnapshot,
    loadCareerPath,
    loadSkillGap,
    loadRoleContext,
    loadKnowledgeSnippets,
    loadGrowthPlan,
    loadLatestSnapshot,
    createReport,
    runCompletenessCheck,
    runPolish,
    runExport,
    reset,
  };
});
