package com.box.l10n.mojito.iterators;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Spliterators {
  public static <T> Stream<List<T>> partitionStream(Spliterator<T> spliterator, int partitionSize) {
    return StreamSupport.stream(
        Iterables.partition(
                (Iterable<T>) () -> StreamSupport.stream(spliterator, false).iterator(),
                partitionSize)
            .spliterator(),
        false);
  }

  public static <T, R> Stream<R> partitionStreamWithIndex(
      Spliterator<T> spliterator,
      int partitionSize,
      Streams.FunctionWithIndex<? super List<T>, ? extends R> function) {
    return Streams.mapWithIndex(partitionStream(spliterator, partitionSize), function);
  }
}
