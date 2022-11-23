package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify that the method should be made pollable.
 *
 * @see PollableAspect
 * @author jaurambault
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Pollable {

  /**
   * Name that will be set in {@link PollableTask#name}
   *
   * @return
   */
  String name() default "";

  /**
   * Message (might contain a message pattern with named parameter) to be set in {@link
   * PollableTask#message}
   *
   * @return
   */
  String message() default "";

  /**
   * (Optional) Message arguments for the {@link #message() }
   *
   * @return The message arguments
   */
  MsgArg[] msgArgs() default {};

  /**
   * Number of expected sub tasks to be set in {@link PollableTask#expectedSubTaskNumber}
   *
   * @return
   */
  int expectedSubTaskNumber() default 0;

  /**
   * If the method should be executed in an asynchronously or not.
   *
   * @return {@code true} if the method should be executed asynchronously else {@code false}
   *     (default).
   */
  boolean async() default false;

  /**
   * Period after which a pollable task should be considered as stuck. By default, the timeout is 1
   * hour.
   *
   * @return the delay in seconds
   */
  long timeout() default 3600;

  /**
   * Indicates if the timeout value set in the annotation should take precedence over the parent's
   * timeout value.
   *
   * @return {@code true} if a parent tasks timeout value should be overridden in the child else
   *     {@code false} (default)
   */
  boolean overrideParentTimeout() default false;
}
