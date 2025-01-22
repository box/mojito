package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.model.RepositoryRepository;

/**
 * @author garion
 */
public class RepositoryStatusChecker {
  public boolean hasStringsForTranslationsForExportableLocales(RepositoryRepository repository) {
    DropExportCommand dropExportCommand = new DropExportCommand();
    return dropExportCommand.shouldCreateDrop(repository);
  }
}
