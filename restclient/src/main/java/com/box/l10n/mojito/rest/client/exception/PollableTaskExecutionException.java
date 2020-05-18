package com.box.l10n.mojito.rest.client.exception;

import com.box.l10n.mojito.rest.entity.ErrorMessage;

/**
 * @author aloison
 */
public class PollableTaskExecutionException extends PollableTaskException {

    ErrorMessage errorMessage;

    public PollableTaskExecutionException(ErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        this.errorMessage = errorMessage;
    }
}
