package com.box.l10n.mojito.service.pollableTask;

import java.lang.annotation.Annotation;

/**
 * Contains information about an annotated function argument
 *
 * @author jaurambault
 */
public class AnnotatedMethodParam<T extends Annotation> {

  /** The annotation type of the argument */
  private T annotation;

  /** Index of the argument in the argument list */
  private Integer index;

  /** The argument value */
  private Object arg;

  public AnnotatedMethodParam() {}

  public AnnotatedMethodParam(T annotation, Integer index, Object arg) {
    this.annotation = annotation;
    this.index = index;
    this.arg = arg;
  }

  public T getAnnotation() {
    return annotation;
  }

  public void setAnnotation(T annotation) {
    this.annotation = annotation;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public Object getArg() {
    return arg;
  }

  public void setArg(Object arg) {
    this.arg = arg;
  }
}
