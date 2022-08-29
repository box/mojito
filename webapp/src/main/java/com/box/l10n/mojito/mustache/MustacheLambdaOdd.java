package com.box.l10n.mojito.mustache;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prints "odd" for every odd invocation of the lambda
 *
 * @author jeanaurambault
 */
public class MustacheLambdaOdd implements Mustache.Lambda {

  Map<String, AtomicInteger> counters = new HashMap<>();

  @Override
  public void execute(Template.Fragment frag, Writer out) throws IOException {
    String key = frag.execute();
    execute(key, out);
  }

  void execute(String key, Writer out) throws IOException {
    AtomicInteger counter = counters.get(key);

    if (counter == null) {
      counter = new AtomicInteger();
      counters.put(key, counter);
    }

    if (counter.getAndIncrement() % 2 != 0) {
      out.write("odd");
    }
  }
}
