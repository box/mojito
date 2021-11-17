package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GlossaryCaseChecker extends AbstractCliChecker {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
        CliCheckResult cliCheckResult = createCliCheckerResult();
        try {
            GlossaryTermCaseCheckerTrie glossaryTermCaseCheckerTrie = getGlossaryTermTrie();
            List<GlossaryCaseCheckerSearchResult> failures = getGlossarySearchResults(glossaryTermCaseCheckerTrie, assetExtractionDiffs);
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

    private List<GlossaryCaseCheckerSearchResult> getGlossarySearchResults(GlossaryTermCaseCheckerTrie glossaryTermCaseCheckerTrie, List<AssetExtractionDiff> assetExtractionDiffs) {
        List<GlossaryCaseCheckerSearchResult> failures = getAddedTextUnits(assetExtractionDiffs).stream()
            .map(assetExtractorTextUnit -> glossaryTermCaseCheckerTrie.runGlossaryCaseCheck(assetExtractorTextUnit.getSource()))
            .filter(result -> !result.isSuccess())
            .collect(Collectors.toList());
        return failures;
    }

    private GlossaryTermCaseCheckerTrie getGlossaryTermTrie() throws IOException {
        List<GlossaryTerm> terms = Arrays.asList(objectMapper.readValue(Paths.get(cliCheckerOptions.getGlossaryFilePath()).toFile(), GlossaryTerm[].class));
        GlossaryTermCaseCheckerTrie glossaryTermCaseCheckerTrie = new GlossaryTermCaseCheckerTrie(terms);
        return glossaryTermCaseCheckerTrie;
    }

    private StringBuilder buildNotificationText(List<GlossaryCaseCheckerSearchResult> failures){
        StringBuilder builder = new StringBuilder();
        builder.append("Glossary check failures:");
        builder.append(System.lineSeparator());
        builder.append(failures.stream()
                .map(failure -> getFailureText(builder, failure))
                .collect(Collectors.joining(System.lineSeparator())));
        return builder;
    }

    private String getFailureText(StringBuilder builder, GlossaryCaseCheckerSearchResult failure) {
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        for (String failureText : failure.getFailures()) {
            builder.append(String.format("* %s", failureText));
            builder.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
