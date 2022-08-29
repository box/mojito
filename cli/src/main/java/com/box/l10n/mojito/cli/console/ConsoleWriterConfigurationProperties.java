package com.box.l10n.mojito.cli.console;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.console-writer")
public class ConsoleWriterConfigurationProperties {
  /** To enable or disable printing of ANSI escape codes in the ainsi output. */
  boolean ansiCodeEnabled = true;

  ConsoleWriter.OutputType outputType = ConsoleWriter.OutputType.ANSI_CONSOLE_AND_LOGGER;

  public boolean isAnsiCodeEnabled() {
    return ansiCodeEnabled;
  }

  public void setAnsiCodeEnabled(boolean ansiCodeEnabled) {
    this.ansiCodeEnabled = ansiCodeEnabled;
  }

  public ConsoleWriter.OutputType getOutputType() {
    return outputType;
  }

  public void setOutputType(ConsoleWriter.OutputType outputType) {
    this.outputType = outputType;
  }
}
