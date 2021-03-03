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
    final int fetchLimit;
    int offset;
    boolean needsFetching = true;
    PageFetcherOffsetAndLimit<T> pageFetcherOffsetAndLimit;

    public PageFetcherOffsetAndLimitSplitIterator(PageFetcherOffsetAndLimit<T> pageFetcherOffsetAndLimit, int limit) {
        this(pageFetcherOffsetAndLimit, limit, false);
    }

    public PageFetcherOffsetAndLimitSplitIterator(PageFetcherOffsetAndLimit<T> pageFetcherOffsetAndLimit, int limit, boolean fetchLimitPlusOne) {
        super(Long.MAX_VALUE, 0);
        this.limit = limit;
        this.fetchLimit = fetchLimitPlusOne ? limit + 1 : limit;
        this.offset = -limit;
        this.pageFetcherOffsetAndLimit = pageFetcherOffsetAndLimit;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (needsFetching) {
            offset += limit;
            List<T> fetched = pageFetcherOffsetAndLimit.fetch(offset, fetchLimit);
            fetched.subList(0, Math.min(fetched.size(), limit)).forEach(action::accept);
            needsFetching = fetched.size() == fetchLimit;
        }
        return needsFetching;
    }
}
