package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for Commands, provides basic support for usage display and the shared {@code --json}
 * machine-readable output convention.
 *
 * @author jaurambault
 */
public abstract class Command {

  static final String HELP_LONG = "--help";
  static final String HELP_SHORT = "-h";
  static final String HELP_DESCRIPTION = "Show help";

  public static final String JSON_LONG = "--json";
  public static final String JSON_DESCRIPTION =
      "Emit a single machine-readable JSON result on stdout instead of human console output";

  public List<String> originalArgs;

  @Parameter(
      names = {HELP_LONG, HELP_SHORT},
      help = true,
      description = HELP_DESCRIPTION)
  private boolean help;

  @Parameter(
      names = {JSON_LONG},
      description = JSON_DESCRIPTION)
  private boolean jsonOutput;

  @Autowired(required = false)
  JsonOutput jsonOutputWriter;

  /**
   * Method to be overridden to implement the business logic of this command
   *
   * @throws CommandException
   */
  protected abstract void execute() throws CommandException;

  /**
   * Runs the command. Implements usage display or and calls {@link #execute() } which actually
   * contains the business logic of the command.
   *
   * @throws CommandException
   */
  public void run() throws CommandException {
    if (shouldShowUsage()) {
      showUsage();
    } else {
      execute();
    }
  }

  /** Indicates if the command should be shown in help command list */
  public boolean shouldShowInCommandList() {
    return true;
  }

  /**
   * Indicates if the command usage should be shown
   *
   * @return
   */
  public boolean shouldShowUsage() {
    return help;
  }

  /**
   * Whether this invocation requested machine-readable JSON on stdout ({@code --json}). When true,
   * commands should suppress human console progress and emit a JSON envelope via {@link
   * #writeJsonSuccess(Object)} / {@link #writeJsonFailure(String)}.
   */
  public boolean isJsonOutput() {
    return jsonOutput;
  }

  /** Write a successful JSON envelope (only when {@link #isJsonOutput()} is true). */
  protected void writeJsonSuccess(Object data) {
    if (!isJsonOutput()) {
      return;
    }
    requireJsonOutputWriter().writeSuccess(getName(), data);
  }

  /** Write a failure JSON envelope (only when {@link #isJsonOutput()} is true). */
  protected void writeJsonFailure(String error) {
    if (!isJsonOutput()) {
      return;
    }
    requireJsonOutputWriter().writeFailure(getName(), error);
  }

  private JsonOutput requireJsonOutputWriter() {
    if (jsonOutputWriter == null) {
      throw new IllegalStateException(
          "JsonOutput bean is not available; --json requires Spring wiring");
    }
    return jsonOutputWriter;
  }

  /** Shows the command usage. */
  void showUsage() {
    new L10nJCommander().usage(getName());
  }

  /**
   * Gets the {@link Parameters} annotation from the command.
   *
   * @return the {@link Parameters} annotation
   */
  Parameters getParameters() {
    Parameters parameters = this.getClass().getAnnotation(Parameters.class);
    Preconditions.checkNotNull(
        parameters, "There must be @Parameters on the Command class: " + this.getClass());
    return parameters;
  }

  /**
   * Gets the command description from the {@link Parameters} annotation.
   *
   * @return the command description
   */
  public String getDescription() {
    return getParameters().commandDescription();
  }

  /**
   * Gets the names of this command (should be long name first followed by short name).
   *
   * @return list of command names
   */
  public List<String> getNames() {
    Parameters parameters = getParameters();
    String[] commandNames = parameters.commandNames();
    return Arrays.asList(commandNames);
  }

  /**
   * Gets the first name of this command (should be the long name).
   *
   * @return name of this command
   */
  public String getName() {
    List<String> names = getNames();

    if (names.isEmpty()) {
      throw new RuntimeException("A command must have a name see @Parameters");
    }

    return names.get(0);
  }

  public void setOriginalArgs(List<String> originalArgs) {
    this.originalArgs = originalArgs;
  }
}
