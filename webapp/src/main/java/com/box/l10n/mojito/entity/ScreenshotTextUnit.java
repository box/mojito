package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Contains the list of text unit names associated with a screenshot
 *
 * @author jaurambault
 */
@Entity
@Table(
        name = "screenshot_text_unit"
)
public class ScreenshotTextUnit extends BaseEntity {

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "screenshot_id", foreignKey = @ForeignKey(name = "FK__SCREENSHOT_TEXT_UNIT__SCREENSHOT__ID"), nullable = false)
    private Screenshot screenshot;

    @Column(name = "name")
    private String name;

    @Column(name = "source", length = Integer.MAX_VALUE)
    private String source;

    @Column(name = "target", length = Integer.MAX_VALUE)
    private String target;

    @Column(name = "rendered_target", length = Integer.MAX_VALUE)
    private String renderedTarget;

    @Column(name = "number_of_match")
    private Integer numberOfMatch;

    @ManyToOne
    @JoinColumn(name = "tm_text_unit_id", foreignKey = @ForeignKey(name = "FK__SCREENSHOT_TEXT_UNIT__TM_TEXT_UNIT__ID"))
    private TMTextUnit tmTextUnit;

    @ManyToOne
    @JoinColumn(name = "tm_text_unit_variant_id", foreignKey = @ForeignKey(name = "FK__SCREENSHOT_TEXT_UNIT__TM_TEXT_UNIT_VARIANT__ID"))
    private TMTextUnitVariant tmTextUnitVariant;

    public Screenshot getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(Screenshot screenshot) {
        this.screenshot = screenshot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getRenderedTarget() {
        return renderedTarget;
    }

    public void setRenderedTarget(String renderedTarget) {
        this.renderedTarget = renderedTarget;
    }

    public TMTextUnit getTmTextUnit() {
        return tmTextUnit;
    }

    public void setTmTextUnit(TMTextUnit tmTextUnit) {
        this.tmTextUnit = tmTextUnit;
    }

    public TMTextUnitVariant getTmTextUnitVariant() {
        return tmTextUnitVariant;
    }

    public void setTmTextUnitVariant(TMTextUnitVariant tmTextUnitVariant) {
        this.tmTextUnitVariant = tmTextUnitVariant;
    }

    public Integer getNumberOfMatch() {
        return numberOfMatch;
    }

    public void setNumberOfMatch(Integer numberOfMatch) {
        this.numberOfMatch = numberOfMatch;
    }
    
}
