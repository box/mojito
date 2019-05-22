package com.box.l10n.mojito.utils;

import java.util.List;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * Generic {@link java.util.Spliterator} to fetch data from a Paginated source.
 *
 * @param <T>
 */
public class PageFetcherSplitIterator<T> extends Spliterators.AbstractSpliterator<T> {

    final int limit;
    int offset;
    boolean needsFetching = true;
    PageFetcher<T> pageFetcher;

    PageFetcherSplitIterator(PageFetcher<T> pageFetcher, int limit) {
        super(Long.MAX_VALUE, 0);
        this.limit = limit;
        this.offset = -limit;
        this.pageFetcher = pageFetcher;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (needsFetching) {
            offset += limit;
            List<T> fetch = pageFetcher.fetch(offset, limit);
            fetch.forEach(action::accept);
            needsFetching = fetch.size() == limit;
        }
        return needsFetching;
    }
}
