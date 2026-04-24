import http from './http';
import type { ChatMessageDTO, ReportSnapshot, StudentProfileSnapshot } from '@/types/api';

export async function getMyHistory(): Promise<{
  profiles: StudentProfileSnapshot[];
  reports: ReportSnapshot[];
  chats: ChatMessageDTO[];
}> {
  const response = await http.get('/history');
  return response.data;
}
