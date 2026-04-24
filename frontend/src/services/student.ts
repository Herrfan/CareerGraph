import http from './http';
import type { StudentProfile, StudentProfileEvaluation } from '@/types/api';

export async function uploadStudentProfile(
  resume: File,
  expectedPosition: string,
  expectedSalary: string,
  expectedCity: string,
  onProgress?: (progress: { percent: number; loaded: number; total: number; uploadCompleted: boolean }) => void,
): Promise<StudentProfile> {
  const formData = new FormData();
  formData.append('resume', resume);
  formData.append('expected_position', expectedPosition);
  formData.append('expected_salary', expectedSalary);
  formData.append('expected_city', expectedCity);

  const response = await http.post<StudentProfile>('/student/profile', formData, {
    timeout: 180000,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (event) => {
      if (!onProgress) {
        return;
      }
      const loaded = Number(event.loaded ?? 0);
      const total = Number(event.total ?? 0);
      const safeTotal = total > 0 ? total : Math.max(loaded, 1);
      const percent = Math.round((loaded * 100) / safeTotal);
      onProgress({
        percent: Math.min(100, Math.max(0, percent)),
        loaded,
        total: safeTotal,
        uploadCompleted: loaded >= safeTotal,
      });
    },
  });

  return response.data;
}

export async function submitManualStudentProfile(profile: StudentProfile): Promise<StudentProfile> {
  const response = await http.post<StudentProfile>('/student/profile/manual', profile, {
    timeout: 120000,
  });
  return response.data;
}

export async function evaluateStudentProfile(profile: StudentProfile): Promise<StudentProfileEvaluation> {
  const response = await http.post<StudentProfileEvaluation>('/student/profile/evaluate', profile);
  return response.data;
}

export async function getMyStudentProfile(): Promise<StudentProfile> {
  const response = await http.get<StudentProfile>('/student/profile/me');
  return response.data;
}
