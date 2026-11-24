package com.box.l10n.mojito;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;

/**
 * Aligns OpenAPI schemas with Mojito's Jackson serialization of {@link ZonedDateTime}.
 *
 * <p>Mojito keeps epoch-millisecond JSON numbers for datetime fields (Jackson default with {@code
 * WRITE_DATES_AS_TIMESTAMPS}). Springdoc would otherwise document them as {@code string}/{@code
 * date-time}, which breaks OpenAPI-generated clients.
 *
 * @author ehoogerbeets
 */
@Configuration
public class OpenApiConfig {

  static final String EPOCH_MILLIS_DESCRIPTION =
      "Epoch milliseconds since 1970-01-01T00:00:00Z";

  @Bean
  public PropertyCustomizer zonedDateTimeEpochMillisPropertyCustomizer() {
    return (property, type) -> {
      if (isZonedDateTime(type)) {
        return epochMillisSchema(property);
      }
      return property;
    };
  }

  @Bean
  public ParameterCustomizer zonedDateTimeEpochMillisParameterCustomizer() {
    return (parameterModel, methodParameter) -> {
      if (isZonedDateTime(methodParameter)) {
        parameterModel.setSchema(epochMillisSchema(parameterModel.getSchema()));
      }
      return parameterModel;
    };
  }

  static Schema<?> epochMillisSchema(Schema<?> existing) {
    Schema<?> schema = new IntegerSchema().format("int64");
    if (existing != null && existing.getDescription() != null) {
      schema.setDescription(existing.getDescription());
    } else {
      schema.setDescription(EPOCH_MILLIS_DESCRIPTION);
    }
    if (existing != null && existing.getNullable() != null) {
      schema.setNullable(existing.getNullable());
    }
    return schema;
  }

  static boolean isZonedDateTime(AnnotatedType annotatedType) {
    return annotatedType != null && isZonedDateTime(annotatedType.getType());
  }

  static boolean isZonedDateTime(MethodParameter methodParameter) {
    return methodParameter != null && isZonedDateTime(methodParameter.getParameterType());
  }

  static boolean isZonedDateTime(Type type) {
    if (type == null) {
      return false;
    }
    // Springdoc/Swagger resolve properties via Jackson JavaType (not Class).
    if (type instanceof JavaType javaType) {
      return ZonedDateTime.class.equals(javaType.getRawClass());
    }
    if (type instanceof Class<?> clazz) {
      return ZonedDateTime.class.equals(clazz);
    }
    if (type instanceof ParameterizedType parameterizedType) {
      return isZonedDateTime(parameterizedType.getRawType());
    }
    return ZonedDateTime.class.getTypeName().equals(type.getTypeName());
  }
}
