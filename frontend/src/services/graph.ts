import http from './http';
import type { GraphRoleContext, GraphSimilarJob, SkillGapAnalysis, StudentProfile } from '@/types/api';

export async function getGraphSimilarJobs(jobTitle: string, limit = 5) {
  const response = await http.get<{ job_title: string; jobs: GraphSimilarJob[] }>('/graph/similar-jobs', {
    timeout: 5000,
    params: {
      job_title: jobTitle,
      limit,
    },
  });
  return response.data;
}

export async function getSkillGap(studentProfile: StudentProfile, targetJob: string) {
  const response = await http.post<SkillGapAnalysis>(
    '/graph/skill-gap',
    {
      studentProfile,
      targetJob,
    },
    {
      timeout: 5000,
    },
  );
  return response.data;
}

export async function getGraphRoleContext(jobTitle: string) {
  const response = await http.get<GraphRoleContext>('/graph/role-context', {
    timeout: 5000,
    params: {
      job_title: jobTitle,
    },
  });
  return response.data;
}
