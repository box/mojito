package com.box.l10n.mojito.iterators;

import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PageFetcherOffsetAndLimitSplitIteratorTest {

    @Test
    public void testSplitIterator() {
        PageFetcherOffsetAndLimitSplitIterator<Integer> integerPageFetcherSplitIterator = new PageFetcherOffsetAndLimitSplitIterator<>((offset, limit) -> {
            // return fake paginated result
            return IntStream.range(offset, offset < 50 ? offset + limit : offset + 10).boxed().collect(Collectors.toList());
        }, 20);

        Stream<Integer> stream = StreamSupport.stream(integerPageFetcherSplitIterator, false);

        assertEquals(
                IntStream.range(0, 70).boxed().collect(Collectors.toList()),
                stream.collect(Collectors.toList())
        );
    }

    @Test
    public void testSplitIteratorEmpty() {
        PageFetcherOffsetAndLimitSplitIterator<Integer> integerPageFetcherSplitIterator = new PageFetcherOffsetAndLimitSplitIterator<>((offset, limit) -> {
            return new ArrayList<>();
        }, 20);

        Stream<Integer> stream = StreamSupport.stream(integerPageFetcherSplitIterator, false);
        assertTrue(stream.collect(Collectors.toList()).isEmpty());
    }

}
