export type AiReviewMessage = {
  role: 'user' | 'assistant';
  content: string;
};

export type AiReviewRequest = {
  source?: string;
  target?: string;
  localeTag?: string;
  messages: AiReviewMessage[];
};

export type AiReviewSuggestion = {
  content: string;
  confidenceLevel?: number;
  explanation?: string;
};

export type AiReviewReview = {
  score: number;
  explanation: string;
};

export type AiReviewResponse = {
  message: AiReviewMessage;
  suggestions: AiReviewSuggestion[];
  review?: AiReviewReview;
};

export async function requestAiReview(payload: AiReviewRequest): Promise<AiReviewResponse> {
  if (!payload.messages.length) {
    throw new Error('AI review requires at least one message.');
  }

  return postJson<AiReviewResponse>('/api/ai/review', payload);
}

async function postJson<TResponse>(url: string, body: unknown): Promise<TResponse> {
  const response = await fetch(url, {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(body),
  });

  const text = await response.text();
  if (!response.ok) {
    const error: Error & { status?: number } = new Error(
      text || `Request failed with status ${response.status}`,
    );
    error.status = response.status;
    throw error;
  }

  if (!text) {
    throw new Error('AI review returned an empty response.');
  }

  return JSON.parse(text) as TResponse;
}
