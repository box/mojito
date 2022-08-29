package com.box.l10n.mojito.iterators;

/**
 * To fetch data from a paginated source. To be used with {@link
 * PageFetcherOffsetAndLimitSplitIterator}.
 *
 * @param <T>
 */
@FunctionalInterface
public interface PageFetcherCurrentAndTotalPages<T> {
  ListWithLastPage<T> fetch(int currentPage);
}
