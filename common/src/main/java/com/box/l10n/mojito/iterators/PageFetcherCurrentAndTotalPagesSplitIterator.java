package com.box.l10n.mojito.iterators;

import com.google.common.base.Preconditions;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * Generic {@link java.util.Spliterator} to fetch data from a Paginated source. A starting page is
 * needed, will fetch until reaching the last page provided with {@link
 * ListWithLastPage#getLastPage()}.
 *
 * @param <T>
 */
public class PageFetcherCurrentAndTotalPagesSplitIterator<T>
    extends Spliterators.AbstractSpliterator<T> {
  int pageToFetch = 0;
  boolean needsFetching = true;
  PageFetcherCurrentAndTotalPages<T> pageFetcherCurrentAndTotalPages;

  public PageFetcherCurrentAndTotalPagesSplitIterator(
      PageFetcherCurrentAndTotalPages<T> pageFetcherCurrentAndTotalPages) {
    this(pageFetcherCurrentAndTotalPages, 0);
  }

  public PageFetcherCurrentAndTotalPagesSplitIterator(
      PageFetcherCurrentAndTotalPages<T> pageFetcherCurrentAndTotalPages, int startingPage) {
    super(Long.MAX_VALUE, 0);
    Preconditions.checkState(startingPage >= 0, "starting page must >= 0");
    this.pageToFetch = startingPage;
    this.pageFetcherCurrentAndTotalPages = pageFetcherCurrentAndTotalPages;
  }

  @Override
  public boolean tryAdvance(Consumer<? super T> action) {
    if (needsFetching) {
      ListWithLastPage<T> fetch = pageFetcherCurrentAndTotalPages.fetch(pageToFetch);
      fetch.getList().forEach(action::accept);
      Preconditions.checkState(
          fetch.getLastPage() >= pageToFetch, "lastPage must be >= pageToFetch");
      needsFetching = pageToFetch < fetch.getLastPage();
      pageToFetch++;
    }
    return needsFetching;
  }
}
