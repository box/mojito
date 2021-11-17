package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GlossaryChecker extends AbstractCliChecker {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CliCheckResult run() {
        CliCheckResult cliCheckResult = new CliCheckResult(isHardFail(), CliCheckerType.GLOSSARY_CHECKER.name());
        try {
            GlossaryTermTrie glossaryTermTrie = getGlossaryTermTrie();
            List<GlossarySearchResult> failures = getGlossarySearchResults(glossaryTermTrie);
            if(!failures.isEmpty()){
                if(failures.stream().anyMatch(result -> result.isMajorFailure())){
                    cliCheckResult.setSuccessful(false);
                }
                cliCheckResult.setNotificationText(buildNotificationText(failures).toString());
            }
        } catch (IOException e) {
            throw new CommandException(String.format("Error retrieving glossary terms from file path %s: %s", cliCheckerOptions.getGlossaryFilePath(), e.getMessage()));
        }

        return cliCheckResult;
    }

    private List<GlossarySearchResult> getGlossarySearchResults(GlossaryTermTrie glossaryTermTrie) {
        List<GlossarySearchResult> failures = getAddedTextUnits().stream()
            .map(assetExtractorTextUnit -> glossaryTermTrie.runGlossaryCheck(assetExtractorTextUnit.getSource()))
            .filter(result -> !result.isSuccess())
            .collect(Collectors.toList());
        return failures;
    }

    private GlossaryTermTrie getGlossaryTermTrie() throws IOException {
        List<GlossaryTerm> terms = Arrays.asList(objectMapper.readValue(Paths.get(cliCheckerOptions.getGlossaryFilePath()).toFile(), GlossaryTerm[].class));
        GlossaryTermTrie glossaryTermTrie = new GlossaryTermTrie(terms);
        return glossaryTermTrie;
    }

    private StringBuilder buildNotificationText(List<GlossarySearchResult> failures){
        StringBuilder builder = new StringBuilder();
        builder.append("Glossary check failures:");
        failures.stream().forEach(failure -> {
            builder.append(System.lineSeparator());
            failure.getFailures().stream().forEach(failureText -> {
                builder.append(String.format("* %s", failureText));
                builder.append(System.lineSeparator());
            });
        });
        builder.append(System.lineSeparator());
        return builder;
    }
}
