package com.box.l10n.mojito.iterators;

import java.util.List;
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
    PageFetcherOffsetAndLimit<T> pageFetcherOffsetAndLimit;

    public PageFetcherOffsetAndLimitSplitIterator(PageFetcherOffsetAndLimit<T> pageFetcherOffsetAndLimit, int limit) {
        super(Long.MAX_VALUE, 0);
        this.limit = limit;
        this.offset = -limit;
        this.pageFetcherOffsetAndLimit = pageFetcherOffsetAndLimit;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (needsFetching) {
            offset += limit;
            List<T> fetch = pageFetcherOffsetAndLimit.fetch(offset, limit);
            fetch.forEach(action::accept);
            needsFetching = fetch.size() == limit;
        }
        return needsFetching;
    }
}
