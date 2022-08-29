package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import net.sf.okapi.common.annotation.GenericAnnotation;

/**
 * Annotation that contains information
 *
 * @author jaurambault
 */
public class TMTextUnitVariantCommentAnnotation extends GenericAnnotation {

  TMTextUnitVariantComment.Type commentType;
  TMTextUnitVariantComment.Severity severity;
  String message;

  public TMTextUnitVariantCommentAnnotation() {
    super(TMTextUnitVariantComment.class.getName());
  }

  public TMTextUnitVariantComment.Type getCommentType() {
    return commentType;
  }

  public void setCommentType(TMTextUnitVariantComment.Type commentType) {
    this.commentType = commentType;
  }

  public TMTextUnitVariantComment.Severity getSeverity() {
    return severity;
  }

  public void setSeverity(TMTextUnitVariantComment.Severity severity) {
    this.severity = severity;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
