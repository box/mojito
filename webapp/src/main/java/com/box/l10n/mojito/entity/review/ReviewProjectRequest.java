package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_project_request")
public class ReviewProjectRequest extends AuditableEntity {

  @Column(name = "request_uuid", length = 36, nullable = false, unique = true)
  private String requestUuid;

  @Column(name = "name", length = 255)
  private String name;

  @Column(name = "payload_json", columnDefinition = "longtext")
  private String payloadJson;

  public String getRequestUuid() {
    return requestUuid;
  }

  public void setRequestUuid(String requestUuid) {
    this.requestUuid = requestUuid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(String payloadJson) {
    this.payloadJson = payloadJson;
  }
}
