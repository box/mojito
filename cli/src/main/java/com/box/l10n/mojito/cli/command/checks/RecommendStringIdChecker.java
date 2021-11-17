package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link CliChecker} that generates a recommended string id based off the file path of the file that contains the new string.
 * <br>
 * <br>
 * <b>NOTE:</b> For an id to be recommended the text unit must contain an existing message context value in it's name.
 * e.g. "Some string --- some.message.context"
 *
 * @author mallen
 */
public class RecommendStringIdChecker extends AbstractCliChecker {

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

    @Override
    public CliCheckResult run() {
        CliCheckResult result = new CliCheckResult(isHardFail(), CliCheckerType.RECOMMEND_STRING_ID_CHECKER.name());
        List<String> recommendations = getRecommendedIdPrefixUpdates();
        if(!recommendations.isEmpty()){
            result.setSuccessful(false);
            result.setNotificationText(buildNotificationText(recommendations));
        }
        return result;
    }

    private String buildNotificationText(List<String> recommendations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recommended id updates for the following strings:");
        sb.append(System.lineSeparator());
        recommendations.stream().forEach(recommendation -> sb.append("* " + recommendation + System.lineSeparator()));
        return sb.toString();
    }

    private List<String> getRecommendedIdPrefixUpdates() {
        return getAddedTextUnits().stream().map(textUnit -> getRecommendStringIdCheckResult(textUnit))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(recommendation -> recommendation.isRecommendedUpdate())
                .map(recommendation -> String.format("Please update id for string '%s' to be prefixed with '%s'", recommendation.getSource(), recommendation.getRecommendedIdPrefix()))
                .collect(Collectors.toList());
    }

    private Optional<RecommendStringIdCheckResult> getRecommendStringIdCheckResult(AssetExtractorTextUnit textUnit) {
        RecommendStringIdCheckResult recommendation = new RecommendStringIdCheckResult();
        recommendation.setSource(textUnit.getSource());
        recommendation.setRecommendedUpdate(false);
        return textUnit.getUsages().stream().findFirst().map(path -> generateRecommendation(textUnit, recommendation, removeLineNumberFromUsage(path)));
    }

    private String removeLineNumberFromUsage(String usage) {
        return usage.replaceAll(":\\d", "");
    }

    private RecommendStringIdCheckResult generateRecommendation(AssetExtractorTextUnit textUnit, RecommendStringIdCheckResult recommendation, String path) {
        Path file = Paths.get(path);
        String fileName = file.getFileName().toString();
        Deque<String> dirNames = getDirectoryNames(file);
        String msgctxt = "";
        if(textUnit.getName().contains("---")) {
            msgctxt = textUnit.getName().split("---")[1].trim();
        }
        String idPrefix = "";
        if(dirNames.size() >= 2 ){
            idPrefix = dirNames.pop() + "." + dirNames.pop() + ".";
        } else if (!dirNames.isEmpty() && !dirNames.peek().equals(fileName)){
            idPrefix = dirNames.pop() + ".";
        } else {
            idPrefix = "root.";
        }

        if(!msgctxt.isEmpty() && !msgctxt.startsWith(idPrefix)) {
            recommendation.setRecommendedUpdate(true);
            recommendation.setRecommendedIdPrefix(idPrefix);
        }

        return recommendation;
    }

    private Deque<String> getDirectoryNames(Path file) {
        Deque<String> dirNames = new ArrayDeque<>();
        while(file.getParent() != null) {
            dirNames.push(file.getParent().getFileName().toString());
            file = file.getParent();
        }
        return dirNames;
    }
}
