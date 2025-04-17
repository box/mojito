package com.box.l10n.mojito.github;

public enum PRLabel {
  TRANSLATIONS_REQUIRED("translations-required"),
  TRANSLATIONS_READY("translations-ready"),
  SKIP_TRANSLATIONS_REQUIRED("skip-translations-required");

  private String labelName;

  PRLabel(String labelName) {
    this.labelName = labelName;
  }

  @Override
  public String toString() {
    return labelName;
  }

  public static void updatePRLabel(
      GithubClient githubClient, String repository, int prNumber, PRLabel label) {
    String oppositeLabel =
        label == TRANSLATIONS_READY
            ? TRANSLATIONS_REQUIRED.toString()
            : TRANSLATIONS_READY.toString();
    if (githubClient.isLabelAppliedToPR(repository, prNumber, oppositeLabel)) {
      githubClient.removeLabelFromPR(repository, prNumber, oppositeLabel);
    }

    boolean hasSkipTranslationsLabel =
        githubClient.isLabelAppliedToPR(
            repository, prNumber, SKIP_TRANSLATIONS_REQUIRED.toString());
    if ((label != TRANSLATIONS_REQUIRED || !hasSkipTranslationsLabel)
        && !githubClient.isLabelAppliedToPR(repository, prNumber, label.toString())) {
      githubClient.addLabelToPR(repository, prNumber, label.toString());
    }
  }
}
