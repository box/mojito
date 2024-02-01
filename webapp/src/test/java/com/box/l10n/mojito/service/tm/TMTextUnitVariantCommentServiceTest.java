package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jaurambault
 */
public class TMTextUnitVariantCommentServiceTest extends ServiceTestBase {

  @Autowired TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

  @Autowired TMTextUnitVariantCommentRepository tmTextUnitVariantCommentRepository;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  TMTestData tmTestData;

  @Before
  @Transactional
  public void setup() {
    tmTestData = new TMTestData(testIdWatcher);
  }

  @Test
  public void testAddComment() {

    Long tmTextUnitVariantId = tmTestData.addCurrentTMTextUnitVariant1FrFR.getId();

    List<TMTextUnitVariantComment> initialComments =
        tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(tmTextUnitVariantId);
    Assert.assertEquals(
        "There should be no comment in the TextUnitVariant", 0L, initialComments.size());

    TMTextUnitVariantComment addComment =
        tmTextUnitVariantCommentService.addComment(
            tmTextUnitVariantId,
            TMTextUnitVariantComment.Type.LEVERAGING,
            TMTextUnitVariantComment.Severity.INFO,
            "for test");

    List<TMTextUnitVariantComment> comments =
        tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(tmTextUnitVariantId);
    Assert.assertEquals("There should be 1 comment in the TextUnitVariant", 1L, comments.size());

    Assert.assertEquals(
        "Comment should be the one just created", addComment.getId(), comments.get(0).getId());
    Assert.assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comments.get(0).getType());
    Assert.assertEquals(TMTextUnitVariantComment.Severity.INFO, comments.get(0).getSeverity());
    Assert.assertEquals("for test", comments.get(0).getContent());
  }

  @Test
  public void testCopyComments() {

    Long sourceTmTextUnitVariantId = tmTestData.addCurrentTMTextUnitVariant1FrFR.getId();

    tmTextUnitVariantCommentService.addComment(
        sourceTmTextUnitVariantId,
        TMTextUnitVariantComment.Type.LEVERAGING,
        TMTextUnitVariantComment.Severity.INFO,
        "for test");

    tmTextUnitVariantCommentService.addComment(
        sourceTmTextUnitVariantId,
        TMTextUnitVariantComment.Type.LEVERAGING,
        TMTextUnitVariantComment.Severity.WARNING,
        "for test2");

    Long targetTmTextUnitVariantId = tmTestData.addCurrentTMTextUnitVariant1KoKR.getId();

    tmTextUnitVariantCommentService.copyComments(
        sourceTmTextUnitVariantId, targetTmTextUnitVariantId);

    List<TMTextUnitVariantComment> targetComments =
        tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(sourceTmTextUnitVariantId);
    Assert.assertEquals(
        "There should be 2 copied comments in the TextUnitVariant", 2L, targetComments.size());

    Iterator<TMTextUnitVariantComment> sourceCommentsIt =
        tmTextUnitVariantCommentRepository
            .findAllByTmTextUnitVariant_id(targetTmTextUnitVariantId)
            .iterator();

    for (TMTextUnitVariantComment targetComment : targetComments) {
      TMTextUnitVariantComment sourceComment = sourceCommentsIt.next();

      Assert.assertNotEquals(
          "Ids should be different", sourceComment.getId(), targetComment.getId());
      Assert.assertEquals(sourceComment.getType(), targetComment.getType());
      Assert.assertEquals(sourceComment.getSeverity(), targetComment.getSeverity());
      Assert.assertEquals(sourceComment.getContent(), targetComment.getContent());
    }
  }

  @Test
  public void testEnrichTextUnitDTOWithComments() {

    Long tmTextUnitVariantId = tmTestData.addCurrentTMTextUnitVariant1FrFR.getId();

    tmTextUnitVariantCommentService.addComment(
        tmTextUnitVariantId,
        TMTextUnitVariantComment.Type.LEVERAGING,
        TMTextUnitVariantComment.Severity.INFO,
        "leveraging");

    tmTextUnitVariantCommentService.addComment(
        tmTextUnitVariantId,
        TMTextUnitVariantComment.Type.QUALITY_CHECK,
        TMTextUnitVariantComment.Severity.WARNING,
        "quality check");

    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();
    parameters.setAssetId(tmTestData.asset.getId());
    parameters.setLocaleId(tmTestData.frFR.getId());
    parameters.setTarget(tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent());

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(parameters);
    Assert.assertEquals("There should be only one DTO returned", 1, textUnitDTOs.size());

    List<TextUnitDTOWithComments> textUnitDTOsWithComments =
        tmTextUnitVariantCommentService.enrichTextUnitDTOsWithComments(textUnitDTOs);
    Assert.assertEquals(
        "There should be only one DTO with comments returned", 1, textUnitDTOsWithComments.size());

    List<TMTextUnitVariantComment> variantComments =
        textUnitDTOsWithComments.get(0).getTmTextUnitVariantComments();

    Assert.assertEquals("The DTO should have 2 comments", 2, variantComments.size());
    Assert.assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, variantComments.get(0).getType());
    Assert.assertEquals(
        TMTextUnitVariantComment.Severity.INFO, variantComments.get(0).getSeverity());
    Assert.assertEquals("quality check", variantComments.get(1).getContent());
  }
}
