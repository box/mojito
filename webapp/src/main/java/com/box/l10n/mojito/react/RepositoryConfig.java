package com.box.l10n.mojito.react;

public class RepositoryConfig {

  Location location;
  Commit commit;
  ThirdParty thirdParty;
  PullRequest pullRequest;
  TextUnitNameToTextUnitNameInSource textUnitNameToTextUnitNameInSource;
  CustomMd5 customMd5;

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public Commit getCommit() {
    return commit;
  }

  public void setCommit(Commit commit) {
    this.commit = commit;
  }

  public ThirdParty getThirdParty() {
    return thirdParty;
  }

  public void setThirdParty(ThirdParty thirdParty) {
    this.thirdParty = thirdParty;
  }

  public PullRequest getPullRequest() {
    return pullRequest;
  }

  public void setPullRequest(PullRequest pullRequest) {
    this.pullRequest = pullRequest;
  }

  public TextUnitNameToTextUnitNameInSource getTextUnitNameToTextUnitNameInSource() {
    return textUnitNameToTextUnitNameInSource;
  }

  public void setTextUnitNameToTextUnitNameInSource(
      TextUnitNameToTextUnitNameInSource textUnitNameToTextUnitNameInSource) {
    this.textUnitNameToTextUnitNameInSource = textUnitNameToTextUnitNameInSource;
  }

  public CustomMd5 getCustomMd5() {
    return customMd5;
  }

  public void setCustomMd5(CustomMd5 customMd5) {
    this.customMd5 = customMd5;
  }
}
