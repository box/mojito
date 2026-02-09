import { describe, expect, it } from 'vitest';

import {
  buildIcuExampleValueSets,
  getPluralCategories,
  getPluralCategorySampleMap,
  getPluralSampleValue,
  mergeValues,
  parseIcuMessage,
  renderIcuWithValues,
} from './icuMessageFormat';

describe('parseIcuMessage', () => {
  it('infers parameters and option values from plural/select blocks', () => {
    const message =
      'Hello {name}, you have {count, plural, one {# item} other {# items}} in {gender, select, male {his} female {her} other {their}} cart.';

    const parsed = parseIcuMessage(message);

    expect(parsed.parameters.map((parameter) => parameter.name)).toEqual([
      'name',
      'count',
      'gender',
    ]);

    const count = parsed.parameters.find((parameter) => parameter.name === 'count');
    expect(count?.kinds).toContain('plural');
    expect(count?.pluralOptions).toEqual(['one', 'other']);

    const gender = parsed.parameters.find((parameter) => parameter.name === 'gender');
    expect(gender?.kinds).toContain('select');
    expect(gender?.selectOptions).toEqual(['male', 'female', 'other']);
  });

  it('throws for invalid ICU messages', () => {
    expect(() => parseIcuMessage('Hello {name')).toThrow();
  });
});

describe('buildIcuExampleValueSets', () => {
  it('creates multiple useful presets', () => {
    const parsed = parseIcuMessage(
      '{name} invited {guestCount, plural, one {# guest} other {# guests}} to {city}.',
    );
    const presets = buildIcuExampleValueSets(parsed.parameters, 'en');

    expect(presets.length).toBeGreaterThanOrEqual(3);
    expect(presets[0]?.values.name).toBeTypeOf('string');
    expect(presets.some((preset) => preset.values.guestCount === '0')).toBe(true);
  });

  it('uses locale-aware numeric samples for plural options', () => {
    const frParsed = parseIcuMessage('{count, plural, one {one} other {other}}');
    const frPresets = buildIcuExampleValueSets(frParsed.parameters, 'fr');
    const frOne = frPresets.find((preset) => preset.label.startsWith('count: one ->'));
    expect(frOne).toBeDefined();
    expect(new Intl.PluralRules('fr').select(Number(frOne?.values.count ?? 'NaN'))).toBe('one');

    const ruParsed = parseIcuMessage(
      '{count, plural, one {one} few {few} many {many} other {other}}',
    );
    const ruPresets = buildIcuExampleValueSets(ruParsed.parameters, 'ru');
    const ruFew = ruPresets.find((preset) => preset.label.startsWith('count: few ->'));
    expect(ruFew).toBeDefined();
    expect(new Intl.PluralRules('ru').select(Number(ruFew?.values.count ?? 'NaN'))).toBe('few');
  });

  it('creates one option preset per plural form present in the message', () => {
    const parsed = parseIcuMessage(
      '{count, plural, one {one} few {few} many {many} other {other}}',
    );
    const presets = buildIcuExampleValueSets(parsed.parameters, 'ru');
    const optionLabels = presets
      .filter((preset) => preset.id.startsWith('option-'))
      .map((preset) => preset.label);

    expect(optionLabels).toEqual([
      'count: one -> 1',
      'count: few -> 2',
      'count: many -> 0',
      'count: other -> 1.1',
    ]);
  });
});

describe('plural rules helpers', () => {
  it('reads plural categories from Intl.PluralRules', () => {
    expect(getPluralCategories('fr')).toEqual(['one', 'other']);
    expect(new Set(getPluralCategories('ru'))).toEqual(new Set(['one', 'few', 'many', 'other']));
  });

  it('returns a locale sample number for every supported category', () => {
    const ruSamples = getPluralCategorySampleMap('ru');
    expect(ruSamples.one).not.toBeNull();
    expect(ruSamples.few).not.toBeNull();
    expect(ruSamples.many).not.toBeNull();
    expect(ruSamples.other).not.toBeNull();
  });

  it('maps a plural option to a sample number for the locale', () => {
    const frOne = Number(getPluralSampleValue('one', 'fr'));
    expect(new Intl.PluralRules('fr').select(frOne)).toBe('one');

    const ruMany = Number(getPluralSampleValue('many', 'ru'));
    expect(new Intl.PluralRules('ru').select(ruMany)).toBe('many');
  });
});

describe('renderIcuWithValues', () => {
  it('formats with coerced values', () => {
    const parsed = parseIcuMessage('Hello {name}, count={count, number}.');
    const rendered = renderIcuWithValues(
      parsed.ast,
      parsed.parameters,
      {
        name: 'Alex',
        count: '3',
      },
      'en',
    );

    expect(rendered).toContain('Hello Alex');
    expect(rendered).toContain('count=3');
  });
});

describe('mergeValues', () => {
  it('only merges keys that exist in inferred parameters', () => {
    const parsed = parseIcuMessage('Hello {name}, count={count, number}.');
    const merged = mergeValues(
      { name: 'Alex', count: '1' },
      { name: 'Sam', unused: 'ignored', count: '2' },
      parsed.parameters,
    );

    expect(merged).toEqual({ name: 'Sam', count: '2' });
  });
});
