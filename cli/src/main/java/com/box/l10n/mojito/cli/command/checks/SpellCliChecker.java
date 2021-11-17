package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import dumonts.hunspell.Hunspell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link CliChecker} that uses hunspell native libraries to spell check words in source strings.
 * <br>
 * <br>
 * <b>NOTE:</b> The hunspell libraries must be available on your system separately for this check to execute successfully.
 *
 * @author mallen
 */
public class SpellCliChecker extends AbstractCliChecker {

    static Logger logger = LoggerFactory.getLogger(SpellCliChecker.class);

    Hunspell hunspell = Hunspell.forDictionaryInResources("en_US", "dictionaries/");

    class SpellCliCheckerResult {

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
    }

    @Override
    public CliCheckResult run() {
        List<String> sourceStrings = getSourceStringsFromDiff();
        loadAdditionalWordsToDictionary(cliCheckerOptions.getDictionaryAdditionsFilePath());
        Map<String, Map<String, List<String>>> failureMap = spellCheck(sourceStrings);
        CliCheckResult cliCheckResult = new CliCheckResult(isHardFail(), CliCheckerType.SPELL_CHECKER.name());
        if(!failureMap.isEmpty()) {
            cliCheckResult.setSuccessful(false);
            cliCheckResult.setNotificationText(buildNotificationText(failureMap));
        }

        return cliCheckResult;
    }

    private Map<String, Map<String, List<String>>> spellCheck(List<String> sourceStrings) {

        return sourceStrings.stream()
                .map(sourceString -> getSpellCliCheckerResult(sourceString))
                .filter(result -> !result.isSuccessful)
                .collect(Collectors.toMap(SpellCliCheckerResult::getSource, SpellCliCheckerResult::getSuggestionMap));

    }

    private SpellCliCheckerResult getSpellCliCheckerResult(String sourceString) {
        SpellCliCheckerResult result = new SpellCliCheckerResult(sourceString, spellCheckSourceString(Arrays.asList(removePlaceholdersFromString(sourceString).split("\\W+"))));
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
            String tmp = stringWithoutPlaceholders.substring(index, associatedClosingBraceIndex + 1 < stringWithoutPlaceholders.length()? associatedClosingBraceIndex + 1 : associatedClosingBraceIndex).replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}");
            stringWithoutPlaceholders = stringWithoutPlaceholders.replaceAll(tmp, "");
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

    private List<String> getSourceStringsFromDiff() {
        List<String> sourceStrings = new ArrayList<>();
        getAddedTextUnits().stream().forEach(assetExtractorTextUnit -> sourceStrings.add(assetExtractorTextUnit.getSource()));
        return sourceStrings;
    }

    private String buildNotificationText(Map<String, Map<String, List<String>>> failureMap) {
        StringBuilder notificationText = new StringBuilder();
        failureMap.keySet().stream().forEach(sourceString -> {
            buildFailureText(failureMap, notificationText, sourceString);
        });
        notificationText.append(System.lineSeparator());
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

    private void buildFailureText(Map<String, Map<String, List<String>>> failureMap, StringBuilder notificationText, String sourceString) {
        notificationText.append("The string '" + sourceString + "' contains misspelled words:" + System.lineSeparator());
        failureMap.get(sourceString).keySet().stream().forEach(misspelling -> {
            List<String> suggestions = failureMap.get(sourceString).get(misspelling);
            notificationText.append(" * '" + misspelling + "' ");
            if(!suggestions.isEmpty()){
                notificationText.append("- Did you mean ");
                notificationText.append(suggestions.stream().collect(Collectors.collectingAndThen(Collectors.toList(), joinCommaSeparated(", ", " or "))));
                notificationText.append("?");
            }
            notificationText.append(System.lineSeparator());
        });
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