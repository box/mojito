package com.box.l10n.mojito.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ObjectMapperTest {

  static final ZonedDateTime A_DATE_TIME =
      ZonedDateTime.of(2023, 12, 06, 15, 22, 5, 123_456_789, ZoneOffset.UTC);

  static final String SERIALIZED_DATE_TIME_SECONDS =
      "{\"zonedDateTime\":1701876125}"; // 10 digits --> second
  static final String SERIALIZED_DATE_TIME_MILLIS =
      "{\"zonedDateTime\":1701876125123}"; // 13 digits --> millis
  static final String SERIALIZED_DATE_TIME_NANO =
      "{\"zonedDateTime\":1701876125.123456789}"; // 10 + 9 digits nano

  @Test
  public void serializationDefault() throws Exception {
    com.fasterxml.jackson.databind.ObjectMapper objectMapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    Pojo pojo = new Pojo();
    pojo.setZonedDateTime(A_DATE_TIME);
    Assertions.assertThat(objectMapper.writeValueAsString(pojo))
        .isEqualTo(SERIALIZED_DATE_TIME_NANO);
  }

  @Test
  public void deserializationDefault() throws Exception {
    com.fasterxml.jackson.databind.ObjectMapper objectMapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    // old millisecond timestamp are now parsed as second, so resulting date in the future
    Pojo pojoMillis = objectMapper.readValue(SERIALIZED_DATE_TIME_MILLIS, Pojo.class);
    Assertions.assertThat(pojoMillis.getZonedDateTime())
        .isEqualTo(ZonedDateTime.of(55900, 4, 23, 8, 5, 23, 0, ZoneOffset.UTC));

    // second timestamp are fine
    Pojo pojoSecond = objectMapper.readValue(SERIALIZED_DATE_TIME_SECONDS, Pojo.class);
    Assertions.assertThat(pojoSecond.getZonedDateTime())
        .isEqualTo(ZonedDateTime.of(2023, 12, 06, 15, 22, 5, 0, ZoneOffset.UTC));

    // nano second timestamp are fine
    Pojo pojoNano = objectMapper.readValue(SERIALIZED_DATE_TIME_NANO, Pojo.class);
    Assertions.assertThat(pojoNano.getZonedDateTime())
        .isEqualTo(ZonedDateTime.of(2023, 12, 06, 15, 22, 5, 123_456_789, ZoneOffset.UTC));
  }

  @Test
  public void serializationDefaultConfiguredMillis() throws Exception {
    com.fasterxml.jackson.databind.ObjectMapper objectMapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    Pojo pojo = new Pojo();
    pojo.setZonedDateTime(A_DATE_TIME);
    Assertions.assertThat(objectMapper.writeValueAsString(pojo))
        .isEqualTo(SERIALIZED_DATE_TIME_MILLIS);
  }

  @Test
  public void deserializationDefaultConfiguredMillis() throws Exception {
    com.fasterxml.jackson.databind.ObjectMapper objectMapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);

    // millis second are fine
    Pojo pojoMillis = objectMapper.readValue(SERIALIZED_DATE_TIME_MILLIS, Pojo.class);
    Assertions.assertThat(pojoMillis.getZonedDateTime())
        .isEqualTo(ZonedDateTime.of(2023, 12, 06, 15, 22, 5, 123_000_000, ZoneOffset.UTC));

    // second timestamp are parsed as millis second, so date is in the past
    Pojo pojoSecond = objectMapper.readValue(SERIALIZED_DATE_TIME_SECONDS, Pojo.class);
    Assertions.assertThat(pojoSecond.getZonedDateTime())
        .isEqualTo(ZonedDateTime.of(1970, 01, 20, 16, 44, 36, 125_000_000, ZoneOffset.UTC));

    // nano time are fine
    Pojo pojoNano = objectMapper.readValue(SERIALIZED_DATE_TIME_NANO, Pojo.class);
    Assertions.assertThat(pojoNano.getZonedDateTime())
        .isEqualTo(ZonedDateTime.of(2023, 12, 06, 15, 22, 5, 123_456_789, ZoneOffset.UTC));
  }

  @Test
  public void serialization() {
    ObjectMapper objectMapper = new ObjectMapper();
    Pojo pojo = new Pojo();
    pojo.setZonedDateTime(A_DATE_TIME);
    Assertions.assertThat(objectMapper.writeValueAsStringUnchecked(pojo))
        .isEqualTo(SERIALIZED_DATE_TIME_MILLIS);
  }

  @Test
  public void deserialization() {
    ObjectMapper objectMapper = new ObjectMapper();
    Pojo pojo = objectMapper.readValueUnchecked(SERIALIZED_DATE_TIME_MILLIS, Pojo.class);
    Assertions.assertThat(pojo.getZonedDateTime())
        .isEqualTo(ZonedDateTime.of(2023, 12, 06, 15, 22, 5, 123_000_000, ZoneOffset.UTC));
  }

  static class Pojo {
    ZonedDateTime zonedDateTime;

    public ZonedDateTime getZonedDateTime() {
      return zonedDateTime;
    }

    public void setZonedDateTime(ZonedDateTime zonedDateTime) {
      this.zonedDateTime = zonedDateTime;
    }
  }
}
