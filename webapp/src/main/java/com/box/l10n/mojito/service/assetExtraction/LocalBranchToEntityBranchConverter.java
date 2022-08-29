package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.localtm.merger.Branch;
import java.time.Instant;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LocalBranchToEntityBranchConverter {

  public static final String NULL_BRANCH_TEXT_PLACEHOLDER = "$$MOJITO_DEFAULT$$";
  public static final DateTime NULL_BRANCH_DATE_PLACEHODLER =
      new DateTime(Instant.EPOCH.toEpochMilli());

  /** logger */
  static Logger logger = LoggerFactory.getLogger(LocalBranchToEntityBranchConverter.class);

  public String localBranchNameToEntityBranchName(String localBranchName) {
    return NULL_BRANCH_TEXT_PLACEHOLDER.equals(localBranchName) ? null : localBranchName;
  }

  public String entityBranchNameToLocalBranchName(String entityBranchName) {
    return entityBranchName == null ? NULL_BRANCH_TEXT_PLACEHOLDER : entityBranchName;
  }

  public String branchEntityToLocalBranchName(com.box.l10n.mojito.entity.Branch branch) {
    return branch == null
        ? NULL_BRANCH_TEXT_PLACEHOLDER
        : entityBranchNameToLocalBranchName(branch.getName());
  }

  public Branch convertEntityBranchToLocaleBranch(com.box.l10n.mojito.entity.Branch b) {
    Branch.Builder builder = Branch.builder();

    if (b == null) {
      // This currently can be null in test, shouldn't be in real case
      builder.name(NULL_BRANCH_TEXT_PLACEHOLDER).createdAt(NULL_BRANCH_DATE_PLACEHODLER);
    } else {
      String branchName = b.getName() == null ? NULL_BRANCH_TEXT_PLACEHOLDER : b.getName();
      DateTime createdDate =
          b.getCreatedDate() != null ? b.getCreatedDate() : NULL_BRANCH_DATE_PLACEHODLER;
      builder.name(branchName).createdAt(createdDate);
    }

    return builder.build();
  }
}
