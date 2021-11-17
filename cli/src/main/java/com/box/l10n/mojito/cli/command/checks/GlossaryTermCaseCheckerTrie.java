package com.box.l10n.mojito.cli.command.checks;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GlossaryTermCaseCheckerTrie {

    private final Node rootNode = new Node();

    public GlossaryTermCaseCheckerTrie(List<GlossaryTerm> terms) {
        for (GlossaryTerm term : terms) {
            this.add(term);
        }
    }

    public void add(GlossaryTerm term) {
        List<String> words = CheckerUtils.getWordsInString(term.getTerm());
        Node currentNode = rootNode;
        for (String word : words) {
            word = word.toLowerCase();
            if (!currentNode.children.containsKey(word)) {
                currentNode.children.put(word, new Node());
            }
            currentNode = currentNode.children.get(word);
        }
        currentNode.glossaryTerms.add(term);
    }

    public GlossaryCaseCheckerSearchResult runGlossaryCaseCheck(String source) {
        String sourceTrimmedWhitespace = source.trim().replaceAll(" +", " ");
        List<String> words = CheckerUtils.getWordsInString(source);
        GlossaryCaseCheckerSearchResult result = new GlossaryCaseCheckerSearchResult(source);
        List<String> failures = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            Node current = rootNode;
            for (int j = i; j < words.size(); j++) {
                current = current.children.get(words.get(j).toLowerCase());
                if (current == null) {
                    break;
                }
                if (!current.glossaryTerms.isEmpty() && isGlossaryTermValid(sourceTrimmedWhitespace, current)) {
                    addFailureText(source, result, failures, current);
                }
            }
        }
        result.failures = failures;
        return result;
    }

    private boolean isGlossaryTermValid(String source, Node current) {
        boolean allFailed = true;
        for (GlossaryTerm glossaryTerm : current.glossaryTerms) {
            int index = StringUtils.indexOfIgnoreCase(source, glossaryTerm.getTerm());
            if (index > -1) {
                String glossaryTermInString = source.substring(index, index + glossaryTerm.getTerm().length());
                if (glossaryTermInString.equals(glossaryTerm.getTerm())) {
                    allFailed = false;
                }
            }
        }

        return allFailed;
    }

    private void addFailureText(String source, GlossaryCaseCheckerSearchResult result, List<String> failures, Node current) {
        result.isSuccess = false;
        if (current.glossaryTerms.stream().anyMatch(term -> term.getSeverity() == GlossaryTermSeverity.MAJOR)) {
            result.isMajorFailure = true;
            failures.add(String.format("MAJOR: String '%s' contains glossary term '%s' which must match exactly.", source,
                    current.glossaryTerms.stream().filter(term -> term.getSeverity() == GlossaryTermSeverity.MAJOR)
                            .map(GlossaryTerm::getTerm).findFirst().get()));
        } else {
            if (current.glossaryTerms.size() > 1) {
                failures.add(String.format("WARN: String '%s' contains glossary terms %s but does not exactly match one of the terms.", source,
                        current.glossaryTerms.stream().map(term -> "'" + term.getTerm() + "'")
                                .collect(Collectors.joining(" or "))));
            } else {
                failures.add(String.format("WARN: String '%s' contains glossary term '%s' but does not exactly match the glossary term.", source, current.glossaryTerms.get(0).getTerm()));
            }
        }
    }

    private class Node {
        Map<String, Node> children = new HashMap<>();
        List<GlossaryTerm> glossaryTerms = new ArrayList<>();
    }
}
