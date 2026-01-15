import { useVirtualizer, type VirtualItem, type Virtualizer } from '@tanstack/react-virtual';
import { type Key, type RefObject, useCallback, useRef } from 'react';

type UseVirtualRowsOptions<TScrollElement extends HTMLElement> = {
  count: number;
  estimateSize: () => number;
  overscan?: number;
  getItemKey?: (index: number) => Key;
  getScrollElement?: () => TScrollElement | null;
};

type UseVirtualRowsResult<TScrollElement extends HTMLElement> = {
  scrollRef: RefObject<TScrollElement>;
  virtualizer: Virtualizer<TScrollElement, TScrollElement>;
  items: VirtualItem[];
  totalSize: number;
  scrollToIndex: (
    index: number,
    align?: Parameters<Virtualizer<TScrollElement, TScrollElement>['scrollToIndex']>[1],
  ) => void;
  measureElement: (element: TScrollElement | null) => void;
};

export function useVirtualRows<TScrollElement extends HTMLElement = HTMLDivElement>({
  count,
  estimateSize,
  overscan = 8,
  getItemKey,
  getScrollElement,
}: UseVirtualRowsOptions<TScrollElement>): UseVirtualRowsResult<TScrollElement> {
  const scrollRef = useRef<TScrollElement>(null!);

  const virtualizer = useVirtualizer<TScrollElement, TScrollElement>({
    count,
    getScrollElement: getScrollElement ?? (() => scrollRef.current),
    estimateSize,
    overscan,
    getItemKey,
  });

  const virtualizerRef = useRef(virtualizer);
  virtualizerRef.current = virtualizer;

  const scrollToIndex = useCallback(
    (
      index: number,
      align?: Parameters<Virtualizer<TScrollElement, TScrollElement>['scrollToIndex']>[1],
    ) => {
      virtualizerRef.current.scrollToIndex(index, align);
    },
    [],
  );

  const measureElement = useCallback((element: TScrollElement | null) => {
    if (!element) {
      return;
    }
    virtualizerRef.current.measureElement(element);
  }, []);

  return {
    scrollRef,
    virtualizer,
    items: virtualizer.getVirtualItems(),
    totalSize: virtualizer.getTotalSize(),
    scrollToIndex,
    measureElement,
  };
}
