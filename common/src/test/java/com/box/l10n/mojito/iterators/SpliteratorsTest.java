package com.box.l10n.mojito.iterators;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpliteratorsTest {

    static Logger logger = LoggerFactory.getLogger(SpliteratorsTest.class);

    @Test
    public void partitionStream() {
        int sourceSize = 9;
        int partitionSize = 2;

        List<List<Integer>> partitions = Spliterators
                .partitionStream(
                        IntStream.range(0, sourceSize).spliterator(),
                        partitionSize)
                .collect(Collectors.toList());
        Assertions.assertThat(partitions).isEqualTo(
                Arrays.asList(
                        Arrays.asList(0, 1),
                        Arrays.asList(2, 3),
                        Arrays.asList(4, 5),
                        Arrays.asList(6, 7),
                        Arrays.asList(8)
                )
        );
    }

    @Test
    public void partitionStreamWithPageFetcher() {
        int sourceSize = 16;
        int fetchSize = 5;
        int partitionSize = 3;

        ArrayList<String> checkCallOrders = new ArrayList<>();

        PageFetcherOffsetAndLimitSplitIterator<String> spliterator = new PageFetcherOffsetAndLimitSplitIterator<>(
                (offset, limit) ->
                {
                    checkCallOrders.add("spliterator-" + offset + "-" + limit);
                    if (offset >= sourceSize) {
                        return ImmutableList.of();
                    }
                    return IntStream.range(offset, offset + limit)
                            .mapToObj(value -> "str-" + value)
                            .collect(ImmutableList.toImmutableList());
                }, fetchSize);

        ImmutableList<List<String>> partitions = Spliterators.partitionStream(spliterator, partitionSize)
                .peek(strings -> checkCallOrders.add("chunk-" + strings.size()))
                .collect(ImmutableList.toImmutableList());

        Assertions.assertThat(checkCallOrders).isEqualTo(ImmutableList.of(
                "spliterator-0-5",
                "chunk-3",
                "spliterator-5-5",
                "chunk-3",
                "chunk-3",
                "spliterator-10-5",
                "chunk-3",
                "chunk-3",
                "spliterator-15-5",
                "chunk-3",
                "spliterator-20-5",
                "chunk-2"));

        Assertions.assertThat(partitions).isEqualTo(
                Arrays.asList(Arrays.asList("str-0", "str-1", "str-2"),
                        Arrays.asList("str-3", "str-4", "str-5"),
                        Arrays.asList("str-6", "str-7", "str-8"),
                        Arrays.asList("str-9", "str-10", "str-11"),
                        Arrays.asList("str-12", "str-13", "str-14"),
                        Arrays.asList("str-15", "str-16", "str-17"),
                        Arrays.asList("str-18", "str-19")));
    }
}