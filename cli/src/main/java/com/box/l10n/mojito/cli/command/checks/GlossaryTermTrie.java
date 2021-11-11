package com.box.l10n.mojito.cli.command.checks;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlossaryTermTrie {

    private class Node {
        Map<String, Node> children = new HashMap<>();
        GlossaryTerm glossaryTerm;
    }

    private Node rootNode = new Node();

    public GlossaryTermTrie(List<GlossaryTerm> terms) {
        for(GlossaryTerm term: terms) {
            this.add(term);
        }
    }

    public void add(GlossaryTerm term) {
        String[] words = term.getTerm().split("\\W+");
        Node currentNode = rootNode;
        for (String word : words) {
            word = word.toLowerCase();
            if(!currentNode.children.containsKey(word)) {
                currentNode.children.put(word, new Node());
            }
            currentNode = currentNode.children.get(word);
        }
        currentNode.glossaryTerm = term;
    }

    public GlossarySearchResult runGlossaryCheck(String source) {
        String[] words = source.split("\\s+");
        GlossarySearchResult result = new GlossarySearchResult(source);
        List<String> failures = new ArrayList<>();
        for(int i = 0; i < words.length; i++) {
            Node current = rootNode;
            for (int j = i; j < words.length; j++) {
                if (!current.children.containsKey(words[j].toLowerCase())){
                    break;
                }
                current = current.children.get(words[j].toLowerCase());
                if(current.glossaryTerm != null){
                    checkGlossaryTerm(source, result, failures, current);
                }
            }
        }
        result.failures = failures;
        return result;
    }

    private void checkGlossaryTerm(String source, GlossarySearchResult result, List<String> failures, Node current) {
        String trimmedWhitespace = source.trim().replaceAll(" +", " ");
        int index = StringUtils.indexOfIgnoreCase(trimmedWhitespace, current.glossaryTerm.getTerm());
        String glossaryTermInString = trimmedWhitespace.substring(index, index + current.glossaryTerm.getTerm().length());
        if(!glossaryTermInString.equals(current.glossaryTerm.getTerm())){
            result.isSuccess = false;
            if(current.glossaryTerm.getSeverity() == GlossaryTermSeverity.MAJOR) {
                result.isMajorFailure = true;
                failures.add(String.format("MAJOR: String '%s' contains glossary term '%s' which must match case exactly.", source, current.glossaryTerm.getTerm()));
            } else {
                failures.add(String.format("WARN: String '%s' contains glossary term '%s' but does not exactly match the glossary term case.", source, current.glossaryTerm.getTerm()));
            }
        }
    }
}
