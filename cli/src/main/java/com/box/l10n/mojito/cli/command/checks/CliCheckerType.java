package com.box.l10n.mojito.cli.command.checks;

public enum CliCheckerType {

    SPELL_CHECKER(SpellCliChecker.class.getName());

    String className;

    CliCheckerType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
