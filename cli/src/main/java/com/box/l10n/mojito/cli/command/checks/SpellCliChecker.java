package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import dumonts.hunspell.Hunspell;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

/**
 * {@link AbstractCliChecker} that uses hunspell native libraries to spell check words in source strings.
 * <br>
 * <br>
 * <b>NOTE:</b> The hunspell libraries must be available on your system separately for this check to execute successfully.
 *
 * @author mallen
 */
public class SpellCliChecker extends AbstractCliChecker {

    static Logger logger = LoggerFactory.getLogger(SpellCliChecker.class);

    private Hunspell hunspell;

    static class SpellCliCheckerResult {

        String source;
        Map<String, List<String>> suggestionMap;
        boolean isSuccessful;

        public SpellCliCheckerResult(String source, Map<String, List<String>> suggestionMap) {
            this.source = source;
            this.suggestionMap = suggestionMap;
            this.isSuccessful = true;
        }

        public String getSource() {
            return source;
        }

        public Map<String, List<String>> getSuggestionMap() {
            return suggestionMap;
        }

        public boolean isSuccessful() {
            return isSuccessful;
        }

        public void setSuccessful(boolean successful) {
            this.isSuccessful = successful;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SpellCliCheckerResult that = (SpellCliCheckerResult) o;
            return isSuccessful == that.isSuccessful && Objects.equals(source, that.source) && Objects.equals(suggestionMap, that.suggestionMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, suggestionMap, isSuccessful);
        }
    }


    @Override
    public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
        checkDictionaryFiles();
        hunspell = getHunspellInstance();
        List<String> sourceStrings = getSourceStringsFromDiff(assetExtractionDiffs);
        loadAdditionalWordsToDictionary(cliCheckerOptions.getDictionaryAdditionsFilePath());
        Map<String, Map<String, List<String>>> failureMap = spellCheck(sourceStrings);
        CliCheckResult cliCheckResult = createCliCheckerResult();
        if(!failureMap.isEmpty()) {
            cliCheckResult.setSuccessful(false);
            cliCheckResult.setNotificationText(buildNotificationText(failureMap));
        }

        return cliCheckResult;
    }

    protected Hunspell getHunspellInstance(){
        if (hunspell == null) {
            hunspell = new Hunspell(Paths.get(cliCheckerOptions.getDictionaryFilePath()), Paths.get(cliCheckerOptions.getDictionaryAffixFilePath()));
        }
        return hunspell;
    }

    private void checkDictionaryFiles() {
        if(StringUtils.isBlank(cliCheckerOptions.getDictionaryFilePath())) {
            throw new CommandException("The dictionary file path parameter cannot be empty when using the spell checker.");
        } else if (StringUtils.isBlank(cliCheckerOptions.getDictionaryAffixFilePath())) {
            throw new CommandException("The dictionary affix file path parameter cannot be empty when using the spell checker.");
        } else if (!Files.exists(Paths.get(cliCheckerOptions.getDictionaryFilePath()))) {
            throw new CommandException(cliCheckerOptions.getDictionaryFilePath() + " does not exist.");
        } else if (!Files.exists(Paths.get(cliCheckerOptions.getDictionaryAffixFilePath()))) {
            throw new CommandException(cliCheckerOptions.getDictionaryAffixFilePath() + " does not exist.");
        }
    }

    private Map<String, Map<String, List<String>>> spellCheck(List<String> sourceStrings) {

        return sourceStrings.stream()
                .map(sourceString -> getSpellCliCheckerResult(sourceString))
                .filter(result -> !result.isSuccessful())
                .distinct()
                .collect(Collectors.toMap(SpellCliCheckerResult::getSource, SpellCliCheckerResult::getSuggestionMap));

    }

    private SpellCliCheckerResult getSpellCliCheckerResult(String sourceString) {
        SpellCliCheckerResult result = new SpellCliCheckerResult(sourceString, spellCheckSourceString(CheckerUtils.getWordsInString(removePlaceholdersFromString(sourceString))));
        if(!result.getSuggestionMap().isEmpty()) {
            result.setSuccessful(false);
        }
        return result;
    }

    private String removePlaceholdersFromString(String sourceString) {
        String stringWithoutPlaceholders = sourceString;
        for(PlaceholderRegularExpressions regex : cliCheckerOptions.getParameterRegexSet()) {
            stringWithoutPlaceholders = removePlaceholders(stringWithoutPlaceholders, regex);
        }
        return stringWithoutPlaceholders;
    }

    private String removePlaceholders(String stringWithoutPlaceholders, PlaceholderRegularExpressions regex) {
        if(regex.equals(PlaceholderRegularExpressions.SINGLE_BRACE_REGEX) || regex.equals(PlaceholderRegularExpressions.DOUBLE_BRACE_REGEX)) {
            stringWithoutPlaceholders = removeBracketedPlaceholders(stringWithoutPlaceholders);
        }else {
            stringWithoutPlaceholders = stringWithoutPlaceholders.replaceAll(regex.getRegex(), "");
        }
        return stringWithoutPlaceholders;
    }

    private String removeBracketedPlaceholders(String stringWithoutPlaceholders) {
        int index = stringWithoutPlaceholders.indexOf("{");
        while(index != -1 && stringWithoutPlaceholders.contains("}")) {
            int associatedClosingBraceIndex = getEndOfPlaceholderIndex(stringWithoutPlaceholders, index);
            stringWithoutPlaceholders = stringWithoutPlaceholders.substring(0,index) + stringWithoutPlaceholders.substring(associatedClosingBraceIndex + 1);
            index = stringWithoutPlaceholders.indexOf("{");
        }
        return stringWithoutPlaceholders;
    }

    private int getEndOfPlaceholderIndex(String str, int startIndex){
        ArrayDeque<Character> stack = new ArrayDeque<>();
        int indexCount = startIndex;
        for (Character c : str.substring(startIndex).toCharArray()) {
            if (c.equals('{')) {
                stack.push(c);
                indexCount++;
                continue;
            } else if (c.equals('}')) {
                if(stack.isEmpty()){
                    throw new CommandException("Invalid number of opening brackets in string.");
                }
                stack.pop();
                if(stack.isEmpty()) {
                    return indexCount;
                }
            }
            indexCount++;
        }
        if(!stack.isEmpty()) {
            throw new CommandException("Invalid number of closing brackets in string.");
        }
        return -1;
    }

    private void loadAdditionalWordsToDictionary(String additionalWordsFilePath) {

        if (additionalWordsFilePath != null && !additionalWordsFilePath.isEmpty()) {
            try {
                Files.readAllLines(Paths.get(additionalWordsFilePath)).stream().forEach(word -> {
                    String w = word.trim();
                    logger.debug("Adding {} to dictionary", w);
                    hunspell.add(w);
                });
            } catch (IOException e) {
                logger.error("Error adding additional words to dictionary: {}", e);
                throw new CommandException("Error adding additional words to dictionary: " + e.getMessage());
            }
        }
    }

    private Map<String, List<String>> spellCheckSourceString(List<String> words) {
        Map<String, List<String>> failureMap = new HashMap<>();
        Set<String> checked = new HashSet<>();
        for (String word : words) {
            if (!checked.contains(word) && !hunspell.spell(word)) {
                List<String> suggestions = hunspell.suggest(word);
                logger.debug("{} is spelt incorrectly. Suggested correct spellings are {}", word, suggestions);
                failureMap.put(word, suggestions);
                checked.add(word);
            }
        }
        return failureMap;
    }

    private String buildNotificationText(Map<String, Map<String, List<String>>> failureMap) {
        StringBuilder notificationText = new StringBuilder();
        notificationText.append(failureMap.keySet().stream()
                .map(sourceString -> buildFailureText(failureMap, sourceString))
                .collect(Collectors.joining(System.lineSeparator())));
        notificationText.append(System.lineSeparator() + System.lineSeparator());
        addDictionaryUpdateInformation(notificationText);

        return notificationText.toString();
    }

    private void addDictionaryUpdateInformation(StringBuilder notificationText) {
        if(cliCheckerOptions.getDictionaryAdditionsFilePath() != null && !cliCheckerOptions.getDictionaryAdditionsFilePath().isEmpty()) {
            notificationText.append("If a word is correctly spelt please add your spelling to " +
                    cliCheckerOptions.getDictionaryAdditionsFilePath() +
                    " to avoid future false negatives.");
        }
    }

    private String buildFailureText(Map<String, Map<String, List<String>>> failureMap, String sourceString) {
        return failureMap.get(sourceString).keySet().stream()
                .map(misspelling -> {
                    StringBuilder builder = new StringBuilder();
                    builder.append("The string " + QUOTE_MARKER + sourceString + QUOTE_MARKER + " contains misspelled words:" + System.lineSeparator());
                    List<String> suggestions = failureMap.get(sourceString).get(misspelling);
                    builder.append(" * '" + misspelling + "' ");
                    if(!suggestions.isEmpty()){
                        builder.append("- Did you mean ");
                        builder.append(suggestions.stream().collect(Collectors.collectingAndThen(Collectors.toList(), joinCommaSeparated(", ", " or "))));
                        builder.append("?");
                    }
                    return builder.toString();
                })
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static Function<List<String>, String> joinCommaSeparated(String delimiter, String finalDelimiter) {
        return result -> {
            int last = result.size() - 1;
            if(last < 1) {
                return String.join(delimiter, result);
            }
            return String.join(finalDelimiter, String.join(delimiter, result.subList(0, last)), result.get(last));
        };
    }
}
