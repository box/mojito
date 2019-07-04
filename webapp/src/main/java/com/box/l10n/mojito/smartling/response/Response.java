package com.box.l10n.mojito.smartling.response;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("response")
public class Response<T> {

    String code;
    T data;
    Errors errors;

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

    public Errors getErrors() {
        return errors;
    }

    public void setErrors(Errors errors) {
        this.errors = errors;
    }
}
