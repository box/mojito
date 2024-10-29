package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.steps.OutputDocumentPostProcessingAnnotation;
import com.box.l10n.mojito.okapi.steps.OutputDocumentPostProcessingAnnotation.OutputDocumentPostProcessorBase;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author jaurambault
 */
@Configurable
public class AndroidFilter extends XMLFilter {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AndroidFilter.class);

  public static final String ANDROIDSTRINGS_CONFIG_FILE_NAME = "AndroidStrings_mojito.fprm";

  public static final String FILTER_CONFIG_ID = "okf_xml@mojito-AndroidStrings";

  private static final String OPTION_OLD_ESCAPING = "oldEscaping";

  private static final String REMOVE_DESCRIPTION = "removeDescription";

  private static final String POST_PROCESS_INDENT = "postProcessIndent";

  private static final String POST_PROCESS_REMOVE_TRANSLATABLE_FALSE =
      "postRemoveTranslatableFalse";

  private static final Pattern PATTERN_PLURAL_START = Pattern.compile("<plurals");
  private static final Pattern PATTERN_PLURAL_END = Pattern.compile("</plurals>");
  private static final Pattern PATTERN_XML_COMMENT = Pattern.compile("<!--(?<comment>.*?)-->");
  private static final Pattern PATTERN_REPLACE_FORM =
      Pattern.compile("<.*?item.+?quantity.+?\"(.+?)\"");
  private static final Pattern PATTERN_UPDATE_FORM =
      Pattern.compile("(\\s*<.*?item.+?quantity.+?\".+?\">)");

  public static final Pattern FIND_LAST_TRANSLATABLE_FALSE =
      Pattern.compile("(?s).*translatable.*=.*\"false\"");

  private static final String XML_COMMENT_GROUP_NAME = "comment";

  @Autowired TextUnitUtils textUnitUtils;

  @Autowired UnescapeUtils unescapeUtils;

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

  @Override
  public List<FilterConfiguration> getConfigurations() {
    List<FilterConfiguration> list = new ArrayList<>();
    list.add(
        new FilterConfiguration(
            getName(),
            getMimeType(),
            getClass().getName(),
            "Android Strings",
            "Configuration for Android Strings XML documents.",
            ANDROIDSTRINGS_CONFIG_FILE_NAME));

    return list;
  }

  LocaleId targetLocale;

  boolean hasAnnotation;

  /** To set old escaping, can be removed if the oldEscaping option is removed. */
  AndroidXMLEncoder androidXMLEncoder;

  /** Option to enable old escaping for the Android filter. */
  boolean oldEscaping = false;

  List<Event> eventQueue = new ArrayList<>();

  boolean removeDescription = false;

  boolean removeTranslatableFalse = false;

  int postProcessIndent = 2;

  /**
   * Historically there was no processing if no option was passed. We want to keep this behavior to
   * avoid output change.
   */
  boolean shouldApplyPostProcessingRemoveUntranslatedExcluded = false;

  @Override
  public void open(RawDocument input) {
    super.open(input);
    targetLocale = input.getTargetLocale();
    hasAnnotation = input.getAnnotation(CopyFormsOnImport.class) != null;
    applyFilterOptions(input);
    input.setAnnotation(
        new RemoveUntranslatedStategyAnnotation(
            RemoveUntranslatedStrategy.PLACEHOLDER_AND_POST_PROCESSING));
    input.setAnnotation(
        new OutputDocumentPostProcessingAnnotation(
            new AndroidFilePostProcessor(
                false,
                removeDescription,
                postProcessIndent,
                removeTranslatableFalse,
                shouldApplyPostProcessingRemoveUntranslatedExcluded)));
  }

  void applyFilterOptions(RawDocument input) {
    FilterOptions filterOptions = input.getAnnotation(FilterOptions.class);

    if (filterOptions != null) {
      filterOptions.getBoolean(
          OPTION_OLD_ESCAPING,
          b -> {
            oldEscaping = b;
            if (androidXMLEncoder != null) {
              androidXMLEncoder.oldEscaping = oldEscaping;
            }
          });
      logger.debug("filter option, old escaping: {}", oldEscaping);

      filterOptions.getBoolean(
          REMOVE_DESCRIPTION,
          b -> {
            removeDescription = b;
            shouldApplyPostProcessingRemoveUntranslatedExcluded = true;
          });

      filterOptions.getBoolean(
          POST_PROCESS_REMOVE_TRANSLATABLE_FALSE,
          b -> {
            removeTranslatableFalse = b;
            shouldApplyPostProcessingRemoveUntranslatedExcluded = true;
          });

      filterOptions.getInteger(
          POST_PROCESS_INDENT,
          i -> {
            postProcessIndent = i;
            shouldApplyPostProcessingRemoveUntranslatedExcluded = true;
          });
    }
  }

  @Override
  public boolean hasNext() {
    return !eventQueue.isEmpty() || super.hasNext();
  }

  @Override
  public Event next() {
    Event event;

    if (eventQueue.isEmpty()) {
      readNextEvents();
    }

    event = eventQueue.remove(0);

    return event;
  }

  private void processTextUnit(Event event) {
    if (event != null && event.isTextUnit()) {

      TextUnit textUnit = (TextUnit) event.getTextUnit();
      String sourceString = textUnitUtils.getSourceAsString(textUnit);

      String unescapedSourceString;

      if (oldEscaping) {
        unescapedSourceString = unescapeUtils.unescape(sourceString);
      } else {
        unescapedSourceString = unescape(sourceString);
      }

      textUnitUtils.replaceSourceString(textUnit, unescapedSourceString);
      extractNoteFromXMLCommentInSkeletonIfNone(textUnit);
    }
  }

  /**
   * Should cover the main cases mention in doc:
   * https://developer.android.com/guide/topics/resources/string-resource#FormattingAndStyling
   *
   * @param sourceString
   * @return
   */
  String unescape(String sourceString) {
    String unescapedSourceString;

    unescapedSourceString = sourceString.trim();
    unescapedSourceString = unescapeUtils.replaceEscapedUnicode(unescapedSourceString);

    if (StringUtils.startsWith(unescapedSourceString, "\"")
        && StringUtils.endsWith(unescapedSourceString, "\"")) {
      unescapedSourceString =
          unescapedSourceString.substring(1, unescapedSourceString.length() - 1);
    } else {
      unescapedSourceString = unescapeUtils.replaceLineFeedWithSpace(unescapedSourceString);
      unescapedSourceString = unescapeUtils.collapseSpaces(unescapedSourceString).trim();
    }

    unescapedSourceString = unescapeUtils.replaceEscapedLineFeed(unescapedSourceString);
    unescapedSourceString = unescapeUtils.replaceEscapedCarriageReturn(unescapedSourceString);
    unescapedSourceString = unescapeUtils.replaceEscapedCharacters(unescapedSourceString);

    return unescapedSourceString;
  }

  protected boolean isPluralGroupStarting(IResource resource) {
    String toString = resource.getSkeleton().toString();
    Matcher matcher = PATTERN_PLURAL_START.matcher(toString);
    boolean startPlural = matcher.find();
    return startPlural;
  }

  protected boolean isPluralGroupEnding(IResource resource) {
    String toString = resource.getSkeleton().toString();
    Matcher matcher = PATTERN_PLURAL_END.matcher(toString);
    boolean endPlural = matcher.find();
    return endPlural;
  }

  /**
   * Extract the note from XML comments only if there is no note on the text unit. In other words if
   * a note was specify via attribute like description for android it won't be overridden by an
   * comments present in the XML file.
   *
   * @param textUnit the text unit for which comments should be extracted
   */
  protected void extractNoteFromXMLCommentInSkeletonIfNone(TextUnit textUnit) {

    String skeleton = textUnit.getSkeleton().toString();

    if (textUnitUtils.getNote(textUnit) == null) {
      String note = getNoteFromXMLCommentsInSkeleton(skeleton);
      if (note != null) {
        textUnitUtils.setNote(textUnit, note);
      }
    }
  }

  /**
   * Gets the note from the XML comments in the skeleton.
   *
   * @param skeleton that may contains comments
   * @return the note or <code>null</code>
   */
  protected String getNoteFromXMLCommentsInSkeleton(String skeleton) {

    String note = null;

    final Matcher matcherForLastTranslatableFalse = FIND_LAST_TRANSLATABLE_FALSE.matcher(skeleton);
    if (matcherForLastTranslatableFalse.find()) {
      skeleton = skeleton.substring(matcherForLastTranslatableFalse.group(0).length());
    }

    StringBuilder commentBuilder = new StringBuilder();

    Matcher matcher = PATTERN_XML_COMMENT.matcher(skeleton);

    while (matcher.find()) {
      if (commentBuilder.length() > 0) {
        commentBuilder.append(" ");
      }
      commentBuilder.append(matcher.group(XML_COMMENT_GROUP_NAME).trim());
    }

    if (commentBuilder.length() > 0) {
      note = commentBuilder.toString();
    }

    return note;
  }

  @Override
  public AndroidXMLEncoder getXMLEncoder() {
    androidXMLEncoder = new AndroidXMLEncoder(oldEscaping);
    return androidXMLEncoder;
  }

  private void readNextEvents() {
    Event next = getNextWithProcess();

    if (next.isTextUnit() && isPluralGroupStarting(next.getResource())) {
      readPlurals(next);
    } else {
      eventQueue.add(next);
    }
  }

  private Event getNextWithProcess() {
    Event next = super.next();
    processTextUnit(next);
    return next;
  }

  private void readPlurals(Event next) {

    List<Event> pluralEvents = new ArrayList<>();

    // add the start event
    pluralEvents.add(next);

    next = getNextWithProcess();

    // read others until the end
    while (next != null && !isPluralGroupEnding(next.getResource())) {
      pluralEvents.add(next);
      next = getNextWithProcess();
    }

    // that doesn't contain last
    pluralEvents = adaptPlurals(pluralEvents);

    eventQueue.addAll(pluralEvents);

    if (isPluralGroupStarting(next.getResource())) {
      readPlurals(next);
    } else {
      eventQueue.add(next);
    }
  }

  protected List<Event> adaptPlurals(List<Event> pluralEvents) {
    logger.debug("Adapt plural forms if needed");
    PluralsHolder pluralsHolder = new AndroidPluralsHolder();
    pluralsHolder.loadEvents(pluralEvents);
    List<Event> completedForms = pluralsHolder.getCompletedForms(targetLocale);
    return completedForms;
  }

  class AndroidPluralsHolder extends PluralsHolder {

    String firstForm = null;
    String comments = null;

    @Override
    protected void loadEvents(List<Event> pluralEvents) {

      if (!pluralEvents.isEmpty()) {
        Event firstEvent = pluralEvents.get(0);
        firstForm = getPluralFormFromSkeleton(firstEvent.getResource());
        ITextUnit firstTextUnit = firstEvent.getTextUnit();
        comments = textUnitUtils.getNote(firstTextUnit);
      }

      super.loadEvents(pluralEvents);
    }

    @Override
    public List<Event> getCompletedForms(LocaleId localeId) {
      List<Event> completedForms = super.getCompletedForms(localeId);
      swapSkeletonBetweenOldFirstAndNewFirst(
          firstForm, getPluralFormFromSkeleton(completedForms.get(0).getResource()));

      for (Event newForm : completedForms) {
        if (comments != null) {
          textUnitUtils.setNote(newForm.getTextUnit(), comments);
        }
      }

      return completedForms;
    }

    @Override
    void replaceFormInSkeleton(
        GenericSkeleton genericSkeleton, String sourceForm, String targetForm) {
      for (GenericSkeletonPart part : genericSkeleton.getParts()) {
        StringBuilder sb = part.getData();
        // TODO make more flexible
        String str = sb.toString().replace(sourceForm + "\"", targetForm + "\"");
        sb.replace(0, sb.length(), str);
      }
    }

    String getPluralFormFromSkeleton(IResource resource) {
      String toString = resource.getSkeleton().toString();
      Matcher matcher = PATTERN_REPLACE_FORM.matcher(toString);
      String res = null;
      if (matcher.find()) {
        res = matcher.group(1);
      }
      return res;
    }

    void updateFormInSkeleton(ITextUnit textUnit) {
      boolean ignore = true;
      GenericSkeleton genericSkeleton = (GenericSkeleton) textUnit.getSkeleton();
      for (GenericSkeletonPart genericSkeletonPart : genericSkeleton.getParts()) {
        String partString = genericSkeletonPart.toString();
        Matcher matcher = PATTERN_UPDATE_FORM.matcher(partString);
        if (matcher.find()) {
          String match = matcher.group(1);
          genericSkeletonPart.setData(match);
          ignore = false;
        }
        if (ignore) {
          genericSkeletonPart.setData("");
        }
      }
    }
  }

  static class AndroidFilePostProcessor extends OutputDocumentPostProcessorBase {
    static final String DESCRIPTION_ATTRIBUTE = "description";
    boolean removeDescription;
    boolean removeTranslatableFalse;
    int indent;
    boolean shouldApplyPostProcessingRemoveUntranslatedExcluded;

    AndroidFilePostProcessor(
        boolean removeUntranslated,
        boolean removeDescription,
        int indent,
        boolean removeTranslatableFalse,
        boolean shouldApplyPostProcessingRemoveUntranslatedExcluded) {
      this.setRemoveUntranslated(removeUntranslated);
      this.removeDescription = removeDescription;
      this.removeTranslatableFalse = removeTranslatableFalse;
      this.indent = indent;
      this.shouldApplyPostProcessingRemoveUntranslatedExcluded =
          shouldApplyPostProcessingRemoveUntranslatedExcluded;
    }

    public String execute(String xmlContent) {

      if (xmlContent == null
          || xmlContent.isBlank()
          || (!shouldApplyPostProcessingRemoveUntranslatedExcluded && !hasRemoveUntranslated())) {
        return xmlContent;
      }

      try {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(new InputSource(new StringReader(xmlContent)));
        document.getDocumentElement().normalize();

        NodeList stringElements = document.getElementsByTagName("string");
        for (int i = 0; i < stringElements.getLength(); i++) {
          Node node = stringElements.item(i);
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            if (hasRemoveUntranslated()
                && element
                    .getTextContent()
                    .equals(RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER)) {
              element.getParentNode().removeChild(element);
              i--;
            }

            if (element.hasAttribute(DESCRIPTION_ATTRIBUTE)) {
              if (removeDescription) {
                element.removeAttribute(DESCRIPTION_ATTRIBUTE);
              }
            }
          }
        }

        NodeList pluralsElements = document.getElementsByTagName("plurals");
        for (int i = 0; i < pluralsElements.getLength(); i++) {
          Element plurals = (Element) pluralsElements.item(i);
          NodeList items = plurals.getElementsByTagName("item");
          boolean hasTranslated = false;
          boolean hasOther = false;

          for (int j = 0; j < items.getLength(); j++) {
            Element item = (Element) items.item(j);

            if ("other".equals(item.getAttribute("quantity"))) {
              hasOther =
                  !RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER.equals(
                      item.getTextContent());
            }

            if (hasRemoveUntranslated()
                && item.getTextContent()
                    .equals(RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER)) {
              item.getParentNode().removeChild(item);
              j--;
            } else {
              hasTranslated = true;
            }
          }

          if (!hasOther || !hasTranslated) {
            plurals.getParentNode().removeChild(plurals);
            i--;
          }

          if (plurals.hasAttribute(DESCRIPTION_ATTRIBUTE)) {
            if (removeDescription) {
              plurals.removeAttribute(DESCRIPTION_ATTRIBUTE);
            }
          }
        }

        if (removeTranslatableFalse) {
          removeTranslatableFalseElements(document);
        }
        removeWhitespaceNodes(document);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(
            "{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));

        boolean hasDeclaration = xmlContent.trim().startsWith("<?xml");
        boolean hasStandalone = xmlContent.contains("standalone=\"");

        transformer.setOutputProperty(
            OutputKeys.OMIT_XML_DECLARATION, hasDeclaration ? "no" : "yes");

        if (hasStandalone) {
          String standaloneValue = xmlContent.contains("standalone=\"yes\"") ? "yes" : "no";
          transformer.setOutputProperty(OutputKeys.STANDALONE, standaloneValue);
        }

        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new StringWriter());
        transformer.transform(domSource, streamResult);
        String processedXmlContent = streamResult.getWriter().toString();

        if (!hasStandalone) {
          processedXmlContent = processedXmlContent.replace(" standalone=\"no\"", "");
        }

        return processedXmlContent;

      } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
        logger.error("Can't post-process Android XML:\n{}", xmlContent);
        throw new RuntimeException(e);
      }
    }

    void removeWhitespaceNodes(Node node) {
      NodeList childNodes = node.getChildNodes();
      for (int i = childNodes.getLength() - 1; i >= 0; i--) {
        Node childNode = childNodes.item(i);
        if (childNode instanceof Text && childNode.getNodeValue().isBlank()) {
          node.removeChild(childNode);
        } else if (childNode instanceof Element) {
          removeWhitespaceNodes(childNode);
        }
      }
    }

    void removeTranslatableFalseElements(Node node) {
      NodeList childNodes = node.getChildNodes();
      for (int i = childNodes.getLength() - 1; i >= 0; i--) {
        Node childNode = childNodes.item(i);
        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
          Element element = (Element) childNode;
          if (element.hasAttribute("translatable")
              && element.getAttribute("translatable").equals("false")) {
            node.removeChild(element);
          } else {
            removeTranslatableFalseElements(element);
          }
        }
      }
    }
  }
}
