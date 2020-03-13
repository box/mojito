package com.box.l10n.mojito.iterators;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PageFetcherCurrentAndTotalPagesSplitIteratorTest {

    @Test
    public void testSplitIterator() {
        PageFetcherCurrentAndTotalPagesSplitIterator<Integer> integerPageFetcherSplitIterator = new PageFetcherCurrentAndTotalPagesSplitIterator<>(pageToFetch -> {
            // return fake paginated result

            int startIdx = pageToFetch * 10;
            int endIdx = pageToFetch == 5 ? (pageToFetch + 1) * 10 - 5 : (pageToFetch + 1) * 10;

            List<Integer> collect = IntStream.range(startIdx, endIdx).boxed().collect(Collectors.toList());
            ListWithLastPage<Integer> list = new ListWithLastPage<>();
            list.setList(collect);
            list.setLastPage(5);
            return list;
        }, 0);

        Stream<Integer> stream = StreamSupport.stream(integerPageFetcherSplitIterator, false);

        assertEquals(
                IntStream.range(0, 55).boxed().collect(Collectors.toList()),
                stream.collect(Collectors.toList())
        );
    }

    @Test
    public void testSplitIteratorEmpty() {
        PageFetcherCurrentAndTotalPagesSplitIterator<Integer> integerPageFetcherSplitIterator = new PageFetcherCurrentAndTotalPagesSplitIterator<>(pageToFetch -> {
            ListWithLastPage<Integer> objectListWithLastPage = new ListWithLastPage<>();
            objectListWithLastPage.setList(new ArrayList<>());
            objectListWithLastPage.setLastPage(0);
            return objectListWithLastPage;
        }, 0);

        Stream<Integer> stream = StreamSupport.stream(integerPageFetcherSplitIterator, false);
        assertTrue(stream.collect(Collectors.toList()).isEmpty());
    }

}
