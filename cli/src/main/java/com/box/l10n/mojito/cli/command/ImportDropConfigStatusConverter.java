package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.model.ImportDropConfig;

/**
 * @author jaurambault
 */
public class ImportDropConfigStatusConverter extends EnumConverter<ImportDropConfig.StatusEnum> {

  @Override
  protected Class<ImportDropConfig.StatusEnum> getGenericClass() {
    return ImportDropConfig.StatusEnum.class;
  }
}
