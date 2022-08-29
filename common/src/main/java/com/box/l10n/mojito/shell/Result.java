package com.box.l10n.mojito.shell;

public class Result {
  int exitCode;
  String output;
  String error;

  public Result(int exitCode, String output, String error) {
    this.exitCode = exitCode;
    this.output = output;
    this.error = error;
  }

  public int getExitCode() {
    return exitCode;
  }

  public void setExitCode(int exitCode) {
    this.exitCode = exitCode;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }
}
