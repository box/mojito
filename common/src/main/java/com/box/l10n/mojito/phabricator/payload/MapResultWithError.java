package com.box.l10n.mojito.phabricator.payload;

import java.util.Map;

public class MapResultWithError<FiledsT> extends ResultWithError {

  Map<String, FiledsT> result;

  public Map<String, FiledsT> getResult() {
    return result;
  }

  public void setResult(Map<String, FiledsT> result) {
    this.result = result;
  }
}
