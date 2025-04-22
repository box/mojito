package com.box.l10n.mojito.service.evolve.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaginationDTO {
  @JsonProperty("current_page")
  private long currentPage;

  @JsonProperty("total_pages")
  private int totalPages;

  public int getTotalPages() {
    return totalPages;
  }

  public long getCurrentPage() {
    return currentPage;
  }

  public void setTotalPages(int totalPages) {
    this.totalPages = totalPages;
  }

  public void setCurrentPage(long currentPage) {
    this.currentPage = currentPage;
  }
}
