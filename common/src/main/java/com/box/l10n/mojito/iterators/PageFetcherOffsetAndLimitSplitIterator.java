package com.box.l10n.mojito.iterators;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * Generic {@link java.util.Spliterator} to fetch data from a Paginated source.
 *
 * @param <T>
 */
public class PageFetcherOffsetAndLimitSplitIterator<T> extends Spliterators.AbstractSpliterator<T> {

  final int limit;
  int offset;
  boolean needsFetching = true;
  Queue<T> fetched;
  PageFetcherOffsetAndLimit<T> pageFetcherOffsetAndLimit;

  public PageFetcherOffsetAndLimitSplitIterator(
      PageFetcherOffsetAndLimit<T> pageFetcherOffsetAndLimit, int limit) {
    super(Long.MAX_VALUE, 0);
    this.limit = limit;
    this.offset = -limit;
    this.pageFetcherOffsetAndLimit = pageFetcherOffsetAndLimit;
    this.fetched = new ArrayDeque<>(limit);
  }

  @Override
  public boolean tryAdvance(Consumer<? super T> action) {
    if (!fetched.isEmpty()) {
      action.accept(fetched.remove());
    } else if (needsFetching) {
      offset += limit;
      fetched.addAll(pageFetcherOffsetAndLimit.fetch(offset, limit));
      needsFetching = fetched.size() == limit;
      if (!fetched.isEmpty()) {
        action.accept(fetched.remove());
      }
    }
    return needsFetching || !fetched.isEmpty();
  }
}
