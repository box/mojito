package com.box.l10n.mojito.service.tm.textunitdtocache;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;

import java.util.List;

public class TextUnitDTOsCacheBlobStorageJson {

    List<TextUnitDTO> textUnitDTOs;

    public List<TextUnitDTO> getTextUnitDTOs() {
        return textUnitDTOs;
    }

    public void setTextUnitDTOs(List<TextUnitDTO> textUnitDTOs) {
        this.textUnitDTOs = textUnitDTOs;
    }
}
