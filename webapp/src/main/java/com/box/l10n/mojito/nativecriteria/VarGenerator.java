package com.box.l10n.mojito.nativecriteria;

import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.annotations.common.util.StringHelper;

/** @author jeanaurambault */
public class VarGenerator {

  static AtomicInteger idGenerator = new AtomicInteger();

  static int nextValue() {
    int incrementAndGet = idGenerator.incrementAndGet();
    if (incrementAndGet == 1000) {
      idGenerator.set(0);
    }
    return incrementAndGet;
  }

  public static String gen(String description) {
    return StringHelper.generateAlias(description.replaceAll("\\(|\\)", ""), nextValue());
  }
}
