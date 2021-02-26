package com.box.l10n.mojito.iterators;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import com.google.common.collect.UnmodifiableIterator;

import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Spliterators {

    public static <T> Stream<List<T>> partition(Spliterator<T> spliterator, int size) {
        UnmodifiableIterator<List<T>> partitions = Iterators.partition(StreamSupport.stream(spliterator, false).iterator(), size);
        return Streams.stream(partitions);
    }

    public static <T> Stream<List<T>> partition(Stream<T> stream, int size) {
        return Streams.stream(Iterators.partition(stream.iterator(), size));
    }

    public static <T,R> Stream<R> partitionWithIndex(Spliterator<T> spliterator, int size, Streams.FunctionWithIndex<List<T>,R> function) {
       return Streams.mapWithIndex(partition(spliterator, size), function);
    }
}
