package com.box.l10n.mojito.service.delta.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

/**
 * Metadata for the Delta response.
 *
 * @author garion
 */
public class DeltaMetadataDTO {

    /**
     * This records the start date from which point we would have captured
     * deltas from, when deltas where created based on a date range.
     */
    @JsonProperty("fromDate")
    public DateTime fromDate;

    /**
     * This records the end date up to which point we would have captured
     * deltas to, when deltas where created based on a date range.
     */
    @JsonProperty("toDate")
    public DateTime toDate;

    public DateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(DateTime fromDate) {
        this.fromDate = fromDate;
    }

    public DateTime getToDate() {
        return toDate;
    }

    public void setToDate(DateTime toDate) {
        this.toDate = toDate;
    }
}
