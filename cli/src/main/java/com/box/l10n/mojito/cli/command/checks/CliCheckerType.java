package com.box.l10n.mojito.cli.command.checks;

public enum CliCheckerType {

    SPELL_CHECKER(SpellCliChecker.class.getName()),
    CONTEXT_COMMENT_CHECKER(ContextAndCommentCliChecker.class.getName()),
    EMPTY_PLACEHOLDER_CHECKER(EmptyPlaceholderChecker.class.getName());

    String className;

    CliCheckerType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
