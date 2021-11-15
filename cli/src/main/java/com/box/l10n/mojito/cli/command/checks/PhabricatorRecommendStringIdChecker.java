package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.PhabricatorPreconditions;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link CliChecker} that calls the Phabricator 'differential.getcommitpaths' API to generate
 * a recommended string id based off the file path of the file that contains the new string.
 *
 * @author mallen
 */
public class PhabricatorRecommendStringIdChecker extends AbstractCliChecker {

    class PhabricatorRecommendStringIdCheckResult {
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
        PhabricatorPreconditions.checkNotNull(cliCheckerOptions.getDifferentialDiff());
        PhabricatorPreconditions.checkNotNull(cliCheckerOptions.getDifferentialRevision());
        CliCheckResult result = new CliCheckResult(isHardFail(), CliCheckerType.PHAB_RECOMMEND_STRING_ID.name());
        String revisionId = cliCheckerOptions.getDifferentialDiff().queryDiff(getDiffId()).getRevisionId();
        Map<String, List<String>> commitPathsMap = buildCommitPathsMap(cliCheckerOptions.getDifferentialRevision().getCommitPaths(revisionId));
        List<String> recommendations = getRecommendedIdUpdates(commitPathsMap);
        if(!recommendations.isEmpty()){
            result.setSuccessful(false);
            result.setNotificationText(buildNotificationText(recommendations));
        }
        return result;
    }

    private List<String> getRecommendedIdUpdates(Map<String, List<String>> commitPathsMap) {
        return getRecommendedIdPrefixUpdates(commitPathsMap).stream()
                .map(recommendation -> String.format("Please update id for string '%s' to be prefixed with '%s'", recommendation.getSource(), recommendation.getRecommendedIdPrefix()))
                .collect(Collectors.toList());
    }

    private String buildNotificationText(List<String> recommendations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recommended id updates for the following strings:");
        sb.append(System.lineSeparator());
        recommendations.stream().forEach(recommendation -> sb.append("\t * " + recommendation + System.lineSeparator()));
        return sb.toString();
    }

    private List<PhabricatorRecommendStringIdCheckResult> getRecommendedIdPrefixUpdates(Map<String, List<String>> commitPathsMap) {
        return getAddedTextUnits().stream().flatMap(textUnit -> {
            PhabricatorRecommendStringIdCheckResult recommendation = new PhabricatorRecommendStringIdCheckResult();
            recommendation.setSource(textUnit.getSource());
            recommendation.setRecommendedUpdate(false);
            return commitPathsMap.keySet().stream().map(path -> {
                if((textUnit.getSource().contains(System.lineSeparator()) && isMultiLineSourceStringInFile(commitPathsMap, textUnit, path))
                        || commitPathsMap.get(path).contains(textUnit.getSource())){
                    generateRecommendation(textUnit, recommendation, path);
                }
                return recommendation;
            });
        }).filter(recommendation -> recommendation.isRecommendedUpdate()).collect(Collectors.toList());
    }

    private Map<String, List<String>> buildCommitPathsMap(List<String> commitPaths) {
        Map<String, List<String>> commitPathsMap = new HashMap<>();
        commitPaths.stream().forEach(path -> {
            try {
                commitPathsMap.put(path, Files.readAllLines(Paths.get(path)));
            } catch (IOException e) {
                throw new CommandException("Error reading file " + path + " in checker " + CliCheckerType.PHAB_RECOMMEND_STRING_ID.name());
            }
        });
        return commitPathsMap;
    }

    private boolean isMultiLineSourceStringInFile(Map<String, List<String>> commitPathsMap, AssetExtractorTextUnit textUnit, String path) {
        String[] lines = textUnit.getSource().split(System.lineSeparator());
        boolean found = false;
        for(String line : lines) {
            if(commitPathsMap.get(path).contains(line)) {
                found = true;
            } else {
                found = false;
            }
        }
        return found;
    }

    private PhabricatorRecommendStringIdCheckResult generateRecommendation(AssetExtractorTextUnit textUnit, PhabricatorRecommendStringIdCheckResult recommendation, String path) {
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

    private String getDiffId() {
        if(cliCheckerOptions.getDiffId() == null || cliCheckerOptions.getDiffId().isEmpty()) {
            throw new CommandException("Phabricator differential id value cannot be empty if using the 'PHAB_RECOMMEND_STRING_ID' checker.");
        }
        return cliCheckerOptions.getDiffId();
    }
}
