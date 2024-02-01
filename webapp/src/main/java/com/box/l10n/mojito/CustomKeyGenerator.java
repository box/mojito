package com.box.l10n.mojito;

import java.lang.reflect.Method;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

/**
 * @author aloison
 */
@Configurable
public class CustomKeyGenerator extends SimpleKeyGenerator {

  /** Generates a key, taking into account the method and its params */
  @Override
  public Object generate(Object target, Method method, Object... params) {
    // Use the java.lang.reflect.Method's generic string description as part of the key, to enable a
    // variety of
    // serialization-based caches to work out of the box, as the Method class isn't serializable
    // itself.
    return generateKey(method.toGenericString(), params);
  }
}
