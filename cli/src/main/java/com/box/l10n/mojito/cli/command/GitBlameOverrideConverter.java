package com.box.l10n.mojito.cli.command;

/**
 * @author jaurambault
 */
public class GitBlameOverrideConverter extends EnumConverter<GitBlameCommand.OverrideType> {

  @Override
  protected Class<GitBlameCommand.OverrideType> getGenericClass() {
    return GitBlameCommand.OverrideType.class;
  }
}
