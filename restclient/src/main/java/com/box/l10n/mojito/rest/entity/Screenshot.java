package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = false)
public class Screenshot {

    private String name;

    private Locale locale;

    private String src;

    private Long sequence;
    
    private Long takenDate;

    @JsonProperty("textUnits")
    Set<ScreenshotTextUnit> screenshotTextUnits = new LinkedHashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public Set<ScreenshotTextUnit> getScreenshotTextUnits() {
        return screenshotTextUnits;
    }

    public void setScreenshotTextUnits(Set<ScreenshotTextUnit> screenshotTextUnits) {
        this.screenshotTextUnits = screenshotTextUnits;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public Long getTakenDate() {
        return takenDate;
    }

    public void setTakenDate(Long takenDate) {
        this.takenDate = takenDate;
    }

}
