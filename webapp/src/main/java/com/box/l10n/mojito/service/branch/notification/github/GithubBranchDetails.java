package com.box.l10n.mojito.service.branch.notification.github;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.github.GithubException;
import java.util.Objects;
import org.slf4j.Logger;

public class GithubBranchDetails {

  static Logger logger = getLogger(GithubBranchDetails.class);

  private final String owner;
  private final String repository;
  private final Integer prNumber;

  public GithubBranchDetails(String branchName) {
    String[] details = branchName.split("/");
    if (details.length < 4) {
      String message =
          String.format(
              "Github branch '%s' does not contain all required details in the expected format of 'owner/repository/pull/prNumber'",
              branchName);
      logger.error(message);
      throw new GithubException(message);
    }
    this.owner = details[0];
    this.repository = details[1];
    this.prNumber = Integer.parseInt(details[3]);
  }

  public String getOwner() {
    return owner;
  }

  public String getRepository() {
    return repository;
  }

  public Integer getPrNumber() {
    return prNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GithubBranchDetails that = (GithubBranchDetails) o;
    return Objects.equals(owner, that.owner)
        && Objects.equals(repository, that.repository)
        && Objects.equals(prNumber, that.prNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(owner, repository, prNumber);
  }
}
