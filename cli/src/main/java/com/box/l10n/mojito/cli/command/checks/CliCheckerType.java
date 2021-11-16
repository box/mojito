package com.box.l10n.mojito.cli.command.checks;

public enum CliCheckerType {

    SPELL_CHECKER(SpellCliChecker.class.getName()),
    CONTEXT_COMMENT_CHECKER(ContextAndCommentCliChecker.class.getName()),
    EMPTY_PLACEHOLDER_CHECKER(EmptyPlaceholderChecker.class.getName()),
    PLACEHOLDER_COMMENT_CHECKER(PlaceholderCommentChecker.class.getName()),
    GLOSSARY_CHECKER(GlossaryChecker.class.getName()),
    RECOMMEND_STRING_ID_CHECKER(RecommendStringIdChecker.class.getName());

    String className;

    CliCheckerType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }


}
