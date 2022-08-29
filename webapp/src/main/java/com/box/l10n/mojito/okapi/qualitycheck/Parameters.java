package com.box.l10n.mojito.okapi.qualitycheck;

/** @author aloison */
public class Parameters extends net.sf.okapi.lib.verification.Parameters {

  /** Disables all checks done by the Quality Checker */
  public void disableAllChecks() {
    super.reset();

    setLeadingWS(false);
    setTrailingWS(false);
    setEmptyTarget(false);
    setEmptySource(false);
    setTargetSameAsSource(false);
    setTargetSameAsSourceForSameLanguage(false);
    setTargetSameAsSourceWithCodes(false);
    setCodeDifference(false);
    setGuessOpenClose(false);
    setCheckXliffSchema(false);
    setCheckPatterns(false);
    setCheckWithLT(false);
    setTranslateLTMsg(false);
    setLtBilingualMode(false);
    setDoubledWord(false);
    setCorruptedCharacters(false);
    setCheckMaxCharLength(false);
    setCheckMinCharLength(false);
    setCheckStorageSize(false);
    setCheckAbsoluteMaxCharLength(false);
    setCheckAllowedCharacters(false);
    setCheckCharacters(false);
    setCheckTerms(false);
    setStringMode(false);
    setBetweenCodes(false);
    setCheckBlacklist(false);
  }
}
