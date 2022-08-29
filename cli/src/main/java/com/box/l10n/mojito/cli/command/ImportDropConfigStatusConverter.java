package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.ImportDropConfig;

/** @author jaurambault */
public class ImportDropConfigStatusConverter extends EnumConverter<ImportDropConfig.Status> {

  @Override
  protected Class<ImportDropConfig.Status> getGenericClass() {
    return ImportDropConfig.Status.class;
  }
}
