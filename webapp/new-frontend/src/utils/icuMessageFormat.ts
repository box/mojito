import { type MessageFormatElement, parse, TYPE } from '@formatjs/icu-messageformat-parser';
import IntlMessageFormat from 'intl-messageformat';

export type IcuParameterKind = 'string' | 'number' | 'date' | 'time' | 'select' | 'plural';

export type IcuParameterDescriptor = {
  name: string;
  kinds: IcuParameterKind[];
  selectOptions: string[];
  pluralOptions: string[];
};

export type IcuMessageParseResult = {
  ast: MessageFormatElement[];
  parameters: IcuParameterDescriptor[];
};

export type IcuValueSet = {
  id: string;
  label: string;
  values: Record<string, string>;
};

export type IcuPluralCategory = 'zero' | 'one' | 'two' | 'few' | 'many' | 'other';

const ICU_PLURAL_CATEGORIES: IcuPluralCategory[] = ['zero', 'one', 'two', 'few', 'many', 'other'];

type MutableParameterDescriptor = {
  name: string;
  kinds: Set<IcuParameterKind>;
  selectOptions: Set<string>;
  pluralOptions: Set<string>;
};

const FALLBACK_DATE_TIME = '2026-02-05T09:30';

type MessageFormatParser = typeof parse;

function ensureIntlMessageFormatParser() {
  const formatter = IntlMessageFormat as typeof IntlMessageFormat & {
    __parse?: MessageFormatParser;
  };
  if (typeof formatter.__parse !== 'function') {
    formatter.__parse = parse;
  }
}

export function parseIcuMessage(message: string): IcuMessageParseResult {
  const ast = parse(message, {
    ignoreTag: true,
    requiresOtherClause: false,
  });

  const parameterMap = new Map<string, MutableParameterDescriptor>();

  const ensureParameter = (name: string) => {
    const existing = parameterMap.get(name);
    if (existing) {
      return existing;
    }
    const next: MutableParameterDescriptor = {
      name,
      kinds: new Set<IcuParameterKind>(),
      selectOptions: new Set<string>(),
      pluralOptions: new Set<string>(),
    };
    parameterMap.set(name, next);
    return next;
  };

  const collect = (elements: MessageFormatElement[]) => {
    elements.forEach((element) => {
      switch (element.type) {
        case TYPE.argument: {
          const descriptor = ensureParameter(element.value);
          descriptor.kinds.add('string');
          return;
        }
        case TYPE.number: {
          const descriptor = ensureParameter(element.value);
          descriptor.kinds.add('number');
          return;
        }
        case TYPE.date: {
          const descriptor = ensureParameter(element.value);
          descriptor.kinds.add('date');
          return;
        }
        case TYPE.time: {
          const descriptor = ensureParameter(element.value);
          descriptor.kinds.add('time');
          return;
        }
        case TYPE.select: {
          const descriptor = ensureParameter(element.value);
          descriptor.kinds.add('select');
          Object.entries(element.options).forEach(([option, value]) => {
            descriptor.selectOptions.add(option);
            collect(value.value);
          });
          return;
        }
        case TYPE.plural: {
          const descriptor = ensureParameter(element.value);
          descriptor.kinds.add('plural');
          Object.entries(element.options).forEach(([option, value]) => {
            descriptor.pluralOptions.add(option);
            collect(value.value);
          });
          return;
        }
        default:
          return;
      }
    });
  };

  collect(ast);

  return {
    ast,
    parameters: [...parameterMap.values()].map((item) => ({
      name: item.name,
      kinds: [...item.kinds],
      selectOptions: [...item.selectOptions],
      pluralOptions: [...item.pluralOptions],
    })),
  };
}

export function buildIcuExampleValueSets(
  parameters: IcuParameterDescriptor[],
  locale = 'en',
): IcuValueSet[] {
  const base: Record<string, string> = {};
  const variant: Record<string, string> = {};
  const zeroCase: Record<string, string> = {};

  parameters.forEach((parameter) => {
    base[parameter.name] = guessDefaultValue(parameter, 'base');
    variant[parameter.name] = guessDefaultValue(parameter, 'variant');
    zeroCase[parameter.name] = guessDefaultValue(parameter, 'zero');
  });

  const candidateSets: IcuValueSet[] = [
    { id: 'base', label: 'Base example', values: base },
    { id: 'variant', label: 'Variant example', values: variant },
    { id: 'zero', label: 'Zero/empty case', values: zeroCase },
  ];

  const firstOptionParam = parameters.find(
    (parameter) => parameter.selectOptions.length > 0 || parameter.pluralOptions.length > 0,
  );
  if (firstOptionParam) {
    const usesSelectOptions = firstOptionParam.selectOptions.length > 0;
    const optionValues = usesSelectOptions
      ? firstOptionParam.selectOptions
      : firstOptionParam.pluralOptions;

    optionValues.forEach((option, index) => {
      const optionValue = usesSelectOptions ? option : getPluralSampleValue(option, locale);
      const values = { ...base, [firstOptionParam.name]: optionValue };
      candidateSets.push({
        id: `option-${index}`,
        label: usesSelectOptions
          ? `${firstOptionParam.name}=${option}`
          : `${firstOptionParam.name}: ${option} -> ${optionValue}`,
        values,
      });
    });
  }

  const deduped = new Map<string, IcuValueSet>();
  candidateSets.forEach((set) => {
    const key = set.id.startsWith('option-') ? set.id : `${set.id}:${JSON.stringify(set.values)}`;
    if (!deduped.has(key)) {
      deduped.set(key, set);
    }
  });

  return [...deduped.values()];
}

export function getPluralCategories(locale: string): IcuPluralCategory[] {
  let rules: Intl.PluralRules;
  try {
    rules = new Intl.PluralRules(locale || 'en');
  } catch {
    try {
      rules = new Intl.PluralRules('en');
    } catch {
      return ['other'];
    }
  }

  const categories = rules.resolvedOptions().pluralCategories ?? [];
  const normalized = categories.filter((category): category is IcuPluralCategory =>
    ICU_PLURAL_CATEGORIES.includes(category as IcuPluralCategory),
  );
  if (normalized.length === 0) {
    return ['other'];
  }

  return normalized;
}

export function getPluralCategorySampleMap(
  locale: string,
): Record<IcuPluralCategory, string | null> {
  const map: Record<IcuPluralCategory, string | null> = {
    zero: null,
    one: null,
    two: null,
    few: null,
    many: null,
    other: null,
  };

  for (const category of getPluralCategories(locale)) {
    map[category] = findSampleNumberForPluralCategory(category, locale);
  }

  return map;
}

export function getPluralSampleValue(option: string, locale: string): string {
  if (option.startsWith('=')) {
    const parsed = Number(option.slice(1));
    if (Number.isFinite(parsed)) {
      return String(parsed);
    }
  }

  const localeCandidate = findSampleNumberForPluralCategory(option, locale);
  if (localeCandidate != null) {
    return localeCandidate;
  }

  switch (option) {
    case 'zero':
      return '0';
    case 'one':
      return '1';
    case 'two':
      return '2';
    case 'few':
      return '3';
    case 'many':
      return '5';
    case 'other':
      return '7';
    default:
      return '1';
  }
}

const PLURAL_SAMPLE_CANDIDATES = [
  0, 1, 2, 3, 4, 5, 6, 10, 11, 12, 14, 15, 20, 21, 22, 24, 25, 30, 31, 32, 35, 40, 41, 42, 45, 50,
  51, 52, 55, 60, 61, 62, 65, 70, 71, 72, 75, 80, 81, 82, 85, 90, 91, 92, 95, 100, 101, 102, 111,
  1.1, 1.5, 2.1, 5.1,
];

function findSampleNumberForPluralCategory(option: string, locale: string): string | null {
  if (
    option !== 'zero' &&
    option !== 'one' &&
    option !== 'two' &&
    option !== 'few' &&
    option !== 'many' &&
    option !== 'other'
  ) {
    return null;
  }

  let rules: Intl.PluralRules;
  try {
    rules = new Intl.PluralRules(locale || 'en');
  } catch {
    try {
      rules = new Intl.PluralRules('en');
    } catch {
      return null;
    }
  }

  for (const candidate of PLURAL_SAMPLE_CANDIDATES) {
    if (rules.select(candidate) === option) {
      return String(candidate);
    }
  }

  return null;
}

export function mergeValues(
  baseValues: Record<string, string>,
  incomingValues: Record<string, string>,
  parameters: IcuParameterDescriptor[],
): Record<string, string> {
  const allowedNames = new Set(parameters.map((parameter) => parameter.name));
  const merged: Record<string, string> = { ...baseValues };

  Object.entries(incomingValues).forEach(([key, value]) => {
    if (!allowedNames.has(key)) {
      return;
    }
    merged[key] = String(value);
  });

  return merged;
}

export function renderIcuWithValues(
  ast: MessageFormatElement[] | null | undefined,
  parameters: IcuParameterDescriptor[],
  values: Record<string, string>,
  locale: string,
  message?: string,
): string {
  const normalizedMessage = typeof message === 'string' ? message.trim() : '';
  ensureIntlMessageFormatParser();

  if (Array.isArray(ast) && ast.length > 0) {
    try {
      const formatter = new IntlMessageFormat(ast, locale || 'en', undefined, {
        ignoreTag: true,
      });
      const formatted = formatter.format(coerceForFormatting(values, parameters));
      if (Array.isArray(formatted)) {
        return formatted.map((part) => String(part)).join('');
      }
      return String(formatted);
    } catch {
      // Fall back to parsing the message string when AST formatting fails.
    }
  }

  if (!normalizedMessage) {
    return '';
  }

  const formatter = new IntlMessageFormat(normalizedMessage, locale || 'en', undefined, {
    ignoreTag: true,
  });
  const formatted = formatter.format(coerceForFormatting(values, parameters));
  if (Array.isArray(formatted)) {
    return formatted.map((part) => String(part)).join('');
  }
  return String(formatted);
}

function coerceForFormatting(
  values: Record<string, string>,
  parameters: IcuParameterDescriptor[],
): Record<string, Date | number | string> {
  const output: Record<string, Date | number | string> = {};

  parameters.forEach((parameter) => {
    const raw = values[parameter.name] ?? '';
    if (hasKind(parameter, 'number') || hasKind(parameter, 'plural')) {
      const parsedNumber = Number(raw);
      output[parameter.name] = Number.isFinite(parsedNumber) ? parsedNumber : 0;
      return;
    }

    if (hasKind(parameter, 'date') || hasKind(parameter, 'time')) {
      const parsedDate = Date.parse(raw);
      output[parameter.name] = Number.isNaN(parsedDate)
        ? new Date(FALLBACK_DATE_TIME)
        : new Date(parsedDate);
      return;
    }

    output[parameter.name] = raw;
  });

  return output;
}

function hasKind(parameter: IcuParameterDescriptor, kind: IcuParameterKind) {
  return parameter.kinds.includes(kind);
}

function guessDefaultValue(
  parameter: IcuParameterDescriptor,
  variant: 'base' | 'variant' | 'zero',
): string {
  const { name } = parameter;
  const lowerName = name.toLowerCase();

  if (hasKind(parameter, 'select')) {
    return chooseOption(parameter.selectOptions, variant);
  }

  if (hasKind(parameter, 'plural')) {
    if (parameter.pluralOptions.includes(`=${variant === 'zero' ? '0' : '1'}`)) {
      return variant === 'zero' ? '0' : '1';
    }
    if (variant === 'zero') {
      return '0';
    }
    if (variant === 'variant') {
      return '5';
    }
    return '1';
  }

  if (hasKind(parameter, 'number')) {
    if (variant === 'zero') {
      return '0';
    }
    if (variant === 'variant') {
      return '42';
    }
    return lowerName.includes('price') || lowerName.includes('amount') ? '12.5' : '1';
  }

  if (hasKind(parameter, 'date') || hasKind(parameter, 'time')) {
    if (variant === 'variant') {
      return '2026-07-11T18:45';
    }
    if (variant === 'zero') {
      return '2026-01-01T00:00';
    }
    return FALLBACK_DATE_TIME;
  }

  if (lowerName.includes('name')) {
    return variant === 'variant' ? 'Sam' : 'Alex';
  }
  if (lowerName.includes('email')) {
    return variant === 'variant' ? 'sam@example.com' : 'alex@example.com';
  }
  if (lowerName.includes('city')) {
    return variant === 'variant' ? 'Austin' : 'San Francisco';
  }
  if (lowerName.includes('country')) {
    return variant === 'variant' ? 'Canada' : 'United States';
  }
  if (variant === 'zero') {
    return '';
  }
  return variant === 'variant' ? `${name}-alt` : `${name}-value`;
}

function chooseOption(options: string[], variant: 'base' | 'variant' | 'zero'): string {
  const ordered = [...options];
  if (ordered.length === 0) {
    return variant === 'variant' ? 'alt' : 'value';
  }

  if (variant === 'base') {
    const preferred = ordered.find((option) => option !== 'other');
    return preferred ?? ordered[0];
  }

  if (variant === 'variant') {
    return ordered[1] ?? ordered[0];
  }

  if (ordered.includes('other')) {
    return 'other';
  }
  return ordered[ordered.length - 1];
}
