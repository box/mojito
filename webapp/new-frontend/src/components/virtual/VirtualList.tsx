import './virtual-list.css';

import { type VirtualItem } from '@tanstack/react-virtual';
import { type CSSProperties, type ReactNode, type Ref, type RefObject } from 'react';

type VirtualRowRenderResult = {
  key?: React.Key;
  className?: string;
  style?: CSSProperties;
  props?: React.HTMLAttributes<HTMLDivElement> & { ref?: Ref<HTMLDivElement> };
  content: ReactNode;
};

type VirtualListProps = {
  scrollRef: RefObject<HTMLDivElement>;
  items: VirtualItem[];
  totalSize: number;
  renderRow: (virtualItem: VirtualItem) => VirtualRowRenderResult | null;
};

export function VirtualList({ scrollRef, items, totalSize, renderRow }: VirtualListProps) {
  return (
    <div className="virtual-scroll" ref={scrollRef}>
      <div
        className="virtual-scroll__inner"
        style={{
          height: totalSize,
          position: 'relative',
          width: '100%',
        }}
      >
        {items.map((virtualItem) => {
          const result = renderRow(virtualItem);
          if (!result) {
            return null;
          }

          const { key, className, content } = result;
          const style: CSSProperties = result.style ?? {};
          const { transform: userTransform, willChange, ...restStyle } = style;
          const props = result.props ?? {};
          const translateY = `translateY(${virtualItem.start}px)`;
          const combinedTransform = userTransform ? `${userTransform} ${translateY}` : translateY;
          return (
            <div
              key={key ?? virtualItem.key ?? virtualItem.index}
              data-index={virtualItem.index}
              className={className}
              style={{
                ...restStyle,
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                willChange: willChange ?? 'transform',
                transform: combinedTransform,
              }}
              {...props}
            >
              {content}
            </div>
          );
        })}
      </div>
    </div>
  );
}
