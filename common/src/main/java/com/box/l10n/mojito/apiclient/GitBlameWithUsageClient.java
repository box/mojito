package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.apiclient.model.GitBlameWithUsage;
import com.box.l10n.mojito.apiclient.model.GitBlameWithUsageGitBlameWithUsage;
import com.box.l10n.mojito.apiclient.model.PollableTask;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GitBlameWithUsageClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(GitBlameWithUsageClient.class);

  @Autowired private TextUnitWsApi textUnitClient;

  public List<GitBlameWithUsageGitBlameWithUsage> getGitBlameWithUsages(
      List<Long> repositoryIds,
      List<String> repositoryNames,
      Long tmTextUnitId,
      String usedFilter,
      String statusFilter,
      Boolean doNotTranslateFilter,
      Integer limit,
      Integer offset) {
    logger.debug("getGitBlameWithUsages");
    return this.textUnitClient.getGitBlameWithUsages(
        repositoryIds,
        repositoryNames,
        tmTextUnitId,
        usedFilter,
        statusFilter,
        doNotTranslateFilter,
        limit,
        offset);
  }

  public PollableTask saveGitBlameWithUsages(List<GitBlameWithUsage> body) {
    logger.debug("saveGitBlameWithUsages");
    return this.textUnitClient.saveGitBlameWithUsages(body);
  }
}
