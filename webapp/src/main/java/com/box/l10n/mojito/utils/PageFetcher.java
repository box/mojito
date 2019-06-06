package com.box.l10n.mojito.utils;

import java.util.List;

/**
 * To fetch data from a paginated source. To be used with {@link PageFetcherSplitIterator}.
 *
 * @param <T>
 */
@FunctionalInterface
public interface PageFetcher<T> {
    List<T> fetch(int offset, int limit);
}
