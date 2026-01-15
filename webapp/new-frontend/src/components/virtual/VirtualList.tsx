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
  outerClassName?: string;
  innerClassName?: string;
  innerStyle?: CSSProperties;
};

export function VirtualList({
  scrollRef,
  items,
  totalSize,
  renderRow,
  outerClassName = '',
  innerClassName = '',
  innerStyle,
}: VirtualListProps) {
  return (
    <div className={outerClassName} ref={scrollRef}>
      <div
        className={innerClassName}
        style={{
          height: totalSize,
          position: 'relative',
          width: '100%',
          ...innerStyle,
        }}
      >
        {items.map((virtualItem) => {
          const result = renderRow(virtualItem);
          if (!result) {
            return null;
          }

          const { key, className, content } = result;
          const style = result.style ?? {};
          const props = result.props ?? {};
          return (
            <div
              key={key ?? virtualItem.key ?? virtualItem.index}
              className={className}
              style={{
                transform: `translateY(${virtualItem.start}px)`,
                ...style,
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
