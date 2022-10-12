package com.box.l10n.mojito.cli.filefinder2;

import com.google.common.base.Stopwatch;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FileTreeWalkerTest {

  @Test
  void test() {
    final Stopwatch stopwatch = Stopwatch.createStarted();

    final Path start = Paths.get("/Users/jeanaurambault/").toAbsolutePath();
    System.out.println(start);

    final ArrayList<Path> paths = new ArrayList<>();

    new FileTreeWalker(
        start,
        path -> {
          //      System.out.println(path);
          //        System.out.println(start.relativize(path));
          //        try {
          //          Thread.sleep(10);
          //        } catch (InterruptedException e) {
          //          throw new RuntimeException(e);
          //        }
          paths.add(path);
        },
        Arrays.asList(
//                  "^(?<parentpath>(?:.+/)?)res/values/(?<basename>strings)\\.(?<extension>xml)$",
//                  "^(?<parentpath>(?:.+/)?)(?<basename>[^.]+)\\.(?<extension>xlf)$",
//                  "^(?<parentpath>(?:.+/)?)(?<locale>en)\\.lproj/(?<basename>.+)\\.(?<extension>strings)$",
//                  "^(?<parentpath>(?:.+/)?)(?<locale>Base)\\.lproj/(?<basename>.+)\\.(?<extension>strings)$",
            "^(?<parentpath>(?:.+/)?)LC_MESSAGES/(?<basename>.+)\\.(?<extension>pot)$"
//                  "^(?<parentpath>(?:.+/)?)(?<basename>[^\\_]+)\\.(?<extension>properties)$"
        ),
        Arrays.asList(
//            ".git"
        ))
        .walkFileTree();

    System.out.println("source files count: " + paths.size());
    paths.stream().forEach(System.out::println);

    System.out.println("elapsed time: " + stopwatch);
  }
}
