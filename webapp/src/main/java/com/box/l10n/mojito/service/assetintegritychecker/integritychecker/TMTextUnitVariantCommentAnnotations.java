package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.resource.TextContainer;

/**
 * Helper class to manipulate {@link TMTextUnitVariantCommentAnnotation} on a {@link TextContainer}.
 *
 * @author jaurambault
 */
public class TMTextUnitVariantCommentAnnotations extends GenericAnnotations {

  TextContainer textContainer;

  /**
   * Creates an instance of the helper that will read/add annotations on the provided {@link
   * TextContainer}.
   *
   * @param textContainer contains the
   */
  public TMTextUnitVariantCommentAnnotations(TextContainer textContainer) {
    this.textContainer = textContainer;
  }

  /**
   * Indicates if the {@link TextContainer} has at least one annotation with an error.
   *
   * @return {@code true} if has at least one error else {@code false}
   */
  public boolean hasCommentWithErrorSeverity() {
    return hasCommentWithSeverity(TMTextUnitVariantComment.Severity.ERROR);
  }

  /**
   * Indicates if the {@link TextContainer} has at least one annotation with a warning.
   *
   * @return {@code true} if has at least one warning else {@code false}
   */
  public boolean hasCommentWithWarningSeverity() {
    return hasCommentWithSeverity(TMTextUnitVariantComment.Severity.WARNING);
  }

  /**
   * Indicates if the {@link TextContainer} has at least one annotation with provided severity.
   *
   * @param severity severity to look for in the annotations
   * @return {@code true} if has at least one annotation with specified severity else {@code false}
   */
  protected boolean hasCommentWithSeverity(TMTextUnitVariantComment.Severity severity) {
    boolean hasCommentWithSeverity = false;

    for (TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation : getAnnotations()) {
      if (severity.equals(tmTextUnitVariantCommentAnnotation.severity)) {
        hasCommentWithSeverity = true;
        break;
      }
    }

    return hasCommentWithSeverity;
  }

  /**
   * Gets the {@link TMTextUnitVariantCommentAnnotation}s present on the {@link TextContainer}.
   *
   * @return the list of annotations
   */
  public List<TMTextUnitVariantCommentAnnotation> getAnnotations() {
    List<TMTextUnitVariantCommentAnnotation> result = new ArrayList<>();

    TMTextUnitVariantCommentAnnotations tmTextUnitVariantCommentAnnotations =
        textContainer.getAnnotation(TMTextUnitVariantCommentAnnotations.class);

    if (tmTextUnitVariantCommentAnnotations != null) {
      for (GenericAnnotation allAnnotation :
          tmTextUnitVariantCommentAnnotations.getAllAnnotations()) {
        if (allAnnotation instanceof TMTextUnitVariantCommentAnnotation) {
          result.add((TMTextUnitVariantCommentAnnotation) allAnnotation);
        }
      }
    }

    return result;
  }

  /**
   * Adds a {@link TMTextUnitVariantCommentAnnotation} on the {@link TextContainer}.
   *
   * @param tmTextUnitVariantCommentAnnotation annotation to be added
   */
  public void addAnnotation(TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation) {

    TMTextUnitVariantCommentAnnotations current =
        textContainer.getAnnotation(TMTextUnitVariantCommentAnnotations.class);

    if (current == null) {
      current = new TMTextUnitVariantCommentAnnotations(textContainer);
      textContainer.setAnnotation(current);
    }

    current.add(tmTextUnitVariantCommentAnnotation);
  }
}
