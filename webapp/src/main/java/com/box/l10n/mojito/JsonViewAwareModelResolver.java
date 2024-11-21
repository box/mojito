package com.box.l10n.mojito;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.jackson.ModelResolver;
import java.lang.annotation.Annotation;
import org.springframework.data.domain.Page;

/**
 * A custom {@link ModelResolver} class to hide fields that are not annotated with the {@link
 * JsonView} annotation in Swagger
 */
public class JsonViewAwareModelResolver extends ModelResolver {
  public JsonViewAwareModelResolver(ObjectMapper mapper) {
    super(mapper);
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
