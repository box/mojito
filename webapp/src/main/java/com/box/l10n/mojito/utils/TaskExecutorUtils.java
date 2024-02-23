package com.box.l10n.mojito.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TaskExecutorUtils {

  public static <T> void waitForAllFutures(List<CompletableFuture<T>> futures) {
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
  }
}
