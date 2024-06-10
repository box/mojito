package com.box.l10n.mojito.service.thirdparty.phrase;

import com.phrase.client.ApiException;

public class PhraseClientException extends RuntimeException {

  ApiException apiException;

  public PhraseClientException(String message) {
    super(message);
  }

  public PhraseClientException(ApiException e) {
    super(e);
    apiException = e;
  }

  public PhraseClientException(String message, ApiException apiException) {
    super(message);
    this.apiException = apiException;
  }
}
