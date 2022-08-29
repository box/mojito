package com.box.l10n.mojito.phabricator.payload;

public class ListResultWithError<FiledsT> extends ResultWithError {

  ListResult<FiledsT> result;

  public ListResult<FiledsT> getResult() {
    return result;
  }

  public void setResult(ListResult<FiledsT> result) {
    this.result = result;
  }
}
