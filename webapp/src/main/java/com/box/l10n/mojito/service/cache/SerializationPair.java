package com.box.l10n.mojito.service.cache;

/**
 * Implementing this interface enables custom serialization from a custom type to a byte array and
 * vice versa.
 *
 * @author garion
 */
public interface SerializationPair<T> {
  T read(byte[] bytes);

  byte[] write(T element);
}
