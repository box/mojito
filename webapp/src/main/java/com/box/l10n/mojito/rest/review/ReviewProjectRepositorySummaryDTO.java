package com.box.l10n.mojito.rest.review;

public class ReviewProjectRepositorySummaryDTO {

  private Long id;
  private String name;

  public ReviewProjectRepositorySummaryDTO() {}

  public ReviewProjectRepositorySummaryDTO(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
