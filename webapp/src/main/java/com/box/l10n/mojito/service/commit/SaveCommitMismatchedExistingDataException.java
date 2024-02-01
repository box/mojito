package com.box.l10n.mojito.service.commit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author garion
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SaveCommitMismatchedExistingDataException extends Exception {

  public SaveCommitMismatchedExistingDataException(
      String repositoryId, String existingValue, String newValue) {
    super(
        String.format(
            "Could not save the commit as one with the same name and repository already exits, however the following field has a different existing value: %s . Existing value: %s . New value: %s .",
            repositoryId, existingValue, newValue));
  }
}
