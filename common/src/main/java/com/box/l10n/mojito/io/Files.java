package com.box.l10n.mojito.io;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class Files {

  private Files() {}

  public static Path createDirectories(Path dir, FileAttribute<?>... attrs) {
    try {
      return java.nio.file.Files.createDirectories(dir, attrs);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void deleteRecursivelyIfExists(Path path) {
    try {
      if (path.toFile().exists()) {
        MoreFiles.deleteRecursively(path, RecursiveDeleteOption.ALLOW_INSECURE);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Stream<Path> find(
      Path start,
      int maxDepth,
      BiPredicate<Path, BasicFileAttributes> matcher,
      FileVisitOption... options) {
    try {
      return java.nio.file.Files.find(start, maxDepth, matcher, options);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void write(Path path, String content) {
    try {
      java.nio.file.Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String readString(Path path) {
    try {
      return java.nio.file.Files.readString(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Stream<String> lines(Path path) {
    try {
      return java.nio.file.Files.lines(path, StandardCharsets.UTF_8);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }

  public static void write(Path path, Stream<String> lines) {
    write(path, (Iterable<String>) lines::iterator);
  }

  public static void write(
      Path path, Iterable<? extends CharSequence> lines, OpenOption... options) {
    try {
      java.nio.file.Files.write(path, lines, StandardCharsets.UTF_8, options);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }
}
