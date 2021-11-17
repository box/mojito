package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link AbstractCliChecker} that generates a recommended string id based off the file path of the file that contains the new string.
 * <br>
 * <br>
 * <b>NOTE:</b> For an id to be recommended the text unit must contain an existing message context value in its name.
 * e.g. "Some string --- some.message.context"
 *
 * @author mallen
 */
public class RecommendStringIdChecker extends AbstractCliChecker {

    @Override
    public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
        CliCheckResult result = createCliCheckerResult();
        List<String> recommendations = getRecommendedIdPrefixUpdates(assetExtractionDiffs);
        if (!recommendations.isEmpty()) {
            result.setSuccessful(false);
            result.setNotificationText(buildNotificationText(recommendations));
        }
        return result;
    }

    private String buildNotificationText(List<String> recommendations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recommended id updates for the following strings:");
        sb.append(System.lineSeparator());
        sb.append(recommendations.stream()
                .map(recommendation -> "* " + recommendation)
                .collect(Collectors.joining(System.lineSeparator())));
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    private List<String> getRecommendedIdPrefixUpdates(List<AssetExtractionDiff> assetExtractionDiffs) {
        return getAddedTextUnits(assetExtractionDiffs).stream().map(textUnit -> getRecommendStringIdCheckResult(textUnit))
                .filter(recommendation -> recommendation.isRecommendedUpdate())
                .map(recommendation -> String.format("Please update id for string '%s' to be prefixed with '%s'", recommendation.getSource(), recommendation.getRecommendedIdPrefix()))
                .collect(Collectors.toList());
    }

    private RecommendStringIdCheckResult getRecommendStringIdCheckResult(AssetExtractorTextUnit textUnit) {
        RecommendStringIdCheckResult recommendation = new RecommendStringIdCheckResult();
        recommendation.setSource(textUnit.getSource());
        recommendation.setRecommendedUpdate(false);
        return generateRecommendation(textUnit, recommendation);
    }

    private String removeLineNumberFromUsage(String usage) {
        int lastIndexOfColon = usage.lastIndexOf(":");
        if (lastIndexOfColon > -1 && lastIndexOfColon < usage.length() - 1 && StringUtils.isNumeric(usage.substring(lastIndexOfColon + 1))) {
            usage = usage.substring(0, lastIndexOfColon);
        }
        return usage;
    }

    private RecommendStringIdCheckResult generateRecommendation(AssetExtractorTextUnit textUnit, RecommendStringIdCheckResult recommendation) {
        List<String> possiblePrefixes = new ArrayList<>();
        String msgctxt = textUnit.getName().contains("---") ? textUnit.getName().split("---")[1].trim() : "";
        String cwd = Paths.get(".").toAbsolutePath() + FileSystems.getDefault().getSeparator();

        for (String filePath : textUnit.getUsages()) {
            if (Paths.get(filePath).isAbsolute()) {
                filePath = filePath.replace(cwd, "");
            }
            Path file = Paths.get(removeLineNumberFromUsage(filePath));
            String fileName = file.getFileName().toString();
            Deque<String> dirNames = getDirectoryNames(file);
            String idPrefix = "";
            if (dirNames.size() >= 2) {
                idPrefix = dirNames.pop() + "." + dirNames.pop() + ".";
            } else if (!dirNames.isEmpty() && !dirNames.peek().equals(fileName)) {
                idPrefix = dirNames.pop() + ".";
            } else {
                idPrefix = "root.";
            }
            possiblePrefixes.add(idPrefix);
        }

        if (!possiblePrefixes.isEmpty() && !possiblePrefixes.stream().anyMatch(prefix -> msgctxt.startsWith(prefix))) {
            recommendation.setRecommendedUpdate(true);
            recommendation.setRecommendedIdPrefix(possiblePrefixes.get(0));
        }

        return recommendation;
    }

    private Deque<String> getDirectoryNames(Path file) {
        Deque<String> dirNames = new ArrayDeque<>();
        while (file.getParent() != null) {
            dirNames.push(file.getParent().getFileName().toString());
            file = file.getParent();
        }
        return dirNames;
    }

    class RecommendStringIdCheckResult {
        String source;
        String recommendedIdPrefix;
        boolean isRecommendedUpdate;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getRecommendedIdPrefix() {
            return recommendedIdPrefix;
        }

        public void setRecommendedIdPrefix(String recommendedIdPrefix) {
            this.recommendedIdPrefix = recommendedIdPrefix;
        }

        public boolean isRecommendedUpdate() {
            return isRecommendedUpdate;
        }

        public void setRecommendedUpdate(boolean recommendedUpdate) {
            isRecommendedUpdate = recommendedUpdate;
        }
    }
}
