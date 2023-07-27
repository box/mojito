package com.box.l10n.mojito.rest.screenshot;

import com.ibm.icu.text.MessageFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ScreenshotWithNameAndScreenshotRunIdNotFoundException extends RuntimeException {

  public ScreenshotWithNameAndScreenshotRunIdNotFoundException(String name, Long screenshotRunId) {
    super(getMessage(name, screenshotRunId));
  }

  static String getMessage(String name, Long screenshotRunId) {
    return MessageFormat.format(
        "Screenshot with name {0} in run {1} not found", name, screenshotRunId);
  }
}
