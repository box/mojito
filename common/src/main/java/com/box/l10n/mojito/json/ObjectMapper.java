package com.box.l10n.mojito.json;

import com.box.l10n.mojito.io.Files;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Extends {@link com.fasterxml.jackson.databind.ObjectMapper} to provide convenient methods.
 *
 * @author jaurambault
 */
public class ObjectMapper extends com.fasterxml.jackson.databind.ObjectMapper {

  public ObjectMapper() {
    registerJavaTimeModule();
    registerGuavaModule();
  }

  public ObjectMapper(ObjectMapper objectMapper) {
    super(objectMapper);
    registerJavaTimeModule();
    registerGuavaModule();
  }

  public ObjectMapper(SmileFactory smileFactory) {
    super(smileFactory);
    registerJavaTimeModule();
    registerGuavaModule();
    // Afterburner module uses bytecode generation to further speed up serialization/deserialization
    registerAfterburnerModule();
  }

  private void registerAfterburnerModule() {
    registerModule(new AfterburnerModule());
  }

  private final void registerJavaTimeModule() {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    registerModule(javaTimeModule);

    // To keep backward compatibility with the Joda output, disable write/reading nano seconds with
    // Java time and ZonedDateTime
    // also see {@link com.box.l10n.mojito.Application#mappingJackson2HttpMessageConverter}
    disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
  }

  private final void registerGuavaModule() {
    GuavaModule guavaModule = new GuavaModule();
    registerModule(guavaModule);
  }

  @Override
  public com.fasterxml.jackson.databind.ObjectMapper copy() {
    ObjectMapper objectMapper = new ObjectMapper(this);
    return objectMapper;
  }

  /**
   * Same as {@link #writeValueAsString(java.lang.Object) but throws
   * {@link RuntimeException} instead of {@link JsonProcessingException}
   *
   * @param value
   * @return
   */
  public String writeValueAsStringUnchecked(Object value) {
    try {
      return super.writeValueAsString(value);
    } catch (JsonProcessingException jpe) {
      throw new RuntimeException(jpe);
    }
  }

  public byte[] writeValueAsBytes(Object value) {
    try {
      return super.writeValueAsBytes(value);
    } catch (JsonProcessingException jpe) {
      throw new RuntimeException(jpe);
    }
  }

  public <T> T readValueUnchecked(File src, Class<T> valueType) {
    try {
      return super.readValue(src, valueType);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public <T> T readValueUnchecked(String content, Class<T> valueType) {
    try {
      return super.readValue(content, valueType);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public <T> T readValueUnchecked(String content, TypeReference<T> valueTypeRef) {
    try {
      return super.readValue(content, valueTypeRef);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void writeValueUnchecked(File file, Object content) {
    try {
      super.writeValue(file, content);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public JsonNode readTreeUnchecked(String content) {
    try {
      return super.readTree(content);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void createDirectoriesAndWrite(Path path, Object content) {
    Files.createDirectories(path.getParent());
    writeValueUnchecked(path.toFile(), content);
  }

  public static ObjectMapper withIndentedOutput() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    return objectMapper;
  }

  public static ObjectMapper withNoFailOnUnknownProperties() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper;
  }

  public static ObjectMapper withSmileEnabled() {
    ObjectMapper objectMapper = new ObjectMapper(new SmileFactory());
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper;
  }
}
