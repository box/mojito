package com.box.l10n.mojito.rest.review;

public class ReviewProjectLocaleSummaryDTO {

  private Long id;
  private String bcp47Tag;
  private String displayName;
  private int selectedCount;
  private long acceptedCount;

  public ReviewProjectLocaleSummaryDTO() {}

  public ReviewProjectLocaleSummaryDTO(
      Long id, String bcp47Tag, String displayName, int selectedCount, long acceptedCount) {
    this.id = id;
    this.bcp47Tag = bcp47Tag;
    this.displayName = displayName;
    this.selectedCount = selectedCount;
    this.acceptedCount = acceptedCount;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getBcp47Tag() {
    return bcp47Tag;
  }

  public void setBcp47Tag(String bcp47Tag) {
    this.bcp47Tag = bcp47Tag;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public int getSelectedCount() {
    return selectedCount;
  }

  public void setSelectedCount(int selectedCount) {
    this.selectedCount = selectedCount;
  }

  public long getAcceptedCount() {
    return acceptedCount;
  }

  public void setAcceptedCount(long acceptedCount) {
    this.acceptedCount = acceptedCount;
  }
}
