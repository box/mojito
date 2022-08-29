package com.box.l10n.mojito.service.pollableTask;

/**
 * Indicates an issue with an annotation related to Pollable tasks.
 *
 * @author jaurambault
 */
public class IllegalPollableAnnotationException extends RuntimeException {

  public IllegalPollableAnnotationException(String message) {
    super(message);
  }

  public IllegalPollableAnnotationException(String message, Throwable e) {
    super(message, e);
  }
}
