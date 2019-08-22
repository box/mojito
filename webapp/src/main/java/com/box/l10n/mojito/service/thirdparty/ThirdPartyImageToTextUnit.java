package com.box.l10n.mojito.service.thirdparty;

/**
 * Holds a mapping between and image and a text unit in the third party TMS
 */
public class ThirdPartyImageToTextUnit {
    String textUnitId;
    String imageId;

    public String getTextUnitId() {
        return textUnitId;
    }

    public void setTextUnitId(String textUnitId) {
        this.textUnitId = textUnitId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
}
