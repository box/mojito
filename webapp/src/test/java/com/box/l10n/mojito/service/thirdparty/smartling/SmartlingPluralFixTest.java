package com.box.l10n.mojito.service.thirdparty.smartling;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingPluralFix.fixTextUnits;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SmartlingPluralFixTest {

    @Test
    public void testFixTextUnits() {
        List<TextUnitDTO> input = ImmutableList.of();
        assertThat(fixTextUnits(input)).isEmpty();

        input = ImmutableList.of(
                textUnit("singular1", "textContent", null, null),
                textUnit("singular2", "textContent", null, null));

        assertThat(fixTextUnits(input)).containsExactlyElementsOf(input);

        input = ImmutableList.of(
                textUnit("singular1", "textContent", null, null),
                textUnit("singular2", "textContent", null, null),
                textUnit("plural1_zero", "textContent", "zero", "plural1_other"),
                textUnit("plural1_other", "textContent", "other", "plural1_other"));

        assertThat(fixTextUnits(input)).extracting("name", "target", "pluralForm", "pluralFormOther")
                .containsExactlyInAnyOrder(
                        tuple("singular1", "textContent", null, null),
                        tuple("singular2", "textContent", null, null),
                        tuple("plural1_zero", "textContent", "zero", "plural1_other"),
                        tuple("plural1_many", "textContent", "many", "plural1_other"),
                        tuple("plural1_other", "textContent", "other", "plural1_other"));

        input = ImmutableList.of(
                textUnit("plural1_zero", "textContent", "zero", "plural1_other"),
                textUnit("plural1_other", "textContent", "other", "plural1_other"),
                textUnit("plural2_zero", "textContent", "zero", null),
                textUnit("plural2_one", "textContent", "one", null));

        assertThat(fixTextUnits(input)).extracting("name", "target", "pluralForm", "pluralFormOther")
                .containsExactlyInAnyOrder(
                        tuple("plural1_zero", "textContent", "zero", "plural1_other"),
                        tuple("plural1_many", "textContent", "many", "plural1_other"),
                        tuple("plural1_other", "textContent", "other", "plural1_other"),
                        tuple("plural2_zero", "textContent", "zero", null),
                        tuple("plural2_one", "textContent", "one", null));

        input = ImmutableList.of(
                textUnit("plural1_zero", "textContent", "zero", "plural1_other"),
                textUnit("plural1_other", "textContent", "other", "plural1_other"),
                textUnit("plural2_zero", "textContent", "zero", "plural2_other"),
                textUnit("plural2_one", "textContent", "one", "plural2_other"),
                textUnit("plural2_other", "textContent", "other", "plural2_other"));

        assertThat(fixTextUnits(input)).extracting("name", "target", "pluralForm", "pluralFormOther")
                .containsExactlyInAnyOrder(
                        tuple("plural1_zero", "textContent", "zero", "plural1_other"),
                        tuple("plural1_other", "textContent", "other", "plural1_other"),
                        tuple("plural1_many", "textContent", "many", "plural1_other"),
                        tuple("plural2_zero", "textContent", "zero", "plural2_other"),
                        tuple("plural2_one", "textContent", "one", "plural2_other"),
                        tuple("plural2_many", "textContent", "many", "plural2_other"),
                        tuple("plural2_other", "textContent", "other", "plural2_other"));
    }

    private TextUnitDTO textUnit(String name, String content, String pluralForm, String pluralFormOther){

        TextUnitDTO textUnit = new TextUnitDTO();
        textUnit.setName(name);
        textUnit.setTarget(content);
        textUnit.setPluralForm(pluralForm);
        textUnit.setPluralFormOther(pluralFormOther);

        return textUnit;
    }

}
