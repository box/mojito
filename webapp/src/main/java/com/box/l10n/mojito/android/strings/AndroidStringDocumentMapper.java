package com.box.l10n.mojito.android.strings;

import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.OTHER;

import com.box.l10n.mojito.service.tm.PluralNameParser;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AndroidStringDocumentMapper {

  static Logger logger = LoggerFactory.getLogger(AndroidStringDocumentMapper.class);

  private static final String DEFAULT_ASSET_DELIMITER = "#@#";

  private static final CharMatcher INVALID_CONTROL =
      CharMatcher.anyOf("\t\r\n").negate().and(CharMatcher.javaIsoControl());

  private final String pluralSeparator;
  private final String assetDelimiter;
  private final String locale;
  private final String repositoryName;
  private final PluralNameParser pluralNameParser;
  private final boolean addTextUnitIdInName;

  public AndroidStringDocumentMapper(
      String pluralSeparator,
      String assetDelimiter,
      String locale,
      String repositoryName,
      boolean addTextUnitIdInName) {
    this.pluralSeparator = pluralSeparator;
    this.assetDelimiter =
        Optional.ofNullable(Strings.emptyToNull(assetDelimiter)).orElse(DEFAULT_ASSET_DELIMITER);
    this.locale = locale;
    this.repositoryName = repositoryName;
    this.pluralNameParser = new PluralNameParser();
    this.addTextUnitIdInName = addTextUnitIdInName;
  }

  public AndroidStringDocumentMapper(
      String pluralSeparator, String assetDelimiter, String locale, String repositoryName) {
    this(pluralSeparator, assetDelimiter, locale, repositoryName, false);
  }

  public AndroidStringDocumentMapper(String pluralSeparator, String assetDelimiter) {
    this(pluralSeparator, assetDelimiter, null, null);
  }

  public AndroidStringDocument readFromTextUnits(List<TextUnitDTO> textUnits, boolean useSource) {

    AndroidStringDocument document = new AndroidStringDocument();
    Map<String, AndroidPlural.AndroidPluralBuilder> pluralByOther = new HashMap<>();

    for (TextUnitDTO textUnit : textUnits) {

      if (isSingularTextUnit(textUnit)) {
        document.addSingular(textUnitToAndroidSingular(textUnit, useSource));
      } else {
        pluralByOther.compute(
            getKeyToGroupByPluralOtherAndComment(textUnit),
            (key, builder) -> {
              if (builder == null) {
                builder = AndroidPlural.builder();
              }

              if (OTHER.name().equalsIgnoreCase(textUnit.getPluralForm())) {
                String name = pluralNameParser.getPrefix(textUnit.getName(), pluralSeparator);
                builder.setName(textUnit.getAssetPath() + assetDelimiter + name);
                builder.setComment(textUnit.getComment());
              }

              // TODO builder.build() can fail if multiple text units have the same plural form and
              // form
              // t1=name_one,source,one,name_other t2=name_one,source2,one,name_other
              // t3=name_other,sourcepl,other,name_other
              // nothing prevent this to have this state in the DB and it can happen with the
              // current mac filter
              // and wrong data.
              builder.addItem(
                  new AndroidPluralItem(
                      textUnit.getPluralForm(),
                      textUnit.getTmTextUnitId(),
                      useSource ? textUnit.getSource() : textUnit.getTarget()));

              return builder;
            });
      }
    }

    pluralByOther.forEach(
        (pluralFormOther, builder) -> {
          if (addTextUnitIdInName) {
            String ids =
                builder.getSortedItems().stream()
                    .map(AndroidPluralItem::getId)
                    .map(Objects::toString)
                    .collect(Collectors.joining(","));
            builder.setName(ids + "#@#" + builder.getName());
          }

          document.addPlural(builder.build());
        });

    return document;
  }

  public AndroidStringDocument readFromSourceTextUnits(List<TextUnitDTO> textUnits) {
    return readFromTextUnits(textUnits, true);
  }

  public AndroidStringDocument readFromTargetTextUnits(List<TextUnitDTO> textUnits) {
    return readFromTextUnits(textUnits, false);
  }

  public List<TextUnitDTO> mapToTextUnits(AndroidStringDocument document) {
    return mapToTextUnitStream(document).collect(Collectors.toList());
  }

  public Stream<TextUnitDTO> mapToTextUnitStream(AndroidStringDocument document) {
    return document.getStrings().stream().flatMap(this::stringToTextUnits);
  }

  TextUnitDTO addTextUnitDTOAttributes(TextUnitDTO textUnit) {

    if (!Strings.isNullOrEmpty(locale)) {
      textUnit.setTargetLocale(locale);
    }

    if (!Strings.isNullOrEmpty(repositoryName)) {
      textUnit.setRepositoryName(repositoryName);
    }

    if (textUnit.getName().contains(assetDelimiter)) {

      if (addTextUnitIdInName) {
        String[] nameParts = textUnit.getName().split(assetDelimiter, 3);
        if (nameParts.length > 2) {

          if (textUnit.getPluralForm() == null) {
            textUnit.setTmTextUnitId(Long.valueOf(nameParts[0]));
          } else {
            String idsString = nameParts[0];
            List<Long> ids = Arrays.stream(idsString.split(",")).map(Long::valueOf).toList();
            if (ids.size() != 6) {
              throw new RuntimeException(
                  "There must be 6 ids, check that all the required text units are provided (typically TextUnitSearcherParameters#setPluralFormsFiltered(false)");
            }

            AndroidPluralQuantity androidPluralQuantity =
                AndroidPluralQuantity.valueOf(textUnit.getPluralForm());
            textUnit.setTmTextUnitId(ids.get(androidPluralQuantity.ordinal()));
          }
          textUnit.setAssetPath(nameParts[1]);
          textUnit.setName(nameParts[2]);
        }
      } else {
        String[] nameParts = textUnit.getName().split(assetDelimiter, 2);
        if (nameParts.length > 1) {
          textUnit.setAssetPath(nameParts[0]);
          textUnit.setName(nameParts[1]);
        }
      }
    }

    return textUnit;
  }

  Stream<TextUnitDTO> stringToTextUnits(AbstractAndroidString androidString) {
    Stream<TextUnitDTO> result;

    if (androidString.isSingular()) {
      result = singularToTextUnit((AndroidSingular) androidString);
    } else {
      result = pluralToTextUnits((AndroidPlural) androidString);
    }

    return result;
  }

  Stream<TextUnitDTO> singularToTextUnit(AndroidSingular singular) {
    TextUnitDTO textUnit = new TextUnitDTO();
    textUnit.setName(singular.getName());
    textUnit.setComment(singular.getComment());
    textUnit.setTmTextUnitId(singular.getId());
    textUnit.setTarget(unescape(singular.getContent()));
    addTextUnitDTOAttributes(textUnit);

    return Stream.of(textUnit);
  }

  Stream<TextUnitDTO> pluralToTextUnits(AndroidPlural plural) {
    return plural
        .sortedStream()
        .map(
            item -> {
              TextUnitDTO textUnit = new TextUnitDTO();
              String quantity = item.getQuantity().toString();
              String name =
                  pluralNameParser.toPluralName(plural.getName(), quantity, pluralSeparator);
              String pluralFormOther =
                  pluralNameParser.toPluralName(
                      plural.getName(), OTHER.toString(), pluralSeparator);

              textUnit.setName(name);
              textUnit.setComment(plural.getComment());
              textUnit.setTmTextUnitId(item.getId());
              textUnit.setPluralForm(quantity);
              textUnit.setPluralFormOther(pluralFormOther);
              textUnit.setTarget(unescape(item.getContent()));
              addTextUnitDTOAttributes(textUnit);

              return textUnit;
            });
  }

  boolean isSingularTextUnit(TextUnitDTO textUnit) {
    return Strings.isNullOrEmpty(textUnit.getPluralForm());
  }

  String getKeyToGroupByPluralOtherAndComment(TextUnitDTO textUnit) {
    return textUnit.getAssetPath()
        + DEFAULT_ASSET_DELIMITER
        + textUnit.getPluralFormOther()
        + "_"
        + textUnit.getComment();
  }

  AndroidSingular textUnitToAndroidSingular(TextUnitDTO textUnit, boolean useSource) {

    return new AndroidSingular(
        textUnit.getTmTextUnitId(),
        (addTextUnitIdInName ? textUnit.getTmTextUnitId() + assetDelimiter : "")
            + textUnit.getAssetPath()
            + assetDelimiter
            + textUnit.getName(),
        removeInvalidControlCharacter(
            Strings.nullToEmpty(useSource ? textUnit.getSource() : textUnit.getTarget())),
        removeInvalidControlCharacter(textUnit.getComment()));
  }

  static String removeInvalidControlCharacter(String str) {
    String withoutControlCharacters = null;

    if (str != null) {
      withoutControlCharacters = INVALID_CONTROL.removeFrom(str);
      if (!str.equals(withoutControlCharacters)) {
        logger.warn(
            "Removing invalid control characters. It is likely they shouldn't be present in a first place"
                + " consider cleaning up. String: `"
                + str
                + "`");
      }
    }

    return withoutControlCharacters;
  }

  /** should use {@link com.box.l10n.mojito.okapi.filters.AndroidFilter#unescape(String)} */
  static String unescape(String str) {

    String unescape = str;

    if (StringUtils.startsWith(unescape, "\"") && StringUtils.endsWith(unescape, "\"")) {
      unescape = unescape.substring(1, unescape.length() - 1);
    }

    unescape =
        Strings.nullToEmpty(unescape)
            .replaceAll("\\\\'", "'")
            .replaceAll("\\\\\"", "\"")
            .replaceAll("\\\\@", "@")
            .replaceAll("\\\\n", "\n");

    return unescape;
  }
}
