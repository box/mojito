import './char-code-helper.css';

import { useCallback, useMemo, useState } from 'react';

function parseCodePoint(raw: string): number | null {
  if (!raw) return null;
  const trimmed = raw.trim();
  if (!trimmed) return null;
  const isHex = /^((0x|0X|U\+|u\+).+)/.test(trimmed);
  const digits = isHex ? trimmed.replace(/^(0x|0X|U\+|u\+)/, '') : trimmed;
  const base = isHex ? 16 : 10;
  if (!digits || !/^[0-9a-fA-F]+$/.test(digits)) return null;
  const code = parseInt(digits, base);
  if (!Number.isInteger(code) || code < 0 || code > 0x10ffff) return null;
  return code;
}

function describeCodePoint(codePoint: number): {
  isControl: boolean;
  isForbiddenControl: boolean;
  label: string;
} {
  const isAsciiControl =
    (codePoint >= 0 && codePoint <= 0x1f) || (codePoint >= 0x7f && codePoint <= 0x9f);
  if (isAsciiControl) {
    if (codePoint === 0x09)
      return { isControl: true, isForbiddenControl: false, label: 'Control (TAB)' };
    if (codePoint === 0x0a)
      return { isControl: true, isForbiddenControl: false, label: 'Control (LF)' };
    if (codePoint === 0x0d)
      return { isControl: true, isForbiddenControl: false, label: 'Control (CR)' };
    return { isControl: true, isForbiddenControl: true, label: 'Control (non-printing)' };
  }
  return { isControl: false, isForbiddenControl: false, label: 'Printable' };
}

type DisplayNamesCtor = new (
  locales: string | string[],
  options: { type: 'character' },
) => { of: (code: number) => string | undefined };

function codePointToName(codePoint: number): string | null {
  if (typeof Intl === 'undefined' || !('DisplayNames' in Intl)) {
    return null;
  }
  try {
    const DisplayNames = (Intl as typeof Intl & { DisplayNames: DisplayNamesCtor }).DisplayNames;
    const dn = new DisplayNames(['en'], { type: 'character' });
    return dn.of(codePoint) ?? null;
  } catch {
    return null;
  }
}

export function CharCodeHelperPage() {
  const [input, setInput] = useState('');
  const [codePoint, setCodePoint] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [charInput, setCharInput] = useState('');
  const [controlInput, setControlInput] = useState('');

  const derived = useMemo(() => {
    if (codePoint === null) {
      return null;
    }
    const hex = codePoint.toString(16).toUpperCase();
    const hexPadded = hex.padStart(Math.max(4, hex.length), '0');
    const regexRaw = `\\x{${hexPadded}}`;
    const regexSqlLiteral = `\\\\x{${hexPadded}}`;
    return {
      char: String.fromCodePoint(codePoint),
      dec: String(codePoint),
      hex: `0x${hex}`,
      unicode: `U+${hexPadded}`,
      regex: regexRaw,
      regexSql: regexSqlLiteral,
      name: codePointToName(codePoint),
      descriptor: describeCodePoint(codePoint),
    };
  }, [codePoint]);

  const applyCodePoint = useCallback((cp: number) => {
    setCodePoint(cp);
    setError(null);
    setCopied(false);
  }, []);

  const onCopy = useCallback(async () => {
    const parsedFromInput = parseCodePoint(input);
    const cp =
      parsedFromInput !== null && !Number.isNaN(parsedFromInput)
        ? parsedFromInput
        : derived
          ? parseInt(derived.dec, 10)
          : null;

    if (cp === null || Number.isNaN(cp)) {
      setError('Enter decimal, 0x, or U+ code point.');
      setCodePoint(null);
      setCopied(false);
      return;
    }
    applyCodePoint(cp);
    try {
      await navigator.clipboard.writeText(String.fromCodePoint(cp));
      setError(null);
      setCopied(true);
      setTimeout(() => setCopied(false), 900);
    } catch {
      setError('Copy failed. Try copying manually.');
    }
  }, [applyCodePoint, derived, input]);

  const charBreakdown = useMemo(() => {
    if (!charInput) return [];
    const chars = Array.from(charInput);
    return chars.map((ch) => {
      const cp = ch.codePointAt(0)!;
      const hex = cp.toString(16).toUpperCase();
      const hexPadded = hex.padStart(Math.max(4, hex.length), '0');
      const regexRaw = `\\x{${hexPadded}}`;
      const regexSqlLiteral = `\\\\x{${hexPadded}}`;
      return {
        char: ch,
        dec: String(cp),
        hex: `0x${hex}`,
        unicode: `U+${hexPadded}`,
        regex: regexRaw,
        regexSql: regexSqlLiteral,
        name: codePointToName(cp),
        descriptor: describeCodePoint(cp),
      };
    });
  }, [charInput]);

  const controlMarkedText = useMemo(() => {
    if (!controlInput) return [];
    return Array.from(controlInput).map((ch) => {
      const cp = ch.codePointAt(0)!;
      const descriptor = describeCodePoint(cp);
      return {
        char: ch,
        codePoint: cp,
        descriptor,
      };
    });
  }, [controlInput]);

  const onCopyCode = useCallback(async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 900);
    } catch {
      setError('Copy failed. Try copying manually.');
    }
  }, []);

  return (
    <div className="page-wrapper char-helper">
      <div className="card card--padded char-helper__card">
        <div className="card__header">
          <div>
            <h1 className="page-title">Code → Character</h1>
            <p className="hint">
              Enter a code point for the same character: decimal (129302), hex (0x1F916), or Unicode
              (U+1F916).
            </p>
          </div>
        </div>
        <div className="card__content card__content--stack">
          <label className="form-label" htmlFor="code-point-input">
            Code point
          </label>
          <input
            id="code-point-input"
            className="input"
            type="text"
            placeholder="U+1F916 or 0x1F916 or 129302"
            value={input}
            onChange={(e) => {
              setInput(e.target.value);
              setCopied(false);
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                void onCopy();
              }
            }}
          />
          <div className="char-helper__action-row">
            <button
              className="btn btn--primary"
              type="button"
              onClick={() => {
                void onCopy();
              }}
            >
              Copy character
            </button>
            {copied ? <span className="char-helper__copy-status">Copied!</span> : null}
          </div>
          {error ? <div className="alert alert--error">{error}</div> : null}
          {derived ? (
            <div className="char-card">
              <div className="char-card__glyph">{derived.char}</div>
              <div className="char-card__meta">
                {[
                  { label: 'Decimal', value: derived.dec },
                  { label: 'Hex', value: derived.hex },
                  { label: 'Unicode', value: derived.unicode },
                  { label: 'Regex', value: derived.regex },
                  { label: 'Regex (SQL literal)', value: derived.regexSql },
                ].map((item) => (
                  <div key={item.label}>
                    <span className="meta-label">{item.label}</span> <code>{item.value}</code>{' '}
                    <button
                      className="btn btn--ghost"
                      type="button"
                      onClick={() => {
                        void onCopyCode(item.value);
                      }}
                    >
                      Copy
                    </button>
                  </div>
                ))}
                {derived.name ? (
                  <div className="meta-name">
                    <span className="meta-label">Name</span> {derived.name}
                  </div>
                ) : null}
                <div className="meta-name">
                  <span className="meta-label">Type</span> {derived.descriptor.label}
                </div>
              </div>
            </div>
          ) : (
            <div className="alert alert--muted">
              Enter a code point to see the character and copy it.
            </div>
          )}

          <div className="char-helper__divider" />

          <h2 className="page-title">Character → Code</h2>
          <p className="hint">Paste or type character(s) to see their code points, e.g., 🤖.</p>
          <label className="form-label" htmlFor="char-input">
            Character(s)
          </label>
          <input
            id="char-input"
            className="input"
            type="text"
            placeholder="Paste a character, e.g., 🤖"
            value={charInput}
            onChange={(e) => {
              setCharInput(e.target.value);
              setCopied(false);
            }}
          />
          {charBreakdown.length === 0 ? (
            <div className="alert alert--muted">Nothing to show yet. Paste a character.</div>
          ) : (
            <div className="char-breakdown">
              {charBreakdown.map((item, index) => (
                <div key={`${item.unicode}-${index}`} className="char-breakdown__row">
                  <div className="char-breakdown__glyph">{item.char}</div>
                  <div className="char-breakdown__meta">
                    {[
                      { label: 'Unicode', value: item.unicode },
                      { label: 'Decimal', value: item.dec },
                      { label: 'Hex', value: item.hex },
                      { label: 'Regex', value: item.regex },
                      { label: 'Regex (SQL literal)', value: item.regexSql },
                    ].map((meta) => (
                      <div key={meta.label}>
                        <span className="meta-label">{meta.label}</span> <code>{meta.value}</code>{' '}
                        <button
                          className="btn btn--ghost"
                          type="button"
                          onClick={() => {
                            void onCopyCode(meta.value);
                          }}
                        >
                          Copy
                        </button>
                      </div>
                    ))}
                    {item.name ? (
                      <div className="meta-name">
                        <span className="meta-label">Name</span> {item.name}
                      </div>
                    ) : null}
                    <div className="meta-name">
                      <span className="meta-label">Type</span> {item.descriptor.label}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="char-helper__divider" />

          <h2 className="page-title">Control markers</h2>
          <p className="hint">
            ASCII control characters (except TAB / LF / CR) are shown as markers so you can see them
            in context.
          </p>
          <label className="form-label" htmlFor="control-input">
            Control marker input
          </label>
          <input
            id="control-input"
            className="input"
            type="text"
            placeholder="Paste text; control chars will be marked"
            value={controlInput}
            onChange={(e) => {
              setControlInput(e.target.value);
              setCopied(false);
            }}
          />
          {controlMarkedText.length === 0 ? (
            <div className="alert alert--muted">
              Nothing to show yet. Paste a character or text.
            </div>
          ) : (
            <div className="control-text">
              {controlMarkedText.map((segment, idx) =>
                segment.descriptor.isForbiddenControl ? (
                  <span key={idx} className="control-chip">
                    CTRL {`U+${segment.codePoint.toString(16).toUpperCase().padStart(4, '0')}`}
                  </span>
                ) : (
                  <span key={idx} className="control-text__char">
                    {segment.char}
                  </span>
                ),
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
import './char-code-helper.css';
