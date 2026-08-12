package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.json.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Shared helper for the CLI {@code --json} convention: emit a single machine-readable JSON document
 * on stdout (no ANSI), suitable for scripts and Jenkins to parse.
 *
 * <p>Envelope shape:
 *
 * <pre>
 * {
 *   "successful": true|false,
 *   "command": "&lt;command-name&gt;",
 *   "data": { ... },      // present on success
 *   "error": "..."        // present on failure
 * }
 * </pre>
 *
 * <p>When {@code --json} is set, commands should suppress human {@code ConsoleWriter} progress on
 * stdout and call {@link #writeSuccess} / {@link #writeFailure} instead. Progress may still go to
 * the logger / stderr.
 */
@Component
public class JsonOutput {

  @Autowired ObjectMapper objectMapper;

  /** Write a successful result envelope for {@code command} with payload {@code data}. */
  public void writeSuccess(String command, Object data) {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("successful", true);
    envelope.put("command", command);
    envelope.put("data", data);
    write(envelope);
  }

  /** Write a failure result envelope for {@code command}. */
  public void writeFailure(String command, String error) {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("successful", false);
    envelope.put("command", command == null ? "" : command);
    envelope.put("error", error == null ? "" : error);
    write(envelope);
  }

  /** Serialize {@code value} as pretty-printed JSON to stdout. */
  public void write(Object value) {
    try {
      System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize JSON output", e);
    }
  }
}
