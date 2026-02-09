import type { IcuParameterDescriptor } from '../utils/icuMessageFormat';
import { requestAiReview } from './ai-review';

export type AiIcuValueRequest = {
  message: string;
  locale: string;
  parameters: IcuParameterDescriptor[];
  currentValues: Record<string, string>;
};

export async function requestAiIcuParameterValues({
  message,
  locale,
  parameters,
  currentValues,
}: AiIcuValueRequest): Promise<Record<string, string>> {
  if (parameters.length === 0) {
    return {};
  }

  const response = await requestAiReview({
    source: message,
    target: '',
    localeTag: locale,
    messages: [
      {
        role: 'user',
        content: buildIcuValuePrompt(message, locale, parameters, currentValues),
      },
    ],
  });

  const parsed = parseAiJsonObject(response.message.content);
  if (!parsed) {
    throw new Error('AI did not return valid JSON object values.');
  }

  const allowedNames = new Set(parameters.map((parameter) => parameter.name));
  const normalized: Record<string, string> = {};
  Object.entries(parsed).forEach(([name, value]) => {
    if (!allowedNames.has(name)) {
      return;
    }
    if (value == null) {
      normalized[name] = '';
      return;
    }
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
      normalized[name] = String(value);
      return;
    }
    normalized[name] = JSON.stringify(value);
  });
  return normalized;
}

function buildIcuValuePrompt(
  message: string,
  locale: string,
  parameters: IcuParameterDescriptor[],
  currentValues: Record<string, string>,
) {
  const parametersSummary = parameters
    .map((parameter) => ({
      name: parameter.name,
      kinds: parameter.kinds,
      selectOptions: parameter.selectOptions,
      pluralOptions: parameter.pluralOptions,
    }))
    .map((item) => JSON.stringify(item))
    .join('\n');

  return [
    'You generate sample values for ICU message formatting.',
    'Return ONLY a JSON object (no markdown, no explanation).',
    `Locale: ${locale}`,
    `ICU message: ${message}`,
    'Parameters:',
    parametersSummary,
    `Current values: ${JSON.stringify(currentValues)}`,
    'Rules:',
    '- Only include keys listed in Parameters.',
    '- For number/plural, return values as strings containing numbers.',
    '- For date/time, return ISO-like strings compatible with datetime-local input (YYYY-MM-DDTHH:mm).',
    '- For select, use one of listed options.',
  ].join('\n');
}

function parseAiJsonObject(text: string): Record<string, unknown> | null {
  const trimmed = text.trim();
  const directObject = tryParseJson(trimmed);
  if (directObject) {
    return directObject;
  }

  const fencedMatch = trimmed.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
  if (fencedMatch?.[1]) {
    return tryParseJson(fencedMatch[1].trim());
  }

  const start = trimmed.indexOf('{');
  const end = trimmed.lastIndexOf('}');
  if (start >= 0 && end > start) {
    return tryParseJson(trimmed.slice(start, end + 1));
  }

  return null;
}

function tryParseJson(value: string): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(value) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return null;
    }
    return parsed as Record<string, unknown>;
  } catch {
    return null;
  }
}
