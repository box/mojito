package com.box.l10n.mojito.aspect.util;

import com.box.l10n.mojito.service.pollableTask.AnnotatedMethodParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * Utils to find annotated parameters in a {@link ProceedingJoinPoint}.
 *
 * @author jaurambault
 */
@Component
public class AspectJUtils {

  /**
   * Finds the first annotated parameter in the {@link ProceedingJoinPoint}.
   *
   * @param <T> the annotation type
   * @param pjp the join point
   * @param searchedAnnotation annotation class
   * @return the first annotated parameter else {@code null}
   */
  public <T extends Annotation> AnnotatedMethodParam<T> findAnnotatedMethodParam(
      ProceedingJoinPoint pjp, Class<T> searchedAnnotation) {
    AnnotatedMethodParam<T> res = null;

    List<AnnotatedMethodParam<T>> annotatedMethodParams =
        AspectJUtils.this.findAnnotatedMethodParams(pjp, searchedAnnotation, true);

    if (!annotatedMethodParams.isEmpty()) {
      res = annotatedMethodParams.get(0);
    }

    return res;
  }

  /**
   * Finds all annotated parameters in the {@link ProceedingJoinPoint}.
   *
   * @param <T> the annotation type
   * @param pjp the join point
   * @param searchedAnnotation annotation class
   * @return list (not null) of annotated parameters
   */
  public <T extends Annotation> List<AnnotatedMethodParam<T>> findAnnotatedMethodParams(
      ProceedingJoinPoint pjp, Class<T> searchedAnnotation) {
    return findAnnotatedMethodParams(pjp, searchedAnnotation, false);
  }

  private <T extends Annotation> List<AnnotatedMethodParam<T>> findAnnotatedMethodParams(
      ProceedingJoinPoint pjp, Class<T> searchedAnnotation, boolean stopOnFirst) {

    List<AnnotatedMethodParam<T>> list = new ArrayList<>();

    MethodSignature ms = (MethodSignature) pjp.getSignature();
    Method m = ms.getMethod();

    Annotation[][] parameterAnnotations = m.getParameterAnnotations();

    boolean notFoundFirst = true;
    Object[] args = pjp.getArgs();

    for (int i = 0; i < parameterAnnotations.length && notFoundFirst; i++) {

      Annotation[] annotations = parameterAnnotations[i];

      for (int j = 0; j < annotations.length && notFoundFirst; j++) {
        Annotation annotation = annotations[j];

        if (searchedAnnotation.isInstance(annotation)) {
          @SuppressWarnings("unchecked")
          AnnotatedMethodParam<T> methodParam = new AnnotatedMethodParam(annotation, i, args[i]);
          list.add(methodParam);

          if (stopOnFirst) {
            notFoundFirst = false;
          }
        }
      }
    }

    return list;
  }
}
