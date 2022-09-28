package com.box.l10n.mojito.cli.filefinder2;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FileTreeWalkerTest {

  @Test
  void test() {
    final Path start = Paths.get("target/").toAbsolutePath();
    System.out.println(start);
    new FileTreeWalker(start, path -> {
      System.out.println(path);
      System.out.println(start.relativize(path));
    }, Arrays.asList(".*\\.resx"),
        Arrays.asList(".*\\.fr\\.resx", ".*filefinder")).walkFileTree();
  }
}