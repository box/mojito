package com.box.l10n.mojito.service.elasticsearch;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TextUnitElasticsearchFieldMapper {

  public static List<String> getAllTargetFields() {
    return List.of(
        "targetAr",
        "targetHy",
        "targetEu",
        "targetBn",
        "targetPtBr",
        "targetBg",
        "targetCa",
        "targetCs",
        "targetDa",
        "targetNl",
        "targetEn",
        "targetEt",
        "targetFi",
        "targetFr",
        "targetGl",
        "targetDe",
        "targetEl",
        "targetHi",
        "targetHu",
        "targetId",
        "targetGa",
        "targetIt",
        "targetLv",
        "targetLt",
        "targetNo",
        "targetFa",
        "targetPt",
        "targetRo",
        "targetRu",
        "targetSr",
        "targetCkb",
        "targetEs",
        "targetSv",
        "targetTr",
        "targetTh",
        "targetJa",
        "targetKo",
        "targetZhHans",
        "targetZhHant",
        "targetOther");
  }

  public static void setTargetForLocale(
      TextUnitElasticsearch textUnitElasticsearch, String bcp47Tag, String target) {
    Objects.requireNonNull(bcp47Tag);
    Objects.requireNonNull(target);

    textUnitElasticsearch.setTargetLocale(bcp47Tag);

    Locale locale = Locale.forLanguageTag(bcp47Tag);

    switch (locale.getLanguage()) {
      case "ar" -> textUnitElasticsearch.targetAr = target;
      case "hy" -> textUnitElasticsearch.targetHy = target;
      case "eu" -> textUnitElasticsearch.targetEu = target;
      case "bn" -> textUnitElasticsearch.targetBn = target;
      case "pt" -> {
        switch (locale.getCountry().toUpperCase()) {
          case "BR" -> textUnitElasticsearch.targetPtBr = target;
          default -> textUnitElasticsearch.targetPt = target;
        }
      }
      case "bg" -> textUnitElasticsearch.targetBg = target;
      case "ca" -> textUnitElasticsearch.targetCa = target;
      case "zh" -> {
        switch (locale.getScript().toLowerCase()) {
          case "hant" -> textUnitElasticsearch.targetZhHant = target;
          default -> textUnitElasticsearch.targetZhHans = target;
        }
      }
      case "cs" -> textUnitElasticsearch.targetCs = target;
      case "da" -> textUnitElasticsearch.targetDa = target;
      case "nl" -> textUnitElasticsearch.targetNl = target;
      case "en" -> textUnitElasticsearch.targetEn = target;
      case "et" -> textUnitElasticsearch.targetEt = target;
      case "fi" -> textUnitElasticsearch.targetFi = target;
      case "fr" -> textUnitElasticsearch.targetFr = target;
      case "gl" -> textUnitElasticsearch.targetGl = target;
      case "de" -> textUnitElasticsearch.targetDe = target;
      case "el" -> textUnitElasticsearch.targetEl = target;
      case "hi" -> textUnitElasticsearch.targetHi = target;
      case "hu" -> textUnitElasticsearch.targetHu = target;
      case "id" -> textUnitElasticsearch.targetId = target;
      case "ga" -> textUnitElasticsearch.targetGa = target;
      case "it" -> textUnitElasticsearch.targetIt = target;
      case "lv" -> textUnitElasticsearch.targetLv = target;
      case "lt" -> textUnitElasticsearch.targetLt = target;
      case "no" -> textUnitElasticsearch.targetNo = target;
      case "fa" -> textUnitElasticsearch.targetFa = target;
      case "ro" -> textUnitElasticsearch.targetRo = target;
      case "ru" -> textUnitElasticsearch.targetRu = target;
      case "sr" -> textUnitElasticsearch.targetSr = target;
      case "ckb", "ku" -> textUnitElasticsearch.targetCkb = target;
      case "es" -> textUnitElasticsearch.targetEs = target;
      case "sv" -> textUnitElasticsearch.targetSv = target;
      case "tr" -> textUnitElasticsearch.targetTr = target;
      case "th" -> textUnitElasticsearch.targetTh = target;
      case "ja" -> textUnitElasticsearch.targetJa = target;
      case "ko" -> textUnitElasticsearch.targetKo = target;
      default -> textUnitElasticsearch.targetOther = target;
    }
  }

  public static String getFieldNameForSearch(String bcp47Tag) {
    Objects.requireNonNull(bcp47Tag);

    Locale locale = Locale.forLanguageTag(bcp47Tag);

    return switch (locale.getLanguage()) {
      case "ar" -> "targetAr";
      case "hy" -> "targetHy";
      case "eu" -> "targetEu";
      case "bn" -> "targetBn";
      case "pt" ->
          switch (locale.getCountry().toUpperCase()) {
            case "BR" -> "targetPtBr";
            default -> "targetPt";
          };
      case "bg" -> "targetBg";
      case "ca" -> "targetCa";
      case "zh" ->
          switch (locale.getScript().toLowerCase()) {
            case "hant" -> "targetZhHant";
            default -> "targetZhHans";
          };
      case "cs" -> "targetCs";
      case "da" -> "targetDa";
      case "nl" -> "targetNl";
      case "en" -> "targetEn";
      case "et" -> "targetEt";
      case "fi" -> "targetFi";
      case "fr" -> "targetFr";
      case "gl" -> "targetGl";
      case "de" -> "targetDe";
      case "el" -> "targetEl";
      case "hi" -> "targetHi";
      case "hu" -> "targetHu";
      case "id" -> "targetId";
      case "ga" -> "targetGa";
      case "it" -> "targetIt";
      case "lv" -> "targetLv";
      case "lt" -> "targetLt";
      case "no" -> "targetNo";
      case "fa" -> "targetFa";
      case "ro" -> "targetRo";
      case "ru" -> "targetRu";
      case "sr" -> "targetSr";
      case "ckb", "ku" -> "targetCkb";
      case "es" -> "targetEs";
      case "sv" -> "targetSv";
      case "tr" -> "targetTr";
      case "th" -> "targetTh";
      case "ja" -> "targetJa";
      case "ko" -> "targetKo";
      default -> "targetOther";
    };
  }

  public static String getTargetForLocale(
      TextUnitElasticsearch textUnitElasticsearch, String locale) {
    return switch (getFieldNameForSearch(locale)) {
      case "targetAr" -> textUnitElasticsearch.targetAr;
      case "targetHy" -> textUnitElasticsearch.targetHy;
      case "targetEu" -> textUnitElasticsearch.targetEu;
      case "targetBn" -> textUnitElasticsearch.targetBn;
      case "targetPtBr" -> textUnitElasticsearch.targetPtBr;
      case "targetBg" -> textUnitElasticsearch.targetBg;
      case "targetCa" -> textUnitElasticsearch.targetCa;
      case "targetCjk" -> textUnitElasticsearch.targetCjk;
      case "targetCs" -> textUnitElasticsearch.targetCs;
      case "targetDa" -> textUnitElasticsearch.targetDa;
      case "targetNl" -> textUnitElasticsearch.targetNl;
      case "targetEn" -> textUnitElasticsearch.targetEn;
      case "targetEt" -> textUnitElasticsearch.targetEt;
      case "targetFi" -> textUnitElasticsearch.targetFi;
      case "targetFr" -> textUnitElasticsearch.targetFr;
      case "targetGl" -> textUnitElasticsearch.targetGl;
      case "targetDe" -> textUnitElasticsearch.targetDe;
      case "targetEl" -> textUnitElasticsearch.targetEl;
      case "targetHi" -> textUnitElasticsearch.targetHi;
      case "targetHu" -> textUnitElasticsearch.targetHu;
      case "targetId" -> textUnitElasticsearch.targetId;
      case "targetGa" -> textUnitElasticsearch.targetGa;
      case "targetIt" -> textUnitElasticsearch.targetIt;
      case "targetLv" -> textUnitElasticsearch.targetLv;
      case "targetLt" -> textUnitElasticsearch.targetLt;
      case "targetNo" -> textUnitElasticsearch.targetNo;
      case "targetFa" -> textUnitElasticsearch.targetFa;
      case "targetPt" -> textUnitElasticsearch.targetPt;
      case "targetRo" -> textUnitElasticsearch.targetRo;
      case "targetRu" -> textUnitElasticsearch.targetRu;
      case "targetSr" -> textUnitElasticsearch.targetSr;
      case "targetCkb" -> textUnitElasticsearch.targetCkb;
      case "targetEs" -> textUnitElasticsearch.targetEs;
      case "targetSv" -> textUnitElasticsearch.targetSv;
      case "targetTr" -> textUnitElasticsearch.targetTr;
      case "targetTh" -> textUnitElasticsearch.targetTh;
      case "targetJa" -> textUnitElasticsearch.targetJa;
      case "targetKo" -> textUnitElasticsearch.targetKo;
      case "targetZhHans" -> textUnitElasticsearch.targetZhHans;
      case "targetZhHant" -> textUnitElasticsearch.targetZhHant;
      default -> textUnitElasticsearch.targetOther;
    };
  }
}
