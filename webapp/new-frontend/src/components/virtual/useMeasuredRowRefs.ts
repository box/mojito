import { useCallback, useRef } from 'react';

type UseMeasuredRowRefsOptions<K, T extends HTMLElement> = {
  measureElement: (element: T) => void;
  onAssign?: (key: K, element: T | null) => void;
};

type UseMeasuredRowRefsResult<K, T extends HTMLElement> = {
  getRowRef: (key: K) => (element: T | null) => void;
};

export function useMeasuredRowRefs<K, T extends HTMLElement = HTMLDivElement>({
  measureElement,
  onAssign,
}: UseMeasuredRowRefsOptions<K, T>): UseMeasuredRowRefsResult<K, T> {
  const rowRefCallbacks = useRef(new Map<K, (element: T | null) => void>());

  const getRowRef = useCallback(
    (key: K) => {
      const existing = rowRefCallbacks.current.get(key);
      if (existing) {
        return existing;
      }

      const callback = (element: T | null) => {
        if (onAssign) {
          onAssign(key, element);
        }
        if (element) {
          measureElement(element);
        }
      };

      rowRefCallbacks.current.set(key, callback);
      return callback;
    },
    [measureElement, onAssign],
  );

  return { getRowRef };
}
