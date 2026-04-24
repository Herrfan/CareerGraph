import http from './http';
import type { AdminJobRecord } from '@/types/api';

export async function getAdminJobs(): Promise<AdminJobRecord[]> {
  const response = await http.get<AdminJobRecord[]>('/admin/jobs');
  return response.data;
}

export async function updateAdminJob(sourceId: string, payload: Partial<AdminJobRecord>): Promise<AdminJobRecord> {
  const response = await http.put<AdminJobRecord>(`/admin/jobs/${encodeURIComponent(sourceId)}`, {
    company_name: payload.companyName,
    job_title: payload.jobTitle,
    job_category: payload.jobCategory,
    city: payload.city,
    industry: payload.industry,
    salary_text: payload.salaryText,
    experience_text: payload.experienceText,
    education_level: payload.educationLevel,
    job_description: payload.jobDescription,
    skills: payload.skills,
    status: payload.status,
  });
  return response.data;
}
