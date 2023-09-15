package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.idgenerator.ContextAwareCountedMd5IdGenerator;
import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.html.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends Okapi filter to support text unit name generation using near stable ids.
 *
 * <p>HTML support is in "alpha" the implementation can change at any moment without keeping
 * backward compatibility. The code is minimal to support non-well formed documents.
 */
public class HtmlFilter extends net.sf.okapi.filters.html.HtmlFilter {

  static Logger logger = LoggerFactory.getLogger(HtmlFilter.class);

  public static final String FILTER_CONFIG_ID = "okf_html@mojito";
  public static final String FILTER_CONFIG_ID_ALPHA = FILTER_CONFIG_ID + "-alpha";

  ContextAwareCountedMd5IdGenerator contextAwareCountedMd5IdGenerator;

  boolean processImageUrls;

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

  @Override
  public List<FilterConfiguration> getConfigurations() {
    List<FilterConfiguration> list = new ArrayList<FilterConfiguration>();

    // for now, we register only one configuration named "Alpha". It uses a specific namegenerator.
    // Different configuration can use different strategies later but for now keep the code minimal.
    list.add(
        new FilterConfiguration(
            FILTER_CONFIG_ID_ALPHA,
            MimeTypeMapper.HTML_MIME_TYPE,
            this.getClass().getName(),
            "HTML Alpha",
            "Configuration for HTML in alpha version",
            null,
            null,
            null));

    return list;
  }

  @Override
  public void open(RawDocument input) {
    super.open(input);
    input.setAnnotation(new ConvertToHtmlCodesAnnotation());
    contextAwareCountedMd5IdGenerator = new ContextAwareCountedMd5IdGenerator();
    applyFilterOptions(input);
  }

  @Override
  public Event next() {
    Event next = super.next();

    if (next.isTextUnit()) {
      setTextUnitName(next.getTextUnit());
    } else if (next.isDocumentPart()) {
      processDocumentPart(next.getDocumentPart());
    }

    return next;
  }

  void processDocumentPart(DocumentPart documentPart) {
    Property srcProperty = documentPart.getSourceProperty("src");
    if (processImageUrls && srcProperty != null) {
      DocumentPartPropertyAnnotation documentPartPropertyAnnotation =
          new DocumentPartPropertyAnnotation();
      documentPartPropertyAnnotation.setPropertyKey(srcProperty.getName());
      documentPartPropertyAnnotation.setName(
          contextAwareCountedMd5IdGenerator.nextId(srcProperty.getValue()));
      documentPartPropertyAnnotation.setSource(srcProperty.getValue());
      documentPartPropertyAnnotation.setComment(
          "Do not translate: extracted image URL, adapt if needed");
      documentPart.setAnnotation(documentPartPropertyAnnotation);
    }
  }

  /**
   * Generates a near stable id and set it as text unit name.
   *
   * <p>The Okapi HTML filter does not set a name on the text unit. Downstream extraction step would
   * then use the text unit id, but it is not stable and would lead to re-creating brandnew text
   * unit set for small variation of the asset.
   */
  void setTextUnitName(ITextUnit textUnit) {
    // Segmentation is not supported, read only the first content
    assert textUnit.getSource().getSegments().count() == 1;
    String sourceAsText = textUnit.getSource().getFirstContent().toText();
    textUnit.setName(contextAwareCountedMd5IdGenerator.nextId(sourceAsText));
  }

  void applyFilterOptions(RawDocument input) {
    FilterOptions filterOptions = input.getAnnotation(FilterOptions.class);
    Parameters parameters = this.getParameters();
    logger.debug("Override with filter options");
    if (filterOptions != null) {
      // mojito options
      filterOptions.getBoolean("processImageUrls", b -> processImageUrls = b);
    }
  }
}
