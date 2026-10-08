package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.entity.IntegrityChecker;
import com.box.l10n.mojito.rest.entity.IntegrityCheckerType;
import com.box.l10n.mojito.rest.entity.RepoTypeIntegrityChecker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.fusesource.jansi.Ansi;
import org.springframework.util.StringUtils;

/**
 * Parses the shared {@code --integrity-check} / {@code -it} value used by the repo commands {@code
 * repo} and by the repo-type commands {@code repo-type}.
 */
final class IntegrityCheckerCli {

  /**
   * Placeholder in {@code @Parameter} descriptions. Annotation values must be compile-time
   * constants, so the real names are substituted when usage is printed.
   */
  static final String AVAILABLE_CHECKER_TYPES_TOKEN = "{availableCheckerTypes}";

  /** Names accepted by {@link IntegrityCheckerType#valueOf(String)}, in enum order. */
  static final String AVAILABLE_CHECKER_TYPES =
      Arrays.stream(IntegrityCheckerType.values())
          .map(type -> type.name())
          .collect(Collectors.joining(", "));

  private IntegrityCheckerCli() {}

  /**
   * @param integrityCheckParam raw flag value; {@code null} when the flag was omitted
   * @param consoleWriter printer for the extracted pairs, or {@code null} to stay quiet
   * @return {@code null} when the flag was omitted, otherwise the parsed set (empty when the value
   *     is empty)
   */
  static Set<IntegrityChecker> parse(String integrityCheckParam, ConsoleWriter consoleWriter)
      throws CommandException {
    if (integrityCheckParam == null) {
      return null;
    }

    Set<IntegrityChecker> integrityCheckers = new HashSet<>();
    Set<String> integrityCheckerParams = StringUtils.commaDelimitedListToSet(integrityCheckParam);
    if (consoleWriter != null) {
      consoleWriter.a("Extracted Integrity Checkers").println();
    }

    for (String integrityCheckerParam : integrityCheckerParams) {
      String[] param = StringUtils.delimitedListToStringArray(integrityCheckerParam, ":");
      if (param.length != 2) {
        throw new ParameterException(
            "Invalid integrity checker format [" + integrityCheckerParam + "]");
      }
      String fileExtension = param[0];
      String checkerType = param[1];
      IntegrityChecker integrityChecker = new IntegrityChecker();
      integrityChecker.setAssetExtension(fileExtension);
      try {
        integrityChecker.setIntegrityCheckerType(IntegrityCheckerType.valueOf(checkerType));
      } catch (IllegalArgumentException ex) {
        throw new ParameterException("Invalid integrity checker type [" + checkerType + "]");
      }

      if (consoleWriter != null) {
        consoleWriter
            .fg(Ansi.Color.BLUE)
            .a("-- file extension = ")
            .fg(Ansi.Color.GREEN)
            .a(integrityChecker.getAssetExtension())
            .println();
        consoleWriter
            .fg(Ansi.Color.BLUE)
            .a("-- checker type = ")
            .fg(Ansi.Color.GREEN)
            .a(integrityChecker.getIntegrityCheckerType().toString())
            .println();
      }

      integrityCheckers.add(integrityChecker);
    }
    return integrityCheckers;
  }

  /**
   * Copies parsed repository checkers into the repo-type payload shape. {@code null} stays {@code
   * null} so the JSON property is omitted.
   */
  static Set<RepoTypeIntegrityChecker> toRepoTypeCheckers(Set<IntegrityChecker> checkers) {
    if (checkers == null) {
      return null;
    }
    Set<RepoTypeIntegrityChecker> converted = new HashSet<>();
    for (IntegrityChecker checker : checkers) {
      RepoTypeIntegrityChecker typeChecker = new RepoTypeIntegrityChecker();
      typeChecker.setAssetExtension(checker.getAssetExtension());
      typeChecker.setIntegrityCheckerType(checker.getIntegrityCheckerType());
      converted.add(typeChecker);
    }
    return converted;
  }

  /**
   * Sorts repo-type checkers by extension, then checker type, and formats each as {@code
   * extension:CHECKER_TYPE}. A {@code null} or empty set returns an empty list.
   */
  static List<String> sortedPairs(Collection<RepoTypeIntegrityChecker> checkers) {
    if (checkers == null || checkers.isEmpty()) {
      return List.of();
    }
    List<RepoTypeIntegrityChecker> sorted = new ArrayList<>(checkers);
    sorted.sort(
        Comparator.comparing(
                RepoTypeIntegrityChecker::getAssetExtension,
                Comparator.nullsLast(String::compareTo))
            .thenComparing(
                checker ->
                    checker.getIntegrityCheckerType() == null
                        ? ""
                        : checker.getIntegrityCheckerType().name()));
    List<String> pairs = new ArrayList<>();
    for (RepoTypeIntegrityChecker checker : sorted) {
      pairs.add(checker.getAssetExtension() + ":" + checker.getIntegrityCheckerType());
    }
    return pairs;
  }
}
