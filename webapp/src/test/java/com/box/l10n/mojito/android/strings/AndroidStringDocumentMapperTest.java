package com.box.l10n.mojito.android.strings;

import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.FEW;
import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.MANY;
import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.ONE;
import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.OTHER;
import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.TWO;
import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.ZERO;
import static com.box.l10n.mojito.android.strings.AndroidStringDocumentMapper.removeInvalidControlCharacter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.io.Resources;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.Test;

public class AndroidStringDocumentMapperTest {

  String assetDelimiter = "#@#";
  AndroidStringDocumentMapper mapper;
  AndroidStringDocument document;
  List<TextUnitDTO> textUnits = new ArrayList<>();

  @Test
  public void testReadFromSourceTextUnitsForEmptyList() {
    mapper = new AndroidStringDocumentMapper("", assetDelimiter);

    document = mapper.readFromSourceTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getSingulars()).isEmpty();
    assertThat(document.getPlurals()).isEmpty();
  }

  @Test
  public void testReadFromSourceTextUnitsWithoutPluralForms() {
    mapper = new AndroidStringDocumentMapper("_", assetDelimiter);
    textUnits.add(sourceTextUnitDTO(123L, "name0", "content0", "comment0", "my/path0", null, null));
    textUnits.add(sourceTextUnitDTO(124L, "name1", "content1", "comment1", "my/path1", null, null));

    document = mapper.readFromSourceTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getPlurals()).isEmpty();
    assertThat(document.getSingulars()).hasSize(2);
    AndroidSingular singular1 = document.getSingulars().get(0);
    assertThat(singular1.getId()).isEqualTo(123L);
    assertThat(singular1.getName()).isEqualTo("my/path0#@#name0");
    assertThat(singular1.getContent()).isEqualTo("content0");
    assertThat(singular1.getComment()).isEqualTo("comment0");
    AndroidSingular singular2 = document.getSingulars().get(1);
    assertThat(singular2.getId()).isEqualTo(124L);
    assertThat(singular2.getName()).isEqualTo("my/path1#@#name1");
    assertThat(singular2.getContent()).isEqualTo("content1");
    assertThat(singular2.getComment()).isEqualTo("comment1");
  }

  @Test
  public void testReadFromSourceTextUnitsWithoutPluralFormsAndWithTmTextUnitIdInName() {
    mapper = new AndroidStringDocumentMapper("_", assetDelimiter, null, null, true);
    textUnits.add(sourceTextUnitDTO(123L, "name0", "content0", "comment0", "my/path0", null, null));
    textUnits.add(sourceTextUnitDTO(124L, "name1", "content1", "comment1", "my/path1", null, null));

    document = mapper.readFromSourceTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getPlurals()).isEmpty();
    assertThat(document.getSingulars()).hasSize(2);
    AndroidSingular singular1 = document.getSingulars().get(0);
    assertThat(singular1.getId()).isEqualTo(123L);
    assertThat(singular1.getName()).isEqualTo("123#@#my/path0#@#name0");
    assertThat(singular1.getContent()).isEqualTo("content0");
    assertThat(singular1.getComment()).isEqualTo("comment0");
    AndroidSingular singular2 = document.getSingulars().get(1);
    assertThat(singular2.getId()).isEqualTo(124L);
    assertThat(singular2.getName()).isEqualTo("124#@#my/path1#@#name1");
    assertThat(singular2.getContent()).isEqualTo("content1");
    assertThat(singular2.getComment()).isEqualTo("comment1");
  }

  @Test
  public void testReadFromSourceTextUnitsWithoutPluralFormsRemovesBadCharacters() {
    mapper = new AndroidStringDocumentMapper("_", assetDelimiter);
    textUnits.add(
        sourceTextUnitDTO(
            124L, "name1", "test" + '\u001D' + "content1", "comment1", "my/path1", null, null));

    document = mapper.readFromSourceTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getPlurals()).isEmpty();
    assertThat(document.getSingulars()).hasSize(1);
    AndroidSingular singular = document.getSingulars().get(0);
    assertThat(singular.getName()).isEqualTo("my/path1#@#name1");
    assertThat(singular.getContent()).isEqualTo("testcontent1");
  }

  @Test
  public void testReadFromSourceTextUnitsWithPlurals() {
    mapper = new AndroidStringDocumentMapper(" _", assetDelimiter);
    textUnits.add(sourceTextUnitDTO(123L, "name0", "content0", "comment0", "my/path0", null, null));

    textUnits.add(
        sourceTextUnitDTO(
            100L, "name1 _other", "content1_zero", "comment1", "my/path1", "zero", "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            101L, "name1 _other", "content1_one", "comment1", "my/path1", "one", "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            102L, "name1 _other", "content1_two", "comment1", "my/path1", "two", "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            103L, "name1 _other", "content1_few", "comment1", "my/path1", "few", "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            104L, "name1 _other", "content1_many", "comment1", "my/path1", "many", "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            105L,
            "name1 _other",
            "content1_other",
            "comment1",
            "my/path1",
            "other",
            "name1_other"));

    document = mapper.readFromSourceTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getSingulars()).hasSize(1);
    AndroidSingular singular = document.getSingulars().get(0);
    assertThat(singular.getId()).isEqualTo(123L);
    assertThat(singular.getName()).isEqualTo("my/path0#@#name0");
    assertThat(singular.getContent()).isEqualTo("content0");
    assertThat(singular.getComment()).isEqualTo("comment0");

    assertThat(document.getPlurals()).hasSize(1);
    AndroidPlural plural = document.getPlurals().get(0);
    assertThat(plural.getName()).isEqualTo("my/path1#@#name1");
    assertThat(plural.getComment()).isEqualTo("comment1");
    assertThat(plural.getItems()).hasSize(6);
    assertThat(plural.getItems().get(ZERO).getId()).isEqualTo(100L);
    assertThat(plural.getItems().get(ZERO).getContent()).isEqualTo("content1_zero");
    assertThat(plural.getItems().get(ONE).getId()).isEqualTo(101L);
    assertThat(plural.getItems().get(ONE).getContent()).isEqualTo("content1_one");
    assertThat(plural.getItems().get(TWO).getId()).isEqualTo(102L);
    assertThat(plural.getItems().get(TWO).getContent()).isEqualTo("content1_two");
    assertThat(plural.getItems().get(FEW).getId()).isEqualTo(103L);
    assertThat(plural.getItems().get(FEW).getContent()).isEqualTo("content1_few");
    assertThat(plural.getItems().get(MANY).getId()).isEqualTo(104L);
    assertThat(plural.getItems().get(MANY).getContent()).isEqualTo("content1_many");
    assertThat(plural.getItems().get(OTHER).getId()).isEqualTo(105L);
    assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("content1_other");
  }

  @Test
  public void testReadFromSourceTextUnitsWithPluralsAndWithTmTextUnitIdInName() {
    mapper = new AndroidStringDocumentMapper(" _", assetDelimiter, null, null, true);
    textUnits.add(sourceTextUnitDTO(123L, "name0", "content0", "comment0", "my/path0", null, null));

    textUnits.add(
        sourceTextUnitDTO(
            100L, "name1 _other", "content1_zero", "comment1", "my/path1", "zero", "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            101L, "name1 _other", "content1_one", "comment1", "my/path1", "one", "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            102L, "name1 _other", "content1_two", "comment1", "my/path1", "two", "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            103L, "name1 _other", "content1_few", "comment1", "my/path1", "few", "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            104L, "name1 _other", "content1_many", "comment1", "my/path1", "many", "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            105L,
            "name1 _other",
            "content1_other",
            "comment1",
            "my/path1",
            "other",
            "name1_other"));

    document = mapper.readFromSourceTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getSingulars()).hasSize(1);
    AndroidSingular singular = document.getSingulars().get(0);
    assertThat(singular.getId()).isEqualTo(123L);
    assertThat(singular.getName()).isEqualTo("123#@#my/path0#@#name0");
    assertThat(singular.getContent()).isEqualTo("content0");
    assertThat(singular.getComment()).isEqualTo("comment0");

    assertThat(document.getPlurals()).hasSize(1);
    AndroidPlural plural = document.getPlurals().get(0);
    assertThat(plural.getName()).isEqualTo("105#@#my/path1#@#name1");
    assertThat(plural.getComment()).isEqualTo("comment1");
    assertThat(plural.getItems()).hasSize(6);
    assertThat(plural.getItems().get(ZERO).getId()).isEqualTo(100L);
    assertThat(plural.getItems().get(ZERO).getContent()).isEqualTo("content1_zero");
    assertThat(plural.getItems().get(ONE).getId()).isEqualTo(101L);
    assertThat(plural.getItems().get(ONE).getContent()).isEqualTo("content1_one");
    assertThat(plural.getItems().get(TWO).getId()).isEqualTo(102L);
    assertThat(plural.getItems().get(TWO).getContent()).isEqualTo("content1_two");
    assertThat(plural.getItems().get(FEW).getId()).isEqualTo(103L);
    assertThat(plural.getItems().get(FEW).getContent()).isEqualTo("content1_few");
    assertThat(plural.getItems().get(MANY).getId()).isEqualTo(104L);
    assertThat(plural.getItems().get(MANY).getContent()).isEqualTo("content1_many");
    assertThat(plural.getItems().get(OTHER).getId()).isEqualTo(105L);
    assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("content1_other");
  }

  @Test
  public void testReadFromSourceTextUnitsWithDuplicatePlurals() {
    mapper = new AndroidStringDocumentMapper(" _", assetDelimiter);
    textUnits.add(sourceTextUnitDTO(123L, "name0", "content0", "comment0", "my/path0", null, null));

    textUnits.add(
        sourceTextUnitDTO(
            100L,
            "name1 _other",
            "content1_zero",
            "comment1",
            "my/path0.xml",
            "zero",
            "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            101L,
            "name1 _other",
            "content1_one",
            "comment1",
            "my/path0.xml",
            "one",
            "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            102L,
            "name1 _other",
            "content1_two",
            "comment1",
            "my/path0.xml",
            "two",
            "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            103L,
            "name1 _other",
            "content1_few",
            "comment1",
            "my/path0.xml",
            "few",
            "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            104L,
            "name1 _other",
            "content1_many",
            "comment1",
            "my/path0.xml",
            "many",
            "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            105L,
            "name1 _other",
            "content1_other",
            "comment1",
            "my/path0.xml",
            "other",
            "name1_other"));

    textUnits.add(
        sourceTextUnitDTO(
            200L,
            "name1 _other",
            "content1_zero",
            "comment1",
            "my/path1.xml",
            "zero",
            "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            201L,
            "name1 _other",
            "content1_one",
            "comment1",
            "my/path1.xml",
            "one",
            "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            202L,
            "name1 _other",
            "content1_two",
            "comment1",
            "my/path1.xml",
            "two",
            "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            203L,
            "name1 _other",
            "content1_few",
            "comment1",
            "my/path1.xml",
            "few",
            "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            204L,
            "name1 _other",
            "content1_many",
            "comment1",
            "my/path1.xml",
            "many",
            "name1_other"));
    textUnits.add(
        sourceTextUnitDTO(
            205L,
            "name1 _other",
            "content1_other",
            "comment1",
            "my/path1.xml",
            "other",
            "name1_other"));

    document = mapper.readFromSourceTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getSingulars()).hasSize(1);
    AndroidSingular singular = document.getSingulars().get(0);
    assertThat(singular.getId()).isEqualTo(123L);
    assertThat(singular.getName()).isEqualTo("my/path0#@#name0");
    assertThat(singular.getContent()).isEqualTo("content0");
    assertThat(singular.getComment()).isEqualTo("comment0");

    assertThat(document.getPlurals()).hasSize(2);
    AndroidPlural plural = document.getPlurals().get(1);
    assertThat(plural.getName()).isEqualTo("my/path0.xml#@#name1");
    assertThat(plural.getComment()).isEqualTo("comment1");
    assertThat(plural.getItems()).hasSize(6);
    assertThat(plural.getItems().get(ZERO).getId()).isEqualTo(100L);
    assertThat(plural.getItems().get(ZERO).getContent()).isEqualTo("content1_zero");
    assertThat(plural.getItems().get(ONE).getId()).isEqualTo(101L);
    assertThat(plural.getItems().get(ONE).getContent()).isEqualTo("content1_one");
    assertThat(plural.getItems().get(TWO).getId()).isEqualTo(102L);
    assertThat(plural.getItems().get(TWO).getContent()).isEqualTo("content1_two");
    assertThat(plural.getItems().get(FEW).getId()).isEqualTo(103L);
    assertThat(plural.getItems().get(FEW).getContent()).isEqualTo("content1_few");
    assertThat(plural.getItems().get(MANY).getId()).isEqualTo(104L);
    assertThat(plural.getItems().get(MANY).getContent()).isEqualTo("content1_many");
    assertThat(plural.getItems().get(OTHER).getId()).isEqualTo(105L);
    assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("content1_other");

    plural = document.getPlurals().get(0);
    assertThat(plural.getName()).isEqualTo("my/path1.xml#@#name1");
    assertThat(plural.getComment()).isEqualTo("comment1");
    assertThat(plural.getItems()).hasSize(6);
    assertThat(plural.getItems().get(ZERO).getId()).isEqualTo(200L);
    assertThat(plural.getItems().get(ZERO).getContent()).isEqualTo("content1_zero");
    assertThat(plural.getItems().get(ONE).getId()).isEqualTo(201L);
    assertThat(plural.getItems().get(ONE).getContent()).isEqualTo("content1_one");
    assertThat(plural.getItems().get(TWO).getId()).isEqualTo(202L);
    assertThat(plural.getItems().get(TWO).getContent()).isEqualTo("content1_two");
    assertThat(plural.getItems().get(FEW).getId()).isEqualTo(203L);
    assertThat(plural.getItems().get(FEW).getContent()).isEqualTo("content1_few");
    assertThat(plural.getItems().get(MANY).getId()).isEqualTo(204L);
    assertThat(plural.getItems().get(MANY).getContent()).isEqualTo("content1_many");
    assertThat(plural.getItems().get(OTHER).getId()).isEqualTo(205L);
    assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("content1_other");
  }

  @Test
  public void testReadFromTargetTextUnitsForEmptyList() {
    mapper = new AndroidStringDocumentMapper("", assetDelimiter);

    document = mapper.readFromTargetTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getSingulars()).isEmpty();
    assertThat(document.getPlurals()).isEmpty();
  }

  @Test
  public void testReadFromTargetTextUnitsWithoutPlural() {
    mapper = new AndroidStringDocumentMapper("_", assetDelimiter);
    textUnits.add(targetTextUnitDTO(123L, "name0", "content0", "comment0", "my/path0", null, null));
    textUnits.add(targetTextUnitDTO(124L, "name1", "content1", "comment1", "my/path1", null, null));

    document = mapper.readFromTargetTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getPlurals()).isEmpty();
    assertThat(document.getSingulars()).hasSize(2);
    AndroidSingular singular1 = document.getSingulars().get(0);
    assertThat(singular1.getId()).isEqualTo(123L);
    assertThat(singular1.getName()).isEqualTo("my/path0#@#name0");
    assertThat(singular1.getContent()).isEqualTo("content0");
    assertThat(singular1.getComment()).isEqualTo("comment0");
    AndroidSingular singular2 = document.getSingulars().get(1);
    assertThat(singular2.getId()).isEqualTo(124L);
    assertThat(singular2.getName()).isEqualTo("my/path1#@#name1");
    assertThat(singular2.getContent()).isEqualTo("content1");
    assertThat(singular2.getComment()).isEqualTo("comment1");
  }

  @Test
  public void testReadFromTargetTextUnitsWithoutPluralsRemovesBadCharacters() {
    mapper = new AndroidStringDocumentMapper("_", assetDelimiter);
    textUnits.add(
        targetTextUnitDTO(
            124L, "name1", "test" + '\u001D' + "content1", "comment1", "my/path1", null, null));

    document = mapper.readFromTargetTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getPlurals()).isEmpty();
    assertThat(document.getSingulars()).hasSize(1);
    AndroidSingular singular = document.getSingulars().get(0);
    assertThat(singular.getName()).isEqualTo("my/path1#@#name1");
    assertThat(singular.getContent()).isEqualTo("testcontent1");
  }

  @Test
  public void testReadFromTargetTextUnitsWithPlurals() {
    mapper = new AndroidStringDocumentMapper(" _", assetDelimiter);
    textUnits.add(targetTextUnitDTO(123L, "name0", "content0", "comment0", "my/path0", null, null));

    textUnits.add(
        targetTextUnitDTO(
            100L, "name1 _other", "content1_zero", "comment1", "my/path1", "zero", "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            101L, "name1 _other", "content1_one", "comment1", "my/path1", "one", "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            102L, "name1 _other", "content1_two", "comment1", "my/path1", "two", "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            103L, "name1 _other", "content1_few", "comment1", "my/path1", "few", "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            104L, "name1 _other", "content1_many", "comment1", "my/path1", "many", "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            105L,
            "name1 _other",
            "content1_other",
            "comment1",
            "my/path1",
            "other",
            "name1_other"));

    document = mapper.readFromTargetTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getSingulars()).hasSize(1);
    AndroidSingular singular = document.getSingulars().get(0);
    assertThat(singular.getId()).isEqualTo(123L);
    assertThat(singular.getName()).isEqualTo("my/path0#@#name0");
    assertThat(singular.getContent()).isEqualTo("content0");
    assertThat(singular.getComment()).isEqualTo("comment0");

    assertThat(document.getPlurals()).hasSize(1);
    AndroidPlural plural = document.getPlurals().get(0);
    assertThat(plural.getName()).isEqualTo("my/path1#@#name1");
    assertThat(plural.getComment()).isEqualTo("comment1");
    assertThat(plural.getItems()).hasSize(6);
    assertThat(plural.getItems().get(ZERO).getId()).isEqualTo(100L);
    assertThat(plural.getItems().get(ZERO).getContent()).isEqualTo("content1_zero");
    assertThat(plural.getItems().get(ONE).getId()).isEqualTo(101L);
    assertThat(plural.getItems().get(ONE).getContent()).isEqualTo("content1_one");
    assertThat(plural.getItems().get(TWO).getId()).isEqualTo(102L);
    assertThat(plural.getItems().get(TWO).getContent()).isEqualTo("content1_two");
    assertThat(plural.getItems().get(FEW).getId()).isEqualTo(103L);
    assertThat(plural.getItems().get(FEW).getContent()).isEqualTo("content1_few");
    assertThat(plural.getItems().get(MANY).getId()).isEqualTo(104L);
    assertThat(plural.getItems().get(MANY).getContent()).isEqualTo("content1_many");
    assertThat(plural.getItems().get(OTHER).getId()).isEqualTo(105L);
    assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("content1_other");
  }

  @Test
  public void testReadFromTargetTextUnitsWithPluralsDuplicated() {
    mapper = new AndroidStringDocumentMapper(" _", assetDelimiter);
    textUnits.add(targetTextUnitDTO(123L, "name0", "content0", "comment0", "my/path0", null, null));

    textUnits.add(
        targetTextUnitDTO(
            100L,
            "name1 _other",
            "content1_zero",
            "comment1",
            "my/path0.xml",
            "zero",
            "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            101L,
            "name1 _other",
            "content1_one",
            "comment1",
            "my/path0.xml",
            "one",
            "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            102L,
            "name1 _other",
            "content1_two",
            "comment1",
            "my/path0.xml",
            "two",
            "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            103L,
            "name1 _other",
            "content1_few",
            "comment1",
            "my/path0.xml",
            "few",
            "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            104L,
            "name1 _other",
            "content1_many",
            "comment1",
            "my/path0.xml",
            "many",
            "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            105L,
            "name1 _other",
            "content1_other",
            "comment1",
            "my/path0.xml",
            "other",
            "name1_other"));

    textUnits.add(
        targetTextUnitDTO(
            200L,
            "name1 _other",
            "content1_zero",
            "comment1",
            "my/path1.xml",
            "zero",
            "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            201L,
            "name1 _other",
            "content1_one",
            "comment1",
            "my/path1.xml",
            "one",
            "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            202L,
            "name1 _other",
            "content1_two",
            "comment1",
            "my/path1.xml",
            "two",
            "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            203L,
            "name1 _other",
            "content1_few",
            "comment1",
            "my/path1.xml",
            "few",
            "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            204L,
            "name1 _other",
            "content1_many",
            "comment1",
            "my/path1.xml",
            "many",
            "name1_other"));
    textUnits.add(
        targetTextUnitDTO(
            205L,
            "name1 _other",
            "content1_other",
            "comment1",
            "my/path1.xml",
            "other",
            "name1_other"));

    document = mapper.readFromTargetTextUnits(textUnits);

    assertThat(document).isNotNull();
    assertThat(document.getSingulars()).hasSize(1);
    AndroidSingular singular = document.getSingulars().get(0);
    assertThat(singular.getId()).isEqualTo(123L);
    assertThat(singular.getName()).isEqualTo("my/path0#@#name0");
    assertThat(singular.getContent()).isEqualTo("content0");
    assertThat(singular.getComment()).isEqualTo("comment0");

    assertThat(document.getPlurals()).hasSize(2);
    AndroidPlural plural = document.getPlurals().get(1);
    assertThat(plural.getName()).isEqualTo("my/path0.xml#@#name1");
    assertThat(plural.getComment()).isEqualTo("comment1");
    assertThat(plural.getItems()).hasSize(6);
    assertThat(plural.getItems().get(ZERO).getId()).isEqualTo(100L);
    assertThat(plural.getItems().get(ZERO).getContent()).isEqualTo("content1_zero");
    assertThat(plural.getItems().get(ONE).getId()).isEqualTo(101L);
    assertThat(plural.getItems().get(ONE).getContent()).isEqualTo("content1_one");
    assertThat(plural.getItems().get(TWO).getId()).isEqualTo(102L);
    assertThat(plural.getItems().get(TWO).getContent()).isEqualTo("content1_two");
    assertThat(plural.getItems().get(FEW).getId()).isEqualTo(103L);
    assertThat(plural.getItems().get(FEW).getContent()).isEqualTo("content1_few");
    assertThat(plural.getItems().get(MANY).getId()).isEqualTo(104L);
    assertThat(plural.getItems().get(MANY).getContent()).isEqualTo("content1_many");
    assertThat(plural.getItems().get(OTHER).getId()).isEqualTo(105L);
    assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("content1_other");

    plural = document.getPlurals().get(0);
    assertThat(plural.getName()).isEqualTo("my/path1.xml#@#name1");
    assertThat(plural.getComment()).isEqualTo("comment1");
    assertThat(plural.getItems()).hasSize(6);
    assertThat(plural.getItems().get(ZERO).getId()).isEqualTo(200L);
    assertThat(plural.getItems().get(ZERO).getContent()).isEqualTo("content1_zero");
    assertThat(plural.getItems().get(ONE).getId()).isEqualTo(201L);
    assertThat(plural.getItems().get(ONE).getContent()).isEqualTo("content1_one");
    assertThat(plural.getItems().get(TWO).getId()).isEqualTo(202L);
    assertThat(plural.getItems().get(TWO).getContent()).isEqualTo("content1_two");
    assertThat(plural.getItems().get(FEW).getId()).isEqualTo(203L);
    assertThat(plural.getItems().get(FEW).getContent()).isEqualTo("content1_few");
    assertThat(plural.getItems().get(MANY).getId()).isEqualTo(204L);
    assertThat(plural.getItems().get(MANY).getContent()).isEqualTo("content1_many");
    assertThat(plural.getItems().get(OTHER).getId()).isEqualTo(205L);
    assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("content1_other");
  }

  @Test
  public void testMapToTextUnitsFromEmptyDocument() {
    mapper = new AndroidStringDocumentMapper("", assetDelimiter);
    document = new AndroidStringDocument();

    textUnits = mapper.mapToTextUnits(document);

    assertThat(textUnits).isEmpty();
  }

  @Test
  public void testMapToTextUnitsFromSingularsDocument() {
    mapper = new AndroidStringDocumentMapper("", assetDelimiter);

    document = new AndroidStringDocument();
    document.addSingular(new AndroidSingular(10L, "string/path1#@#name1", "content1", "comment1"));
    document.addSingular(new AndroidSingular(11L, "string/path2#@#name2", "content2", "comment2"));
    textUnits = mapper.mapToTextUnits(document);

    assertThat(textUnits).isNotEmpty();
    assertThat(textUnits).hasSize(2);
    assertThat(textUnits)
        .extracting("tmTextUnitId", "name", "assetPath", "target")
        .contains(Tuple.tuple(10L, "name1", "string/path1", "content1"))
        .contains(Tuple.tuple(11L, "name2", "string/path2", "content2"));
  }

  @Test
  public void testMapToTextUnitsFromPluralsDocument() {
    String locale = "pt-BR";
    String repo = "brazil";
    mapper = new AndroidStringDocumentMapper("_", assetDelimiter, locale, repo);

    document = new AndroidStringDocument();
    AndroidPlural.AndroidPluralBuilder builder1 = AndroidPlural.builder();
    builder1.setComment("comment1");
    builder1.setName("string/path1#@#name1");
    builder1.addItem(new AndroidPluralItem(10L, ONE, "one1_content"));
    builder1.addItem(new AndroidPluralItem(11L, MANY, "many1_content"));
    document.addPlural(builder1.build());

    AndroidPlural.AndroidPluralBuilder builder2 = AndroidPlural.builder();
    builder2.setComment("comment2");
    builder2.setName("string/path2#@#name2");
    builder2.addItem(new AndroidPluralItem(21L, ONE, "one2_content"));
    builder2.addItem(new AndroidPluralItem(22L, TWO, "two2_content"));
    builder2.addItem(new AndroidPluralItem(23L, MANY, "many2_content"));
    document.addPlural(builder2.build());

    AndroidPlural.AndroidPluralBuilder builder3 = AndroidPlural.builder();
    builder3.setComment("comment3");
    builder3.setName("string/path3#@#name3");
    builder3.addItem(new AndroidPluralItem(30L, ZERO, "zero3_content"));
    builder3.addItem(new AndroidPluralItem(31L, ONE, "one3_content"));
    builder3.addItem(new AndroidPluralItem(32L, TWO, "two3_content"));
    builder3.addItem(new AndroidPluralItem(33L, FEW, "few3_content"));
    builder3.addItem(new AndroidPluralItem(34L, MANY, "many3_content"));
    builder3.addItem(new AndroidPluralItem(35L, OTHER, "other3_content"));
    document.addPlural(builder3.build());

    textUnits = mapper.mapToTextUnits(document);

    assertThat(textUnits).hasSize(11);
    assertThat(textUnits)
        .extracting(
            "tmTextUnitId",
            "name",
            "pluralForm",
            "assetPath",
            "target",
            "targetLocale",
            "repositoryName")
        .contains(tuple(10L, "name1_one", "one", "string/path1", "one1_content", locale, repo))
        .contains(tuple(11L, "name1_many", "many", "string/path1", "many1_content", locale, repo))
        .contains(tuple(21L, "name2_one", "one", "string/path2", "one2_content", locale, repo))
        .contains(tuple(22L, "name2_two", "two", "string/path2", "two2_content", locale, repo))
        .contains(tuple(23L, "name2_many", "many", "string/path2", "many2_content", locale, repo))
        .contains(tuple(30L, "name3_zero", "zero", "string/path3", "zero3_content", locale, repo))
        .contains(tuple(31L, "name3_one", "one", "string/path3", "one3_content", locale, repo))
        .contains(tuple(32L, "name3_two", "two", "string/path3", "two3_content", locale, repo))
        .contains(tuple(33L, "name3_few", "few", "string/path3", "few3_content", locale, repo))
        .contains(tuple(34L, "name3_many", "many", "string/path3", "many3_content", locale, repo))
        .contains(
            tuple(35L, "name3_other", "other", "string/path3", "other3_content", locale, repo));
  }

  @Test
  public void testReadingTextUnitsFromFile() throws Exception {
    String path =
        Resources.getResource("com/box/l10n/mojito/android/strings/test_resources_file.xml")
            .getPath();
    mapper = new AndroidStringDocumentMapper("_", assetDelimiter);
    textUnits = mapper.mapToTextUnits(AndroidStringDocumentReader.fromFile(path));

    assertThat(textUnits).isNotEmpty();
    assertThat(textUnits)
        .extracting("assetPath")
        .containsOnly("Mojito/src/main/res/values/strings.xml");

    assertThat(textUnits)
        .filteredOn(tu -> tu.getName().equalsIgnoreCase("some_string"))
        .extracting("name", "target", "comment")
        .containsOnly(tuple("some_string", "Dela...", "Some string"));

    assertThat(textUnits)
        .filteredOn(tu -> tu.getName().equalsIgnoreCase("show_options"))
        .extracting("name", "target", "comment")
        .containsOnly(tuple("show_options", "Mer \" dela", "Options"));

    assertThat(textUnits)
        .filteredOn(tu -> tu.getName().equalsIgnoreCase("without_comment"))
        .extracting("target", "comment")
        .containsOnly(tuple("ei ' kommentteja", null));

    assertThat(textUnits)
        .filteredOn(tu -> tu.getName().equalsIgnoreCase("line_break"))
        .extracting("target", "comment")
        .containsOnly(tuple("salto de \nlinea", null));

    assertThat(textUnits)
        .filteredOn(tu -> tu.getName().startsWith("test_plural_hindi"))
        .extracting("name", "pluralForm", "target")
        .contains(tuple("test_plural_hindi_one", "one", "%1$d @ बहुवचन"))
        .contains(tuple("test_plural_hindi_other", "other", "%1$d @ विलक्षण"));

    assertThat(textUnits)
        .filteredOn(tu -> tu.getName().startsWith("plural_russian"))
        .extracting("name", "pluralForm", "target")
        .contains(tuple("plural_russian_one", "one", "одно слово"))
        .contains(tuple("plural_russian_many", "many", "больше слов"));
  }

  @Test
  public void testRemoveInvalidControlCharacter() {
    assertThat(removeInvalidControlCharacter(null)).isEqualTo(null);
    assertThat(removeInvalidControlCharacter("")).isEqualTo("");
    assertThat(removeInvalidControlCharacter("String")).isEqualTo("String");
    assertThat(removeInvalidControlCharacter("second" + '\u0000' + "String"))
        .isEqualTo("secondString");
    assertThat(removeInvalidControlCharacter("third" + '\u001c' + "String"))
        .isEqualTo("thirdString");
    assertThat(removeInvalidControlCharacter("fourth" + '\u001d' + "String"))
        .isEqualTo("fourthString");
    assertThat(
            removeInvalidControlCharacter(
                "all" + '\u0000' + "Bad" + '\u001c' + "Characters" + '\u001d' + "Removed"))
        .isEqualTo("allBadCharactersRemoved");
    assertThat(removeInvalidControlCharacter("Some control accepted\u0009\n\r"))
        .isEqualTo("Some control accepted\u0009\n\r");
    assertThat(removeInvalidControlCharacter("ありがとう")).isEqualTo("ありがとう");
  }

  @Test
  public void testAddTextUnitDTOAttributesAssetPathAndName() {
    mapper = new AndroidStringDocumentMapper("_", null);
    TextUnitDTO textUnitDTO = new TextUnitDTO();

    textUnitDTO.setName("asset_path#@#name_part1");
    assertThat(mapper.addTextUnitDTOAttributes(textUnitDTO))
        .extracting(TextUnitDTO::getAssetPath, TextUnitDTO::getName)
        .containsExactly("asset_path", "name_part1");

    textUnitDTO.setName("asset_path#@#name_part1#@#name_part2");
    assertThat(mapper.addTextUnitDTOAttributes(textUnitDTO))
        .extracting(TextUnitDTO::getAssetPath, TextUnitDTO::getName)
        .containsExactly("asset_path", "name_part1#@#name_part2");
  }

  @Test
  public void testAddTextUnitDTOAttributesTextUnitIdAndAssetPathAndName() {
    mapper = new AndroidStringDocumentMapper("_", null, null, null, true);
    TextUnitDTO textUnitDTO = new TextUnitDTO();

    textUnitDTO.setName("156151#@#asset_path#@#name_part1");
    assertThat(mapper.addTextUnitDTOAttributes(textUnitDTO))
        .extracting(TextUnitDTO::getTmTextUnitId, TextUnitDTO::getAssetPath, TextUnitDTO::getName)
        .containsExactly(156151L, "asset_path", "name_part1");

    textUnitDTO.setName("156152#@#asset_path#@#name_part1#@#name_part2");
    assertThat(mapper.addTextUnitDTOAttributes(textUnitDTO))
        .extracting(TextUnitDTO::getTmTextUnitId, TextUnitDTO::getAssetPath, TextUnitDTO::getName)
        .containsExactly(156152L, "asset_path", "name_part1#@#name_part2");
  }

  private TextUnitDTO sourceTextUnitDTO(
      Long id,
      String name,
      String content,
      String comment,
      String assetPath,
      String pluralForm,
      String pluralFormOther) {
    return textUnitDTO(id, name, content, comment, assetPath, pluralForm, pluralFormOther, true);
  }

  private TextUnitDTO targetTextUnitDTO(
      Long id,
      String name,
      String content,
      String comment,
      String assetPath,
      String pluralForm,
      String pluralFormOther) {
    return textUnitDTO(id, name, content, comment, assetPath, pluralForm, pluralFormOther, false);
  }

  private TextUnitDTO textUnitDTO(
      Long id,
      String name,
      String content,
      String comment,
      String assetPath,
      String pluralForm,
      String pluralFormOther,
      boolean toSource) {

    TextUnitDTO textUnit = new TextUnitDTO();
    textUnit.setTmTextUnitId(id);
    textUnit.setName(name);
    textUnit.setComment(comment);
    textUnit.setAssetPath(assetPath);
    textUnit.setPluralForm(pluralForm);
    textUnit.setPluralFormOther(pluralFormOther);

    if (toSource) {
      textUnit.setSource(content);
    } else {
      textUnit.setTarget(content);
    }

    return textUnit;
  }
}
