import http from './http';
import type { CareerPathResponse, StudentProfile } from '@/types/api';

export async function getCareerPath(jobTitle: string, maxDepth = 3, nResults = 5): Promise<CareerPathResponse> {
  const response = await http.get<CareerPathResponse>(`/retrieve/career-path/${encodeURIComponent(jobTitle)}`, {
    params: { max_depth: maxDepth, n_results: nResults },
    timeout: 60000,
  });
  return response.data;
}

export async function generateCareerPath(jobTitle: string, studentProfile?: StudentProfile | null): Promise<CareerPathResponse> {
  const response = await http.post<CareerPathResponse>('/retrieve/career-path/generate', {
    job_title: jobTitle,
    student_profile: studentProfile ?? undefined,
  }, {
    timeout: 60000,
  });
  return response.data;
}
