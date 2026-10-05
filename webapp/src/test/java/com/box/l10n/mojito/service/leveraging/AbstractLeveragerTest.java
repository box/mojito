package com.box.l10n.mojito.service.leveraging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.rest.leveraging.CopyTmConfig.PreserveStatusMode;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * @author jaurambault
 */
public class AbstractLeveragerTest {

  private AbstractLeverager getLeveragingImpl() {
    return getLeveragingImpl(true);
  }

  private AbstractLeverager getLeveragingImpl(boolean translationNeededIfUniqueMatch) {
    return getLeveragingImpl(translationNeededIfUniqueMatch, null);
  }

  private AbstractLeverager getLeveragingImpl(
      boolean translationNeededIfUniqueMatch, Boolean uniqueMatchOverride) {

    return new AbstractLeverager() {

      @Override
      public String getType() {
        return "for test";
      }

      @Override
      public List<TextUnitDTO> getLeveragingMatches(
          TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId) {
        throw new UnsupportedOperationException(
            "Not supported yet."); // To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public boolean isTranslationNeededIfUniqueMatch() {
        return translationNeededIfUniqueMatch;
      }

      @Override
      protected boolean resolveUniqueMatch(boolean computedUniqueMatch) {
        return uniqueMatchOverride != null ? uniqueMatchOverride : computedUniqueMatch;
      }
    };
  }

  @Test
  public void testFilterTextUnitDTOWithSameTMTextUnitIdEmpty() {
    List<TextUnitDTO> textUnitDTOs = new ArrayList<>();
    getLeveragingImpl().filterTextUnitDTOWithSameTMTextUnitId(textUnitDTOs);
    assertTrue(textUnitDTOs.isEmpty());
  }

  @Test
  public void testFilterTextUnitDTOWithSameTMTextUnitId() {
    List<TextUnitDTO> textUnitDTOs = new ArrayList<>();

    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setTmTextUnitId(1L);
    textUnitDTOs.add(textUnitDTO);

    TextUnitDTO textUnitDTO2 = new TextUnitDTO();
    textUnitDTO2.setTmTextUnitId(2L);
    textUnitDTOs.add(textUnitDTO2);

    TextUnitDTO textUnitDTO3 = new TextUnitDTO();
    textUnitDTO3.setTmTextUnitId(1L);
    textUnitDTOs.add(textUnitDTO3);

    getLeveragingImpl().filterTextUnitDTOWithSameTMTextUnitId(textUnitDTOs);

    assertEquals(2, textUnitDTOs.size());
    assertEquals(textUnitDTO, textUnitDTOs.get(0));
    assertEquals(textUnitDTO3, textUnitDTOs.get(1));
  }

  @Test
  public void computeTranslationNeededPrecisionDowngradesNameOnlyUniqueMatch() {
    AbstractLeverager leverager = getLeveragingImpl(true);
    assertTrue(leverager.computeTranslationNeeded(PreserveStatusMode.PRECISION, true));
  }

  @Test
  public void computeTranslationNeededUniquePreservesUniqueMatch() {
    AbstractLeverager leverager = getLeveragingImpl(true);
    assertFalse(leverager.computeTranslationNeeded(PreserveStatusMode.UNIQUE, true));
  }

  @Test
  public void computeTranslationNeededUniqueDowngradesAmbiguousMatch() {
    AbstractLeverager leverager = getLeveragingImpl(true);
    assertTrue(leverager.computeTranslationNeeded(PreserveStatusMode.UNIQUE, false));
  }

  @Test
  public void computeTranslationNeededAllAlwaysPreserves() {
    AbstractLeverager leverager = getLeveragingImpl(true);
    assertFalse(leverager.computeTranslationNeeded(PreserveStatusMode.ALL, false));
  }

  @Test
  public void resolveUniqueMatchOverrideUsesSourceLeveragingUniqueness() {
    AbstractLeverager leverager = getLeveragingImpl(true, false);
    assertFalse(leverager.resolveUniqueMatch(true));
    assertTrue(
        leverager.computeTranslationNeeded(
            PreserveStatusMode.UNIQUE, leverager.resolveUniqueMatch(true)));
  }
}
