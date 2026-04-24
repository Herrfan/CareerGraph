import http from './http';
import type { KnowledgeSnippet } from '@/types/api';

export async function chatWithAi(message: string, sessionId?: string, context?: string) {
  const response = await http.post<{ session_id: string; reply: string }>('/ai/chat', {
    sessionId,
    message,
    context,
  });
  return response.data;
}

export async function searchKnowledge(query: string, targetJob?: string) {
  const response = await http.get<{ query: string; target_job: string; snippets: KnowledgeSnippet[] }>('/ai/knowledge-search', {
    params: {
      query,
      target_job: targetJob,
    },
    timeout: 5000,
  });
  return response.data;
}
