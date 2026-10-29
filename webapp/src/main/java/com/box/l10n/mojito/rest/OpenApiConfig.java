package com.box.l10n.mojito.rest;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aligns the generated OpenAPI schema with Mojito's Jackson wire format for temporal types used by
 * the REST API.
 *
 * <p>HTTP JSON serializes {@link ZonedDateTime}, {@link Instant}, and {@link Date} as epoch
 * milliseconds (JSON integers). springdoc defaults those types to {@code string}/{@code
 * date-time}, which breaks clients generated from {@code /api-docs}.
 */
@Configuration
public class OpenApiConfig {

  static final long EXAMPLE_EPOCH_MILLIS = 1701876125123L;

  static final String EPOCH_MILLIS_DESCRIPTION =
      "Epoch milliseconds since 1970-01-01T00:00:00Z";

  /**
   * Prefer swagger's ModelResolver that follows the application's Jackson {@link ObjectMapper}
   * (including {@code WRITE_DATES_AS_TIMESTAMPS}). springdoc may still emit {@code string}/{@code
   * date-time} for these types via PrimitiveType; the {@link PropertyCustomizer} below covers that
   * case.
   */
  @Bean
  public ModelResolver modelResolver(ObjectMapper objectMapper) {
    return new ModelResolver(objectMapper);
  }

  @Bean
  public PropertyCustomizer epochMillisTemporalPropertyCustomizer() {
    return (property, type) -> {
      if (isEpochMillisTemporal(type)) {
        return epochMillisSchema();
      }
      return property;
    };
  }

  private static boolean isEpochMillisTemporal(AnnotatedType type) {
    if (type == null || type.getType() == null) {
      return false;
    }
    JavaType javaType = Json.mapper().constructType(type.getType());
    if (javaType == null) {
      return false;
    }
    Class<?> raw = javaType.getRawClass();
    // Types actually used on the REST API surface as JSON date/time fields.
    return ZonedDateTime.class.equals(raw)
        || Instant.class.equals(raw)
        || Date.class.equals(raw);
  }

  private static Schema<?> epochMillisSchema() {
    return new IntegerSchema()
        .format("int64")
        .description(EPOCH_MILLIS_DESCRIPTION)
        .example(EXAMPLE_EPOCH_MILLIS);
  }
}
