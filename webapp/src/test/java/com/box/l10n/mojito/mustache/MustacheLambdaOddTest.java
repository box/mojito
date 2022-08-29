package com.box.l10n.mojito.mustache;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.Writer;
import org.junit.Test;

/** @author jeanaurambault */
public class MustacheLambdaOddTest {

  @Test
  public void testExecute() throws IOException {
    MustacheLambdaOdd mustacheLambdaOdd = new MustacheLambdaOdd();
    String key = "key";

    Writer mock = mock(Writer.class);
    mustacheLambdaOdd.execute(key, mock);
    mustacheLambdaOdd.execute(key, mock);
    mustacheLambdaOdd.execute(key, mock);
    mustacheLambdaOdd.execute(key, mock);

    verify(mock, times(2)).write("odd");
  }

  @Test
  public void testExecuteMultipleLambda() throws IOException {
    MustacheLambdaOdd mustacheLambdaOdd = new MustacheLambdaOdd();

    String key = "key";
    String key2 = "key2";

    Writer mock = mock(Writer.class);
    Writer mock2 = mock(Writer.class);
    mustacheLambdaOdd.execute(key, mock);
    mustacheLambdaOdd.execute(key2, mock2);
    mustacheLambdaOdd.execute(key, mock);
    mustacheLambdaOdd.execute(key2, mock2);

    verify(mock, times(1)).write("odd");
    verify(mock2, times(1)).write("odd");
  }
}
