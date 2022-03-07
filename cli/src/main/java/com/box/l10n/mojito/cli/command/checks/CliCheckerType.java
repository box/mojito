package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;

public enum CliCheckerType {

    SPELL_CHECKER(SpellCliChecker.class),
    CONTEXT_COMMENT_CHECKER(ContextAndCommentCliChecker.class),
    EMPTY_PLACEHOLDER_CHECKER(EmptyPlaceholderChecker.class),
    PLACEHOLDER_COMMENT_CHECKER(PlaceholderCommentChecker.class),
    GLOSSARY_CASE_CHECKER(GlossaryCaseChecker.class),
    RECOMMEND_STRING_ID_CHECKER(RecommendStringIdChecker.class),
    CONTEXT_COMMENT_REJECT_PATTERN_CHECKER(ContextCommentRejectPatternChecker.class),
    CONTROL_CHARACTER_CHECKER(ControlCharacterChecker.class);

    Class<? extends AbstractCliChecker> type;

    CliCheckerType(Class<? extends AbstractCliChecker> type) {
        this.type = type;
    }

    public AbstractCliChecker getCliChecker() {
        AbstractCliChecker checker;

        try {
            checker = type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CommandException("Can't create CliChecker", e);
        }

        return checker;
    }

    public static CliCheckerType findByClass(Class<? extends AbstractCliChecker> clazz) {
        CliCheckerType[] cliCheckerTypes = CliCheckerType.values();
        for(CliCheckerType cliCheckerType : cliCheckerTypes) {
            if (cliCheckerType.getType() == clazz) {
                return cliCheckerType;
            }
        }
        throw new CommandException("Unknown checker class type " + clazz.getName());
    }

    private Class<? extends AbstractCliChecker> getType() {
        return type;
    }

}
