import http from './http';
import type { CategoryMatch, JobMatch, MatchResponse, StudentProfile } from '@/types/api';

export async function calculateMatch(studentProfile: StudentProfile, jobId?: string): Promise<MatchResponse> {
  const response = await http.post<MatchResponse>('/match/calculate', {
    student_profile: studentProfile,
    job_id: jobId,
  });
  return response.data;
}

export async function batchMatch(
  studentProfile: StudentProfile,
  topN = 10,
  category?: string,
): Promise<{ matches: JobMatch[] }> {
  const response = await http.post<{ matches: JobMatch[] }>('/match/batch', {
    student_profile: studentProfile,
    category,
    top_n: topN,
  });
  return response.data;
}

export async function getCategories(studentProfile: StudentProfile): Promise<{ categories: CategoryMatch[] }> {
  const response = await http.post<{ categories: CategoryMatch[] }>('/match/categories', {
    student_profile: studentProfile,
  });
  return response.data;
}

export async function getCategoryJobs(category: string, limit = 5): Promise<{ jobs: JobMatch[] }> {
  const response = await http.get<{ jobs: JobMatch[] }>(`/match/jobs/${encodeURIComponent(category)}`, {
    params: { limit },
  });
  return response.data;
}
