import type { TextareaHTMLAttributes } from 'react';
import { forwardRef, useCallback, useLayoutEffect, useRef } from 'react';

type Props = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  value: string;
  minRows?: number;
  maxRows?: number;
};

export const AutoTextarea = forwardRef<HTMLTextAreaElement, Props>(function AutoTextarea(
  { value, minRows, maxRows, style, ...rest },
  forwardedRef,
) {
  const innerRef = useRef<HTMLTextAreaElement | null>(null);

  const setRefs = useCallback(
    (node: HTMLTextAreaElement | null) => {
      innerRef.current = node;
      if (typeof forwardedRef === 'function') {
        forwardedRef(node);
      } else if (forwardedRef) {
        forwardedRef.current = node;
      }
    },
    [forwardedRef],
  );

  useLayoutEffect(() => {
    const element = innerRef.current;
    if (!element) {
      return;
    }

    element.style.height = 'auto';

    const computedStyles = window.getComputedStyle(element);
    let lineHeight = parseFloat(computedStyles.lineHeight || '0');
    if (!Number.isFinite(lineHeight) || lineHeight <= 0) {
      const fontSize = parseFloat(computedStyles.fontSize || '0');
      lineHeight = Number.isFinite(fontSize) && fontSize > 0 ? fontSize * 1.2 : 0;
    }
    const paddingTop = parseFloat(computedStyles.paddingTop || '0');
    const paddingBottom = parseFloat(computedStyles.paddingBottom || '0');
    const borderTop = parseFloat(computedStyles.borderTopWidth || '0');
    const borderBottom = parseFloat(computedStyles.borderBottomWidth || '0');
    const extra = paddingTop + paddingBottom + borderTop + borderBottom;

    let nextHeight = element.scrollHeight;

    if (lineHeight > 0 && typeof minRows === 'number' && minRows > 0) {
      const minHeight = minRows * lineHeight + extra;
      nextHeight = Math.max(nextHeight, minHeight);
    }

    if (lineHeight > 0 && typeof maxRows === 'number' && maxRows > 0) {
      const maxHeight = maxRows * lineHeight + extra;
      nextHeight = Math.min(nextHeight, maxHeight);
      element.style.overflowY = element.scrollHeight > maxHeight ? 'auto' : 'hidden';
    } else {
      element.style.overflowY = 'hidden';
    }

    element.style.height = `${nextHeight}px`;
  }, [value, minRows, maxRows]);

  return (
    <textarea
      {...rest}
      ref={setRefs}
      value={value}
      style={{
        resize: 'vertical',
        overflow: 'hidden',
        boxSizing: 'border-box',
        ...style,
      }}
    />
  );
});
