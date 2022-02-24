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

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

/**
 * {@link AbstractCliChecker} that generates a recommended string id based off the file path of the file that contains the new string.
 *
 * @author mallen
 */
public class RecommendStringIdChecker extends AbstractCliChecker {


    public static final String ID_SEPARATOR = "---";

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
                .map(recommendation -> String.format("Please update id " + QUOTE_MARKER + "%s" + QUOTE_MARKER + " for string " + QUOTE_MARKER + "%s" + QUOTE_MARKER + " to be prefixed with '%s'", recommendation.getStringId(),
                        recommendation.getSource(), recommendation.getRecommendedIdPrefix()))
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
        String msgctxt = removeIgnoredLabelFromContext(textUnit.getName().contains(ID_SEPARATOR) ? textUnit.getName().split(ID_SEPARATOR)[1] : "");
        String cwd = Paths.get(".").toAbsolutePath().toString();
        for (String filePath : textUnit.getUsages()) {
            if (Paths.get(filePath).isAbsolute()) {
                filePath = filePath.replace(cwd.replace(FileSystems.getDefault().getSeparator() + ".", FileSystems.getDefault().getSeparator()), "");
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
            recommendation.setStringId(msgctxt);
            recommendation.setRecommendedIdPrefix(possiblePrefixes.get(0));
        }

        return recommendation;
    }

    private String removeIgnoredLabelFromContext(String msgctxt) {
        if (StringUtils.isNotBlank(cliCheckerOptions.getRecommendStringIdLabelIgnorePattern())) {
            msgctxt = msgctxt.replaceAll(cliCheckerOptions.getRecommendStringIdLabelIgnorePattern(), "");
        }
        return msgctxt.trim();
    }

    private Deque<String> getDirectoryNames(Path file) {
        Deque<String> dirNames = new ArrayDeque<>();
        while (file.getParent() != null && file.getParent().getFileName() != null) {
            dirNames.push(file.getParent().getFileName().toString());
            file = file.getParent();
        }
        return dirNames;
    }

    class RecommendStringIdCheckResult {
        String source;
        String stringId;
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

        public String getStringId() {
            return stringId;
        }

        public void setStringId(String stringId) {
            this.stringId = stringId;
        }
    }
}
