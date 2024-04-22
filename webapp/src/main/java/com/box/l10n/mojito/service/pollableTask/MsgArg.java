package com.box.l10n.mojito.service.pollableTask;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this annotation to a parameter to use it as a message argument for the {@link
 * Pollable#message() }
 *
 * @author jaurambault
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface MsgArg {

  /**
   * @return parameter name
   */
  String name();

  /**
   * @return an optional access on the annotated object (for example getId() to get the id of the
   *     object instead of the object itself)
   */
  String accessor() default "";
}
