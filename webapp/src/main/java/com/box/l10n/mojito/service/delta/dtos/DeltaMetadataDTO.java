package com.box.l10n.mojito.service.delta.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;

/**
 * Metadata for the Delta response.
 *
 * @author garion
 */
public class DeltaMetadataDTO {

  /**
   * This records the start date from which point we would have captured deltas from, when deltas
   * where created based on a date range.
   */
  @JsonProperty("fromDate")
  public ZonedDateTime fromDate;

  /**
   * This records the end date up to which point we would have captured deltas to, when deltas where
   * created based on a date range.
   */
  @JsonProperty("toDate")
  public ZonedDateTime toDate;

  public ZonedDateTime getFromDate() {
    return fromDate;
  }

  public void setFromDate(ZonedDateTime fromDate) {
    this.fromDate = fromDate;
  }

  public ZonedDateTime getToDate() {
    return toDate;
  }

  public void setToDate(ZonedDateTime toDate) {
    this.toDate = toDate;
  }
}
