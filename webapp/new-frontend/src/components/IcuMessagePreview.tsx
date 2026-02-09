import './icu-message-preview.css';

import { useEffect, useMemo, useState } from 'react';

import type { IcuParameterDescriptor, IcuPluralCategory } from '../utils/icuMessageFormat';
import {
  buildIcuExampleValueSets,
  getPluralCategories,
  getPluralSampleValue,
  mergeValues,
  parseIcuMessage,
  renderIcuWithValues,
} from '../utils/icuMessageFormat';

export type IcuAiSuggestionRequest = {
  message: string;
  locale: string;
  parameters: IcuParameterDescriptor[];
  values: Record<string, string>;
};

export type IcuMessagePreviewProps = {
  message?: string;
  locale?: string;
  onMessageChange?: (value: string) => void;
  onLocaleChange?: (value: string) => void;
  initialMessage?: string;
  initialLocale?: string;
  showMessageEditor?: boolean;
  showLocaleInput?: boolean;
  showExamples?: boolean;
  requestAiValues?: (request: IcuAiSuggestionRequest) => Promise<Record<string, string>>;
};

type ParseState = {
  ast: ReturnType<typeof parseIcuMessage>['ast'] | null;
  parameters: IcuParameterDescriptor[];
  error: string | null;
};

const DEFAULT_MESSAGE =
  '{name} has {count, plural, =0 {no tasks} one {# task} other {# tasks}} due by {dueDate, date, medium}.';

export function IcuMessagePreview({
  message: controlledMessage,
  locale: controlledLocale,
  onMessageChange,
  onLocaleChange,
  initialMessage = DEFAULT_MESSAGE,
  initialLocale = 'en',
  showMessageEditor = true,
  showLocaleInput = true,
  showExamples = true,
  requestAiValues,
}: IcuMessagePreviewProps) {
  const [internalMessage, setInternalMessage] = useState(initialMessage);
  const [internalLocale, setInternalLocale] = useState(initialLocale);
  const [selectedPresetId, setSelectedPresetId] = useState<string | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [aiError, setAiError] = useState<string | null>(null);
  const [isFetchingAiValues, setIsFetchingAiValues] = useState(false);
  const message = controlledMessage ?? internalMessage;
  const normalizedMessage = typeof message === 'string' ? message : '';
  const locale = controlledLocale ?? internalLocale;

  const setMessage = (nextMessage: string) => {
    if (onMessageChange) {
      onMessageChange(nextMessage);
      return;
    }
    setInternalMessage(nextMessage);
  };

  const setLocale = (nextLocale: string) => {
    if (onLocaleChange) {
      onLocaleChange(nextLocale);
      return;
    }
    setInternalLocale(nextLocale);
  };

  const parseState = useMemo<ParseState>(() => {
    if (!normalizedMessage.trim()) {
      return {
        ast: [],
        parameters: [],
        error: null,
      };
    }
    try {
      const parsed = parseIcuMessage(normalizedMessage);
      return {
        ast: parsed.ast,
        parameters: parsed.parameters,
        error: null,
      };
    } catch (error: unknown) {
      return {
        ast: null,
        parameters: [],
        error: error instanceof Error ? error.message : 'Unable to parse ICU message.',
      };
    }
  }, [normalizedMessage]);

  const presets = useMemo(
    () => buildIcuExampleValueSets(parseState.parameters, locale),
    [locale, parseState.parameters],
  );

  const singlePluralParameter = useMemo(() => {
    if (parseState.error || parseState.parameters.length !== 1) {
      return null;
    }
    const [parameter] = parseState.parameters;
    return parameter.kinds.includes('plural') ? parameter : null;
  }, [parseState.error, parseState.parameters]);

  useEffect(() => {
    if (parseState.error) {
      setSelectedPresetId(null);
      setValues({});
      return;
    }

    const pluralOptionPreset = presets.find((preset) => preset.id.startsWith('option-'));
    const firstPreset = singlePluralParameter ? (pluralOptionPreset ?? presets[0]) : presets[0];
    const nextSelectedPresetId =
      selectedPresetId && presets.some((preset) => preset.id === selectedPresetId)
        ? selectedPresetId
        : (firstPreset?.id ?? null);

    const activePreset = presets.find((preset) => preset.id === nextSelectedPresetId) ??
      firstPreset ?? {
        id: 'empty',
        label: 'Empty',
        values: {},
      };

    setSelectedPresetId(nextSelectedPresetId);
    setValues((previous) => {
      if (parseState.parameters.length === 0) {
        return {};
      }
      const merged = mergeValues(activePreset.values, previous, parseState.parameters);
      parseState.parameters.forEach((parameter) => {
        if (!(parameter.name in merged)) {
          merged[parameter.name] = activePreset.values[parameter.name] ?? '';
        }
      });
      return merged;
    });
  }, [parseState.error, parseState.parameters, presets, selectedPresetId, singlePluralParameter]);

  const selectedOutput = useMemo(() => {
    if (parseState.error || !parseState.ast) {
      return {
        value: '',
        error: parseState.error ?? 'Unable to parse message.',
      };
    }
    try {
      return {
        value: renderIcuWithValues(
          parseState.ast,
          parseState.parameters,
          values,
          locale,
          normalizedMessage,
        ),
        error: null,
      };
    } catch (error: unknown) {
      return {
        value: '',
        error: error instanceof Error ? error.message : 'Failed to render message.',
      };
    }
  }, [locale, normalizedMessage, parseState.ast, parseState.error, parseState.parameters, values]);

  const presetOutputs = useMemo(() => {
    const ast = parseState.ast;
    if (parseState.error || !ast) {
      return [];
    }

    return presets.map((preset) => {
      try {
        return {
          ...preset,
          output: renderIcuWithValues(
            ast,
            parseState.parameters,
            preset.values,
            locale,
            normalizedMessage,
          ),
          error: null,
        };
      } catch (error: unknown) {
        return {
          ...preset,
          output: '',
          error: error instanceof Error ? error.message : 'Failed to render message.',
        };
      }
    });
  }, [locale, normalizedMessage, parseState.ast, parseState.error, parseState.parameters, presets]);

  const visiblePresetOutputs = useMemo(() => {
    if (!singlePluralParameter) {
      return presetOutputs;
    }
    return presetOutputs.filter((preset) => preset.id.startsWith('option-'));
  }, [presetOutputs, singlePluralParameter]);

  const handleSelectPreset = (presetId: string) => {
    const preset = presets.find((item) => item.id === presetId);
    if (!preset) {
      return;
    }
    setSelectedPresetId(presetId);
    setValues({ ...preset.values });
    setAiError(null);
  };

  const handleChangeValue = (name: string, nextValue: string) => {
    setValues((previous) => ({ ...previous, [name]: nextValue }));
    setAiError(null);
  };

  const handleAskAi = async () => {
    if (!requestAiValues || parseState.parameters.length === 0 || parseState.error) {
      return;
    }

    setIsFetchingAiValues(true);
    setAiError(null);
    try {
      const suggestedValues = await requestAiValues({
        message: normalizedMessage,
        locale,
        parameters: parseState.parameters,
        values,
      });
      setValues((previous) => mergeValues(previous, suggestedValues, parseState.parameters));
    } catch (error: unknown) {
      setAiError(error instanceof Error ? error.message : 'AI value suggestion failed.');
    } finally {
      setIsFetchingAiValues(false);
    }
  };

  const showIntroSection =
    showMessageEditor ||
    showLocaleInput ||
    Boolean(parseState.error) ||
    parseState.parameters.length === 0;

  return (
    <div className="icu-preview">
      {showIntroSection ? (
        <section className="icu-preview__section">
          {showMessageEditor ? (
            <>
              <div className="icu-preview__label-row">
                <label className="form-label" htmlFor="icu-message-input">
                  ICU message
                </label>
              </div>
              <textarea
                id="icu-message-input"
                className="input icu-preview__textarea"
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                rows={5}
                spellCheck={false}
                placeholder="e.g. {name} has {count, plural, one {# message} other {# messages}}."
              />
            </>
          ) : null}
          {showLocaleInput ? (
            <div className="icu-preview__locale-row">
              <label className="form-label" htmlFor="icu-locale-input">
                Locale
              </label>
              <input
                id="icu-locale-input"
                className="input icu-preview__locale-input"
                type="text"
                value={locale}
                onChange={(event) => setLocale(event.target.value)}
                placeholder="en"
              />
            </div>
          ) : null}
          {parseState.error ? (
            <div className="alert alert--error">Parse error: {parseState.error}</div>
          ) : parseState.parameters.length === 0 ? (
            <div className="alert alert--muted">No parameters detected.</div>
          ) : null}
        </section>
      ) : null}

      <section className="icu-preview__section">
        {selectedOutput.error ? (
          <div className="alert alert--error">{selectedOutput.error}</div>
        ) : (
          <pre className="icu-preview__output">{selectedOutput.value}</pre>
        )}
      </section>

      {parseState.parameters.length > 0 && !parseState.error ? (
        <section className="icu-preview__section icu-preview__section--parameters">
          <h2 className="icu-preview__heading">Parameters</h2>
          <div className="icu-preview__parameter-table">
            {parseState.parameters.map((parameter) => (
              <div key={parameter.name} className="icu-preview__parameter-row">
                <div className="icu-preview__parameter-meta">
                  <span className="icu-preview__parameter-name-row">
                    <code>{parameter.name}</code>
                    <span className="icu-preview__parameter-kinds">
                      {formatKinds(parameter.kinds)}
                    </span>
                  </span>
                  {parameter.selectOptions.length > 0 ? (
                    <span className="icu-preview__parameter-options">
                      {parameter.selectOptions.join(', ')}
                    </span>
                  ) : null}
                </div>
                <div className="icu-preview__parameter-input">
                  <ParameterInput
                    parameter={parameter}
                    value={values[parameter.name] ?? ''}
                    locale={locale}
                    onChange={(nextValue) => handleChangeValue(parameter.name, nextValue)}
                  />
                </div>
              </div>
            ))}
          </div>
        </section>
      ) : null}

      {showExamples && visiblePresetOutputs.length > 0 && !parseState.error ? (
        <section className="icu-preview__section">
          <div className="icu-preview__preset-header">
            <h2 className="icu-preview__heading">Examples</h2>
            {requestAiValues ? (
              <button
                type="button"
                className="btn"
                onClick={() => {
                  void handleAskAi();
                }}
                disabled={isFetchingAiValues || parseState.parameters.length === 0}
              >
                {isFetchingAiValues ? 'Asking AI…' : 'Ask AI for values'}
              </button>
            ) : null}
          </div>
          {aiError ? <div className="alert alert--error">{aiError}</div> : null}
          <div className="icu-preview__preset-list">
            {visiblePresetOutputs.map((preset) => (
              <label key={preset.id} className="icu-preview__preset-item">
                <input
                  type="radio"
                  name="icu-preset"
                  checked={selectedPresetId === preset.id}
                  onChange={() => handleSelectPreset(preset.id)}
                />
                <span className="icu-preview__preset-content">
                  <span className="icu-preview__preset-title">{preset.label}</span>
                  <code className="icu-preview__preset-values">
                    {JSON.stringify(preset.values)}
                  </code>
                  {preset.error ? (
                    <span className="icu-preview__preset-error">{preset.error}</span>
                  ) : (
                    <span className="icu-preview__preset-output">{preset.output}</span>
                  )}
                </span>
              </label>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}

function ParameterInput({
  parameter,
  value,
  locale,
  onChange,
}: {
  parameter: IcuParameterDescriptor;
  value: string;
  locale: string;
  onChange: (value: string) => void;
}) {
  if (parameter.selectOptions.length > 0) {
    return (
      <select className="input" value={value} onChange={(event) => onChange(event.target.value)}>
        {parameter.selectOptions.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    );
  }

  if (parameter.kinds.includes('date') || parameter.kinds.includes('time')) {
    return (
      <input
        className="input"
        type="datetime-local"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    );
  }

  if (parameter.kinds.includes('number') || parameter.kinds.includes('plural')) {
    const pluralQuickOptions = parameter.kinds.includes('plural')
      ? buildPluralQuickOptions(parameter.pluralOptions, locale)
      : [];

    return (
      <div className="icu-preview__parameter-input-group">
        <input
          className="input icu-preview__number-input"
          type="number"
          value={value}
          onChange={(event) => onChange(event.target.value)}
        />
        {parameter.kinds.includes('plural') && pluralQuickOptions.length > 0 ? (
          <div
            className="icu-preview__plural-pills"
            role="group"
            aria-label={`${parameter.name} plural forms`}
          >
            {pluralQuickOptions.map((option) => {
              const sampleValue = getPluralSampleValue(option, locale);
              const isActive = value === sampleValue;
              return (
                <button
                  key={`${option}-${sampleValue}`}
                  type="button"
                  className={`icu-preview__plural-pill ${isActive ? 'is-active' : ''}`}
                  onClick={() => onChange(sampleValue)}
                >
                  {option}
                </button>
              );
            })}
          </div>
        ) : null}
      </div>
    );
  }

  return (
    <input
      className="input"
      type="text"
      value={value}
      onChange={(event) => onChange(event.target.value)}
    />
  );
}

function buildPluralQuickOptions(messageOptions: string[], locale: string): string[] {
  const orderedCategories: IcuPluralCategory[] = ['zero', 'one', 'two', 'few', 'many', 'other'];
  const localeCategories = new Set(getPluralCategories(locale));
  const categoryOptions = orderedCategories.filter(
    (category) => localeCategories.has(category) || messageOptions.includes(category),
  );
  const defaultOptions = ['=0', '=1', ...categoryOptions];
  const options = [...defaultOptions];

  messageOptions.forEach((option) => {
    if (!options.includes(option)) {
      options.push(option);
    }
  });

  return options;
}

function formatKinds(kinds: IcuParameterDescriptor['kinds']): string {
  return kinds.join(' · ');
}
