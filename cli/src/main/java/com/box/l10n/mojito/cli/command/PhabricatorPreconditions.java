package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.phabricator.DifferentialRevision;

public class PhabricatorPreconditions {
    public static void checkNotNull(Object phabricatorDependency) {
        if (phabricatorDependency == null) {
            throw new CommandException("Phabricator must be configured with properties: l10n.phabricator.url and l10n.phabricator.token");
        }
    }
}
