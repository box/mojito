package com.box.l10n.mojito.smartling.response;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;

@JsonRootName("response")
public class Response<T> {

  String code;
  T data;
  List<Error> errors;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public List<Error> getErrors() {
    return errors;
  }

  public void setErrors(List<Error> errors) {
    this.errors = errors;
  }
}
