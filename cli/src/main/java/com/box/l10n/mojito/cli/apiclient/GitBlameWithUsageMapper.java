package com.box.l10n.mojito.cli.apiclient;

import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.cli.model.GitBlame;
import com.box.l10n.mojito.cli.model.GitBlameGitBlameWithUsage;
import com.box.l10n.mojito.cli.model.GitBlameWithUsage;
import com.box.l10n.mojito.cli.model.GitBlameWithUsageGitBlameWithUsage;

public class GitBlameWithUsageMapper {
  public static GitBlame mapToGitBlame(GitBlameGitBlameWithUsage gitBlameWithUsage) {
    GitBlame gitBlame = new GitBlame();
    gitBlame.setAuthorEmail(gitBlameWithUsage.getAuthorEmail());
    gitBlame.setAuthorName(gitBlameWithUsage.getAuthorName());
    gitBlame.setCommitTime(gitBlameWithUsage.getCommitTime());
    gitBlame.setCommitName(gitBlameWithUsage.getCommitName());
    return gitBlame;
  }

  public static GitBlameWithUsage mapToGitBlameWithUsage(
      GitBlameWithUsageGitBlameWithUsage gitBlameWithUsage) {
    GitBlameWithUsage newGitBlameWithUsage = new GitBlameWithUsage();
    newGitBlameWithUsage.setUsages(gitBlameWithUsage.getUsages());
    newGitBlameWithUsage.setTextUnitName(gitBlameWithUsage.getTextUnitName());
    newGitBlameWithUsage.setPluralForm(gitBlameWithUsage.getPluralForm());
    newGitBlameWithUsage.setTmTextUnitId(gitBlameWithUsage.getTmTextUnitId());
    newGitBlameWithUsage.setAssetTextUnitId(gitBlameWithUsage.getAssetTextUnitId());
    newGitBlameWithUsage.setContent(gitBlameWithUsage.getContent());
    newGitBlameWithUsage.setComment(gitBlameWithUsage.getComment());
    newGitBlameWithUsage.setGitBlame(
        ofNullable(gitBlameWithUsage.getGitBlame())
            .map(GitBlameWithUsageMapper::mapToGitBlame)
            .orElse(null));
    return newGitBlameWithUsage;
  }
}
