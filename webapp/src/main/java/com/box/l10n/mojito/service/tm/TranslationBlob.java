package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;

import java.util.ArrayList;
import java.util.List;

public class TranslationBlob {

    List<TextUnitDTO> textUnitDTOs = new ArrayList<>();

    public List<TextUnitDTO> getTextUnitDTOs() {
        return textUnitDTOs;
    }

    public void setTextUnitDTOs(List<TextUnitDTO> textUnitDTOs) {
        this.textUnitDTOs = textUnitDTOs;
    }
}
