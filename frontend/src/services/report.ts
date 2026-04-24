import http from './http';
import type {
  CompletenessCheckResponse,
  ExportReportResponse,
  GeneratedReportResponse,
  GrowthPlanResponse,
  ReportSnapshot,
  StudentProfile,
  ProfileScoring,
} from '@/types/api';

export async function generateReport(studentProfile: StudentProfile, jobId?: string, targetJob?: string): Promise<GeneratedReportResponse> {
  const response = await http.post<GeneratedReportResponse>('/report/generate', {
    student_profile: studentProfile,
    job_id: jobId,
    target_job: targetJob,
  }, {
    timeout: 120000,
  });
  return response.data;
}

export async function polishReport(reportContent: Record<string, unknown>, polishScope: string, polishStyle: string) {
  const response = await http.post('/report/polish', {
    report_content: reportContent,
    polish_scope: polishScope,
    polish_style: polishStyle,
  }, {
    timeout: 120000,
  });
  return response.data;
}

export async function checkCompleteness(reportContent: Record<string, unknown>): Promise<CompletenessCheckResponse> {
  const response = await http.post<CompletenessCheckResponse>('/report/check-completeness', {
    report_content: reportContent,
  });
  return response.data;
}

export async function generateGrowthPlan(studentProfile: StudentProfile, targetJob?: string): Promise<GrowthPlanResponse> {
  const response = await http.post<GrowthPlanResponse>('/report/growth-plan/generate', {
    student_profile: studentProfile,
    target_job: targetJob,
  }, {
    timeout: 120000,
  });
  return response.data;
}

export async function exportReport(payload: {
  reportContent: Record<string, unknown>;
  exportFormat: string;
  exportSections: string[];
  pageStyle: string;
  headerText: string;
  footerText: string;
  showPageNumbers: boolean;
  watermark?: Record<string, unknown>;
}): Promise<ExportReportResponse> {
  const response = await http.post<ExportReportResponse>('/report/export', {
    report_content: payload.reportContent,
    export_format: payload.exportFormat,
    export_sections: payload.exportSections,
    page_style: payload.pageStyle,
    header_text: payload.headerText,
    footer_text: payload.footerText,
    show_page_numbers: payload.showPageNumbers,
    watermark: payload.watermark,
  });
  return response.data;
}

export async function getLatestReportSnapshot(studentId: string, targetJob: string): Promise<ReportSnapshot> {
  const response = await http.get<ReportSnapshot>('/report/snapshot/latest', {
    params: {
      student_id: studentId,
      target_job: targetJob,
    },
  });
  return response.data;
}

export async function saveReportSnapshot(payload: {
  studentProfile: StudentProfile;
  targetJob: string;
  matchedJobId?: string;
  matchedJobTitle?: string;
  markdownContent?: string;
  growthPlan?: GrowthPlanResponse | null;
}): Promise<ReportSnapshot> {
  const response = await http.post<ReportSnapshot>('/report/snapshot/save', {
    student_profile: payload.studentProfile,
    target_job: payload.targetJob,
    matched_job_id: payload.matchedJobId,
    matched_job_title: payload.matchedJobTitle,
    markdown_content: payload.markdownContent,
    growth_plan: payload.growthPlan,
  });
  return response.data;
}

export async function getReportSnapshotHistory(studentId: string): Promise<ReportSnapshot[]> {
  const response = await http.get<ReportSnapshot[]>(`/report/snapshot/history/${encodeURIComponent(studentId)}`);
  return response.data;
}

export async function scoreProfile(studentProfile: StudentProfile, targetJob?: string): Promise<ProfileScoring> {
  const params = targetJob ? { target_job: targetJob } : undefined;
  const response = await http.post<ProfileScoring>('/report/score', studentProfile, { params });
  return response.data;
}
