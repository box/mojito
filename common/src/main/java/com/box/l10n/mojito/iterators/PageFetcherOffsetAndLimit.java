package com.box.l10n.mojito.iterators;

import java.util.List;

/**
 * To fetch data from a paginated source. To be used with {@link
 * PageFetcherOffsetAndLimitSplitIterator}.
 *
 * @param <T>
 */
@FunctionalInterface
public interface PageFetcherOffsetAndLimit<T> {
  List<T> fetch(int offset, int limit);
}
