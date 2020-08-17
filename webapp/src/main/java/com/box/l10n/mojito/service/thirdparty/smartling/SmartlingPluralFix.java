package com.box.l10n.mojito.service.thirdparty.smartling;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.OTHER;

public final class SmartlingPluralFix {

    private SmartlingPluralFix() {
        throw new AssertionError("Do not instantiate");
    }

    public static List<TextUnitDTO> fixTextUnits(List<TextUnitDTO> textUnits) {
        Stream<TextUnitDTO> added = textUnits.stream()
                .filter(Objects::nonNull)
                .filter(textUnit -> OTHER.toString().equalsIgnoreCase(textUnit.getPluralForm()))
                .map(textUnit -> {
                    TextUnitDTO tu = new TextUnitDTO();
                    tu.setName(textUnit.getName().replace("_other", "_many"));
                    tu.setPluralForm("many");
                    tu.setPluralFormOther(textUnit.getPluralFormOther());
                    tu.setTarget(textUnit.getTarget());
                    tu.setRepositoryName(textUnit.getRepositoryName());
                    tu.setAssetPath(textUnit.getAssetPath());
                    tu.setTargetLocale(textUnit.getTargetLocale());

                    return tu;
                });

        return Stream.concat(added, textUnits.stream()).collect(Collectors.toList());
    }

}
