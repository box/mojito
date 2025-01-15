package com.box.l10n.mojito;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.annotation.Annotation;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import org.springdoc.core.converters.models.SortObject;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A custom {@link ModelResolver} class to align generated Open API specification schemas with the
 * actual structure of the requests and responses
 */
public class CustomModelResolver extends ModelResolver {

  public CustomModelResolver(ObjectMapper mapper) {
    super(mapper);
  }

  private boolean hasAnnotation(AnnotatedType annotatedType, Class<?> annotationClass) {
    if (annotatedType.getCtxAnnotations() != null) {
      return Arrays.stream(annotatedType.getCtxAnnotations())
          .anyMatch(annotation -> annotation.annotationType().equals(annotationClass));
    }
    return false;
  }

  @Override
  public Schema<?> resolve(
      AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> next) {
    if (annotatedType.getType() instanceof SimpleType
        && (((SimpleType) annotatedType.getType()).getRawClass().equals(ZonedDateTime.class)
            || ((SimpleType) annotatedType.getType()).getRawClass().equals(Date.class))) {
      return new IntegerSchema().format("int64").example(1715699917000L);
    }
    if (annotatedType.getType().equals(SortObject.class)) {
      ObjectSchema objectSchema = new ObjectSchema();
      objectSchema.setName("SortObject");
      objectSchema.setProperties(
          Map.of(
              "empty",
              new BooleanSchema(),
              "sorted",
              new BooleanSchema(),
              "unsorted",
              new BooleanSchema()));
      return objectSchema;
    }
    if (annotatedType.getJsonViewAnnotation() != null
        && this.hasAnnotation(annotatedType, RequestBody.class)) {
      annotatedType.jsonViewAnnotation(null);
      return super.resolve(annotatedType, context, next);
    }
    if (this.hasAnnotation(annotatedType, JsonBackReference.class)
        && !this.hasAnnotation(annotatedType, io.swagger.v3.oas.annotations.media.Schema.class)) {
      return null;
    }
    return super.resolve(annotatedType, context, next);
  }

  @Override
  protected boolean hiddenByJsonView(Annotation[] annotations, AnnotatedType type) {
    JsonView jsonView = type.getJsonViewAnnotation();
    if (jsonView == null) return false;
    Class<?>[] filters = jsonView.value();
    for (Annotation ant : annotations) {
      if (ant instanceof JsonView) {
        Class<?>[] views = ((JsonView) ant).value();
        for (Class<?> f : filters) {
          for (Class<?> v : views) {
            if (v == f || v.isAssignableFrom(f)) {
              return false;
            }
          }
        }
      }
    }
    return !type.getType().getTypeName().startsWith(Page.class.getTypeName());
  }
}
