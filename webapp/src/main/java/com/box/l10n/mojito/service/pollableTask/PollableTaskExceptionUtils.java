package com.box.l10n.mojito.service.pollableTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PollableTaskExceptionUtils {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(PollableTaskExceptionUtils.class);

  public void processException(Throwable t, ExceptionHolder exceptionHolder) {
    if (t instanceof RuntimeException) {
      logger.error(
          "Unexpected error happened while executing the task "
              + "(if the error is known to happen it should be caught and wrapped "
              + "into an checked exception to stop logging it as an error)",
          t);
      exceptionHolder.setExpected(false);
      exceptionHolder.setException((Exception) t);
    } else if (t instanceof Exception) {
      logger.debug("Error happened during task execution", t);
      exceptionHolder.setExpected(true);
      exceptionHolder.setException((Exception) t);
    } else {
      String msg =
          "A throwable was thrown while executing the task, this is most likely a severe issue.";
      logger.error(msg, t);
      exceptionHolder.setExpected(false);
      exceptionHolder.setException(new Exception(msg, t));
    }
  }
}
