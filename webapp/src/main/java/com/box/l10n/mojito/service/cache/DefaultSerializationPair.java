package com.box.l10n.mojito.service.cache;

import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;

/**
 * Implements a {@Link SerializationPair} using Spring's default serialization and deserialization
 * converters.
 *
 * @author garion
 */
public class DefaultSerializationPair implements SerializationPair<Object> {
  private final SerializingConverter serializingConverter;
  private final DeserializingConverter deserializingConverter;

  public DefaultSerializationPair() {
    this.serializingConverter = new SerializingConverter();
    this.deserializingConverter = new DeserializingConverter();
  }

  @Override
  public Object read(byte[] bytes) {
    return deserializingConverter.convert(bytes);
  }

  @Override
  public byte[] write(Object element) {
    return serializingConverter.convert(element);
  }
}
