import './ai-chat-review.css';

import { type FormEvent, useEffect, useMemo, useRef } from 'react';

import type { AiReviewReview, AiReviewSuggestion } from '../api/ai-review';

export type AiChatReviewMessage = {
  id: string;
  sender: 'user' | 'assistant';
  content: string;
  suggestions?: AiReviewSuggestion[];
  review?: AiReviewReview;
};

type AiChatReviewProps = {
  messages: AiChatReviewMessage[];
  input: string;
  onChangeInput: (value: string) => void;
  onSubmit: () => void;
  onUseSuggestion: (suggestion: AiReviewSuggestion) => void;
  isResponding: boolean;
  className?: string;
};

export function AiChatReview({
  messages,
  input,
  onChangeInput,
  onSubmit,
  onUseSuggestion,
  isResponding,
  className,
}: AiChatReviewProps) {
  const firstReviewMessage = useMemo(() => {
    for (const message of messages) {
      if (message.sender === 'assistant' && message.review) {
        return message;
      }
    }
    return null;
  }, [messages]);

  const threadRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const thread = threadRef.current;
    if (!thread) {
      return;
    }
    thread.scrollTo({ top: thread.scrollHeight });
  }, [isResponding, messages]);

  return (
    <div className={className}>
      <div className="ai-chat-review__thread" ref={threadRef}>
        {messages.map((message) => {
          const review = message.review;
          const showReview =
            message.id === firstReviewMessage?.id &&
            message.sender === 'assistant' &&
            Boolean(review);
          const reviewBadge = review ? getReviewBadge(review.score) : null;
          const reviewSummary = review?.explanation?.trim() || message.content;
          const suggestions = message.suggestions ?? [];

          return (
            <div
              key={message.id}
              className={`ai-chat-review__message ai-chat-review__message--${message.sender}`}
            >
              {showReview && review ? (
                <div className="ai-chat-review__review">
                  {reviewBadge ? (
                    <span
                      className={`ai-chat-review__review-badge${
                        reviewBadge.className ? ` ${reviewBadge.className}` : ''
                      }`}
                    >
                      {reviewBadge.label}
                    </span>
                  ) : null}
                  <p>{reviewSummary}</p>
                </div>
              ) : (
                <p className="ai-chat-review__message-content">{message.content}</p>
              )}

              {suggestions.length > 0 ? (
                <div className="ai-chat-review__suggestions">
                  {suggestions.map((suggestion, suggestionIndex) => (
                    <div
                      key={`${message.id}-suggestion-${suggestionIndex}`}
                      className="ai-chat-review__suggestion"
                    >
                      <span className="ai-chat-review__suggestion-score">
                        {formatSuggestionScore(suggestion.confidenceLevel)}
                      </span>
                      <div className="ai-chat-review__suggestion-main">
                        <span className="ai-chat-review__suggestion-content">
                          {suggestion.content}
                        </span>
                        {suggestion.explanation ? (
                          <span className="ai-chat-review__suggestion-explanation">
                            {suggestion.explanation}
                          </span>
                        ) : null}
                      </div>
                      <div className="ai-chat-review__suggestion-actions">
                        <button
                          type="button"
                          className="ai-chat-review__button"
                          onClick={() => onUseSuggestion(suggestion)}
                        >
                          Use
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : null}
            </div>
          );
        })}

        {isResponding ? (
          <div className="ai-chat-review__state">
            <span className="spinner spinner--md" aria-hidden />
            <span>Thinking…</span>
          </div>
        ) : null}
      </div>

      <form
        className="ai-chat-review__form"
        onSubmit={(event: FormEvent<HTMLFormElement>) => {
          event.preventDefault();
          onSubmit();
        }}
      >
        <input
          type="text"
          value={input}
          onChange={(event) => onChangeInput(event.target.value)}
          placeholder="Ask AI for a suggestion"
          disabled={isResponding}
        />
        <button
          type="submit"
          className="ai-chat-review__button ai-chat-review__button--primary"
          disabled={isResponding || input.trim().length === 0}
        >
          Ask
        </button>
      </form>
    </div>
  );
}

function getReviewBadge(score: number): { label: string; className: string } | null {
  if (!Number.isFinite(score)) {
    return null;
  }
  switch (score) {
    case 0:
      return {
        label: 'Bad',
        className: 'ai-chat-review__review-badge--bad',
      };
    case 2:
      return {
        label: 'Good',
        className: '',
      };
    default:
      return {
        label: 'Average',
        className: 'ai-chat-review__review-badge--average',
      };
  }
}

function formatSuggestionScore(confidenceLevel: number | undefined): string {
  if (typeof confidenceLevel !== 'number' || !Number.isFinite(confidenceLevel)) {
    return '—';
  }

  if (Number.isInteger(confidenceLevel)) {
    return String(confidenceLevel);
  }

  return confidenceLevel.toFixed(2).replace(/\.?0+$/, '');
}
