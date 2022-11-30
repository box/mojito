package com.box.l10n.mojito.github;

public class GithubException extends RuntimeException {

  public GithubException(String message) {
    super(message);
  }

  public GithubException(String message, Throwable e) {
    super(message, e);
  }
}
