package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.Repository;

/**
 *
 * @author garion
 */
public class RepositoryStatusChecker {
    public boolean hasStringsForTranslationsForExportableLocales(Repository repository) {
        DropExportCommand dropExportCommand = new DropExportCommand();
        return dropExportCommand.shouldCreateDrop(repository);
    }
}
