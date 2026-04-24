import http from './http';
import type { JobOption, JobProfile, JobSourceStats } from '@/types/api';

export async function getJobs(skip = 0, limit = 1000): Promise<JobProfile[]> {
  const response = await http.get<JobProfile[]>('/jobs', { params: { skip, limit } });
  return response.data;
}

export async function getJobTitles(): Promise<string[]> {
  const response = await http.get<string[]>('/jobs/titles');
  return response.data;
}

export async function getJobOptions(computerOnly = false): Promise<JobOption[]> {
  const response = await http.get<JobOption[]>('/jobs/options', {
    params: {
      computer_only: computerOnly,
    },
  });
  return response.data;
}

export async function getJobSourceStats(): Promise<JobSourceStats> {
  const response = await http.get<JobSourceStats>('/jobs/source-stats');
  return response.data;
}

export async function getJob(jobId: string): Promise<JobProfile> {
  const response = await http.get<JobProfile>(`/jobs/${encodeURIComponent(jobId)}`);
  return response.data;
}

export async function importJobs(): Promise<{ message: string; count: number }> {
  const response = await http.post<{ message: string; count: number }>('/jobs/import');
  return response.data;
}

export async function searchSimilarJobs(query: string, nResults = 5): Promise<JobProfile[]> {
  const response = await http.get<JobProfile[]>('/jobs/search/similar', {
    params: { query, n_results: nResults },
  });
  return response.data;
}
