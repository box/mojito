package com.box.l10n.mojito.entity.review;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReviewProjectTypeConverter implements AttributeConverter<ReviewProjectType, String> {

  @Override
  public String convertToDatabaseColumn(ReviewProjectType attribute) {
    return attribute != null ? attribute.name() : null;
  }

  @Override
  public ReviewProjectType convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    try {
      return ReviewProjectType.valueOf(dbData);
    } catch (IllegalArgumentException ex) {
      return ReviewProjectType.UNKNOWN;
    }
  }
}
