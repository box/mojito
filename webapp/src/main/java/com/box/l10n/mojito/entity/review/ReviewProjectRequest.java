package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_project_request")
public class ReviewProjectRequest extends AuditableEntity {

  @Column(name = "name", length = 255)
  private String name;

  @Column(name = "notes", length = Integer.MAX_VALUE)
  private String notes;

  @OneToMany(mappedBy = "reviewProjectRequest", fetch = FetchType.LAZY)
  private List<ReviewProjectRequestScreenshot> screenshots = new ArrayList<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public List<ReviewProjectRequestScreenshot> getScreenshots() {
    return screenshots;
  }

  public void setScreenshots(List<ReviewProjectRequestScreenshot> screenshots) {
    this.screenshots = screenshots;
  }
}
