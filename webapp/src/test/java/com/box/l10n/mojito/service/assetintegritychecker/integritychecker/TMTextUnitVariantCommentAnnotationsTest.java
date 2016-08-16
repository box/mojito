package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import java.util.List;
import net.sf.okapi.common.resource.TextContainer;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jaurambault
 */
public class TMTextUnitVariantCommentAnnotationsTest {

    @Test
    public void testAddandGetAnnotations() {

        TextContainer target = new TextContainer();
        TMTextUnitVariantCommentAnnotations tmTextUnitVariantCommentAnnotations = new TMTextUnitVariantCommentAnnotations(target);

        List<TMTextUnitVariantCommentAnnotation> annotations = tmTextUnitVariantCommentAnnotations.getAnnotations();
        Assert.assertEquals(0, annotations.size());

        TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation1 = new TMTextUnitVariantCommentAnnotation();
        TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation2 = new TMTextUnitVariantCommentAnnotation();
        TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation3 = new TMTextUnitVariantCommentAnnotation();

        tmTextUnitVariantCommentAnnotations.addAnnotation(tmTextUnitVariantCommentAnnotation1);
        tmTextUnitVariantCommentAnnotations.addAnnotation(tmTextUnitVariantCommentAnnotation2);
        tmTextUnitVariantCommentAnnotations.addAnnotation(tmTextUnitVariantCommentAnnotation3);

        annotations = tmTextUnitVariantCommentAnnotations.getAnnotations();
        Assert.assertEquals(3, annotations.size());
    }

    @Test
    public void testHasCommentWithErrorSeverity() {

        TextContainer target = new TextContainer();
        TMTextUnitVariantCommentAnnotations tmTextUnitVariantCommentAnnotations = new TMTextUnitVariantCommentAnnotations(target);

        List<TMTextUnitVariantCommentAnnotation> annotations = tmTextUnitVariantCommentAnnotations.getAnnotations();
        Assert.assertEquals(0, annotations.size());

        TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation1 = new TMTextUnitVariantCommentAnnotation();
        tmTextUnitVariantCommentAnnotation1.setSeverity(TMTextUnitVariantComment.Severity.ERROR);
        tmTextUnitVariantCommentAnnotations.addAnnotation(tmTextUnitVariantCommentAnnotation1);

        Assert.assertTrue(tmTextUnitVariantCommentAnnotations.hasCommentWithErrorSeverity());
        Assert.assertFalse(tmTextUnitVariantCommentAnnotations.hasCommentWithWarningSeverity());
    }
    
    @Test
    public void testHasCommentWithWarningSeverity() {

        TextContainer target = new TextContainer();
        TMTextUnitVariantCommentAnnotations tmTextUnitVariantCommentAnnotations = new TMTextUnitVariantCommentAnnotations(target);

        List<TMTextUnitVariantCommentAnnotation> annotations = tmTextUnitVariantCommentAnnotations.getAnnotations();
        Assert.assertEquals(0, annotations.size());

        TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation1 = new TMTextUnitVariantCommentAnnotation();
        tmTextUnitVariantCommentAnnotation1.setSeverity(TMTextUnitVariantComment.Severity.WARNING);
        tmTextUnitVariantCommentAnnotations.addAnnotation(tmTextUnitVariantCommentAnnotation1);

        Assert.assertFalse(tmTextUnitVariantCommentAnnotations.hasCommentWithErrorSeverity());
        Assert.assertTrue(tmTextUnitVariantCommentAnnotations.hasCommentWithWarningSeverity());
    }

}
