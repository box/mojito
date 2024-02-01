package com.box.l10n.mojito.service.pollableTask;

/**
 * @author aloison
 */
public class PollableTaskTimeoutException extends PollableTaskException {

  public PollableTaskTimeoutException(String message) {
    super(message);
  }
}
