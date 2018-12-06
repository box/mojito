/*===========================================================================
  Copyright (C) 2008-2013 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 2.1 of the License, or (at
  your option) any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
  General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

  See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html
===========================================================================*/

package org.w3c.its;

import net.sf.okapi.common.FileUtil;
import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.Namespaces;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotationType;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.filterwriter.ITSContent;
import nu.validator.htmlparser.dom.HtmlDocumentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Implements the ITS {@link org.w3c.its.IProcessor} and {@link org.w3c.its.ITraversal} interfaces.
 * <p>This class allows you to apply ITS (the Internationalization Tag Set) to a given document
 * and retrieve the corresponding metadata on each node.
 * <p>See <a href='http://www.w3.org/TR/its20/'>http://www.w3.org/TR/its20/</a> for more details on ITS.
 */
public class ITSEngine implements IProcessor, ITraversal {

    public static final String    ITS_VERSION1 = "1.0";
    public static final String    ITS_VERSION2 = "2.0";
    public static final String    ITS_MIMETYPE = "application/its+xml";

    private static final String   FLAGNAME = "\u00ff"; // Name of the user-data property that holds the flags
    private static final String   FLAGSEP  = "\u001c"; // Separator between data categories

    // Must have '?' as many times as there are FP_XXX entries +1
    // Must have +FLAGSEP as many times as there are FP_XXX_DATA entries +1
    private static final String   FLAGDEFAULTDATA     = "????????????????????"
            +FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP
            +FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP+FLAGSEP;

    private static final String PTRFLAG = "@@"; // Flag for pointer-type attributes
    private static final String REFFLAG = "\u0011"; // Flag for Ref vs non-Ref attributes

    // Pattern to validate the pattern allowed for Allowed Characters (thanks you Shaun, Jirka and Pablo!)
    private static final String VALIDALLOWEDCHARS = "((\\\\[nrt\\\\|.?*+(){}\\u002D\\\u005B\\u005D\\u005E]))|(\\[(([^\\u002D\\u005B\\u005D]|"
            + "(\\\\[nrt\\\\|.?*+(){}\\u002D\\\u005B\\u005D\\u005E]))-([^\\u002D\\u005B\\u005D]|(\\\\[nrt\\\\|.?*+(){}\\u002D\\\u005B\\u005D\\u005E]))|"
            + "[^\\u005B\\u005D]|((\\\\[nrt\\\\|.?*+(){}\\u002D\\\u005B\\u005D\\u005E])))+\\])|(\\.)";

    private Pattern validAllowedChars;

    // List of the default elements within text for HTML5 (must start with a space).
    private final String HTML5_WITHINTEXT_YES = " abbr acronym br cite code dfn kbd q samp span strong var b em big hr i small sub sup tt del ins "
            + "bdo img a font center s strike u isindex "
            + "area audio bdi button canvas command datalist embed iframe input keygen label map mark math meter noscript object output "
            + "progress ruby select svg textarea time video wbr "; // Must end with a space
    // List of the default elements nested for HTML5 (must start with a space).
    private final String HTML5_WITHINTEXT_NESTED_STRICT = " script iframe noscript textarea "; // Must end with a space
    // List of the default elements nested for HTML5 (must start with a space).
    private final String HTML5_WITHINTEXT_NESTED = " iframe noscript textarea "; // Must end with a space

    /**
     * HTML5 attributes deemed 'translatable'.
     * This list is completed with hard-codes conditions on several other attributes.
     * See {@link #getTranslate(Attr)} for the complete list.
     */
    private final String HTML5_TRANSATTR_STRICT = " abbr alt download label placeholder srcdoc style title lang "; // Must end with a space
    private final String HTML5_TRANSATTR_PRACTICAL   = " abbr alt download label placeholder title "; // Must end with a space

    // Indicator position
    private static final int      FP_TRANSLATE             = 0;
    private static final int      FP_DIRECTIONALITY        = 1;
    private static final int      FP_WITHINTEXT            = 2;
    private static final int      FP_TERMINOLOGY           = 3;
    private static final int      FP_LOCNOTE               = 4;
    private static final int      FP_PRESERVEWS            = 5;
    private static final int      FP_LANGINFO              = 6;
    private static final int      FP_DOMAIN                = 7;
    private static final int      FP_EXTERNALRES           = 8;
    private static final int      FP_LOCFILTER             = 9;
    private static final int      FP_LQISSUE               = 10;
    private static final int      FP_STORAGESIZE           = 11;
    private static final int      FP_ALLOWEDCHARS          = 12;
    private static final int      FP_SUBFILTER             = 13;
    private static final int      FP_TARGETPOINTER         = 14;
    private static final int      FP_ANNOTATORSREF         = 15;
    private static final int      FP_MTCONFIDENCE          = 16;
    private static final int      FP_TEXTANALYSIS          = 17;
    private static final int      FP_LQRATING              = 18;
    private static final int      FP_PROVENANCE            = 19;

    // Data position
    private static final int      FP_TERMINOLOGY_DATA      = 0;
    private static final int      FP_LOCNOTE_DATA          = 1;
    private static final int      FP_LANGINFO_DATA         = 2;
    private static final int      FP_TARGETPOINTER_DATA    = 3;
    private static final int      FP_IDVALUE_DATA          = 4;
    private static final int      FP_DOMAIN_DATA           = 5;
    private static final int      FP_EXTERNALRES_DATA      = 6;
    private static final int      FP_LOCFILTER_DATA        = 7;
    private static final int      FP_LQISSUE_DATA          = 8;
    private static final int      FP_STORAGESIZE_DATA      = 9;
    private static final int      FP_ALLOWEDCHARS_DATA     = 10;
    private static final int      FP_SUBFILTER_DATA        = 11;
    private static final int      FP_ANNOTATORSREF_DATA    = 12;
    private static final int      FP_MTCONFIDENCE_DATA     = 13;
    private static final int      FP_TEXTANALYSIS_DATA     = 14;
    private static final int      FP_LQRATING_DATA         = 15;
    private static final int      FP_PROVENANCE_DATA       = 16;

    private static final int      INFOTYPE_TEXT            = 0;
    private static final int      INFOTYPE_REF             = 1;
    private static final int      INFOTYPE_POINTER         = 2;
    private static final int      INFOTYPE_REFPOINTER      = 3;

    private final boolean isHTML5;

    private DocumentBuilderFactory xmlFactory;

    private Document doc;
    private URI docURI;
    private XPath xpath;
    private boolean defaultIdsDone;
    private NSContextManager nsContext;
    private VariableResolver varResolver;
    private XPathFactory xpFact;
    private ArrayList<ITSRule> rules;
    private Node node;
    private boolean startTraversal;
    private Stack<ITSTrace> trace;
    private boolean backTracking;
    private boolean translatableAttributeRuleTriggered;
    private boolean targetPointerRuleTriggered;
    private String version;
    private IdGenerator idGen;
    private IdGenerator idProvGen;
    private String docEncoding;

    // This is hopefully temporary, until the official HTML5 defaults match the practical ones
    // strictMode must be set in the first processed rules set
    private boolean strictModeDone = false;
    private boolean strictMode = false;
    private String html5TransAttr = HTML5_TRANSATTR_PRACTICAL;
    private String html5WithinText = HTML5_WITHINTEXT_NESTED;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ITSEngine(Document doc,
                     URI docURI)
    {
        // For backward compatibility
        this(doc, docURI, false, null);
    }

    /**
     * Creates a new ITSEngine object.
     * @param doc the document to process.
     * @param docURI the URI of the document to process.
     * @param docEncoding the default encoding for the document.
     * @param isHTML5 true if the document is an HTML5 document.
     * @param map map of the parameters key-value pairs (can be null).
     */
    public ITSEngine(Document doc,
                     URI docURI,
                     String docEncoding,
                     boolean isHTML5,
                     Map<String,String> map)
    {
        this(doc, docURI, isHTML5, map);
        this.docEncoding = docEncoding;
    }

    public ITSEngine(Document doc,
                     URI docURI,
                     boolean isHTML5,
                     Map<String, String> map)
    {
        this.doc = doc;
        this.docURI = docURI;
        this.isHTML5 = isHTML5;
        node = null;
        rules = new ArrayList<ITSRule>();
        nsContext = new NSContextManager();
        nsContext.addNamespace(Namespaces.ITS_NS_PREFIX, Namespaces.ITS_NS_URI);
        nsContext.addNamespace(Namespaces.ITSX_NS_PREFIX, Namespaces.ITSX_NS_URI);
        if ( isHTML5 ) {
            nsContext.addNamespace(Namespaces.HTML_NS_PREFIX, Namespaces.HTML_NS_URI);
        }
        varResolver = new VariableResolver();
        if ( !Util.isEmpty(map) ) {
            for ( String name : map.keySet() ) {
                varResolver.add(new QName(name), map.get(name), true);
            }
        }

        // Macintosh work-around
        // When you use -XstartOnFirstThread as a java -Xarg on Leopard, your ContextClassloader gets set to null.
        // That is not the case on 10.4 or with Windows or Linux flavors
        // This allows XPathFactory.newInstance() to have a non-null context
        //Removed because not needed any more (1.7 not supported by 10.5)
        //Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        // end work-around
        xpFact = Util.createXPathFactory();

        xpath = xpFact.newXPath();
        xpath.setNamespaceContext(nsContext);
        xpath.setXPathVariableResolver(varResolver);
        defaultIdsDone = false;

        idGen = null;
        idProvGen = null;
    }

    public void setVariables (Map<String, String> map) {
        //TODO
    }

    /**
     * Indicates if the processed document has triggered a rule for a translatable attribute.
     * This must be called only after {@link #applyRules(long)}.
     * @return true if the document has triggered a rule for a translatable attribute.
     */
    public boolean getTranslatableAttributeRuleTriggered () {
        return translatableAttributeRuleTriggered;
    }

    /**
     * Indicates if the processed document has triggered a target pointer rule.
     * This must be called only after {@link #applyRules(long)}.
     * @return true if the processed document has triggered a target pointer rule.
     */
    public boolean getTargetPointerRuleTriggered () {
        return targetPointerRuleTriggered;
    }

    /**
     * Gets internal XPath object used in this ITS engine.
     * @return the internal XPath object used in this ITS engine.
     */
    public XPath getXPath () {
        return xpath;
    }

    private void ensureDocumentBuilderFactoryExists () {
        if ( xmlFactory == null ) {
            xmlFactory = DocumentBuilderFactory.newInstance();
            xmlFactory.setNamespaceAware(true);
            xmlFactory.setValidating(false);
            xmlFactory.setExpandEntityReferences(false);
        }
    }

    private Document parseXMLDocument (String uriString) {
        ensureDocumentBuilderFactoryExists();
        try {
            return xmlFactory.newDocumentBuilder().parse(uriString);
        }
        catch ( Throwable e ) {
            throw new OkapiException("Error parsing an XML document.\n"+e.getMessage(), e);
        }
    }

    private Document parseXMLDocument (InputSource is) {
        ensureDocumentBuilderFactoryExists();
        try {
            return xmlFactory.newDocumentBuilder().parse(is);
        }
        catch ( Throwable e ) {
            throw new OkapiException("Error parsing an XML document.\n"+e.getMessage(), e);
        }
    }

    private Document parseHTMLDocument (String uriString) {
        HtmlDocumentBuilder docBuilder = new HtmlDocumentBuilder();
        try {
            return docBuilder.parse(uriString);
        }
        catch ( Throwable e ) {
            throw new OkapiException("Error parsing an HTML document.\n"+e.getMessage(), e);
        }
    }

    @Override
    public void addExternalRules (URI docURI) {
        try {
            Document rulesDoc = parseXMLDocument(docURI.toString());
            addExternalRules(rulesDoc, docURI);
        }
        catch ( Throwable e ) {
            throw new OkapiException(e);
        }
    }

    @Override
    public void addExternalRules (Document rulesDoc,
                                  URI docURI)
    {
        compileRules(rulesDoc, docURI, false);
    }

    private void compileRulesInScripts (Document hostDoc,
                                        URI docURI,
                                        boolean isInternal)
    {
        try {
            // Look for all script elements with ITS MIME type
            XPathExpression expr = xpath.compile("//"+Namespaces.HTML_NS_PREFIX+":script[@type='"+ITS_MIMETYPE+"']");
            NodeList nl = (NodeList)expr.evaluate(hostDoc, XPathConstants.NODESET);
            for ( int i=0; i<nl.getLength(); i++ ) {
                // Process the rules in the order they are declared
                Element elem = (Element)nl.item(i);
                String content = elem.getTextContent();
                if ( content == null ) continue;
                // Strip encapsulation and white spaces
                content = content.trim();
                if ( content.startsWith("<!--")) content = content.substring(4);
                if ( content.endsWith("-->")) content = content.substring(0, content.length()-3);
                content = content.trim();
                // Parse the content
                InputSource is = new InputSource(new ByteArrayInputStream(docEncoding != null ? content.getBytes(docEncoding) : content.getBytes()));
                Document scriptDoc = parseXMLDocument(is);
                // And compile the rules
                compileRules(scriptDoc, docURI, isInternal);
            }
        }
        catch ( Throwable e ) {
            throw new ITSException("Error processing ITS markup in HTML script.\n"+e.getMessage());
        }

    }

    private void compileRules (Document rulesDoc,
                               URI docURI,
                               boolean isInternal)
    {
        try {
            // Compile the namespaces
            XPathExpression expr = xpath.compile("//*[@selector]//namespace::*");
            NodeList nl = (NodeList)expr.evaluate(rulesDoc, XPathConstants.NODESET);
            for ( int i=0; i<nl.getLength(); i++ ) {
                String prefix = nl.item(i).getLocalName();
                if ( "xml".equals(prefix) ) continue; // Set by default
                String uri = nl.item(i).getNodeValue();
                nsContext.addNamespace(prefix, uri);
            }

            // Compile the rules
            // First: get the its:rules element(s)
            expr = xpath.compile("//"+Namespaces.ITS_NS_PREFIX+":rules");
            nl = (NodeList)expr.evaluate(rulesDoc, XPathConstants.NODESET);
            if ( nl.getLength() == 0 ) return; // Nothing to do

            // Process each its:rules element
            Element rulesElem;
            for ( int i=0; i<nl.getLength(); i++ ) {
                rulesElem = (Element)nl.item(i);
                // Check version
                String prev = version;
                version = rulesElem.getAttributeNS(null, "version");
                if ( !version.equals(ITS_VERSION1) && !version.equals(ITS_VERSION2) ) {
                    throw new ITSException(String.format("Invalid or missing ITS version (\"%s\")", version));
                }
                if (( prev != null ) && !prev.equals("0") && !version.equals(prev) ) {
                    throw new ITSException(String.format("Two different versions of ITS declared in the same document: '%s' and '%s'.", prev, version));
                }

                // Check strict mode (extension)
                // The HTML5 defaults are not the default most users want.
                // To have them you must set itsx:strict='yes' in the first rules set
                //TODO: Remove strictMode mechanism when the official defaults match the practical defaults
                if ( !strictModeDone ) {
                    strictMode = rulesElem.getAttributeNS(Namespaces.ITSX_NS_URI, "strict").equals("yes");
                    if ( strictMode ) {
                        html5TransAttr = HTML5_TRANSATTR_STRICT;
                        html5WithinText = HTML5_WITHINTEXT_NESTED_STRICT;
                    }
                    // else nothing to do: default is not-strict
                    strictModeDone = true;
                }

                // Check queryLanguage
                String qlang = rulesElem.getAttributeNS(null, "queryLanguage");
                if ( !Util.isEmpty(qlang) ) {
                    if ( isHTML5 ) qlang = qlang.toLowerCase();
                    if ( !qlang.startsWith("xpath") ) {
                        throw new ITSException(String.format("ITS queryLanguage is '%s', but this implementation supports only XPath.", qlang));
                    }
                    if ( !qlang.equals("xpath") ) {
                        // Some version other than 1.0 of XPath: proceed, but warn of the potential issues
                        logger.warn("ITS queryLanguage is '{}', but this implementation supports only XPath 1.0: You may or may not run into problems.'", qlang);
                    }
                }

                // Check for link
                String href = rulesElem.getAttributeNS(Namespaces.XLINK_NS_URI, "href");
                if ( href.length() > 0 ) {
                    int n = href.lastIndexOf('#');
                    if ( n > -1 ) {
                        href = href.substring(0, n);
                    }

                    // xlink:href allows the use of xml:base so we need to calculate it
                    // The initial base is the folder of the current document
                    String baseFolder = "";
                    if ( docURI != null) baseFolder = FileUtil.getPartBeforeFile(docURI);

                    // Then we look for the last xml:base specified
                    Node node = rulesElem;
                    while ( node != null ) {
                        if ( node.getNodeType() == Node.ELEMENT_NODE ) {
                            //TODO: Relative path with ../../ constructs
                            String xmlBase = ((Element)node).getAttribute("xml:base");
                            if ( xmlBase.length() > 0 ) {
                                if ( xmlBase.endsWith("/") )
                                    xmlBase = xmlBase.substring(0, xmlBase.length()-1);
                                if ( !baseFolder.startsWith("/") )
                                    baseFolder = xmlBase + "/" + baseFolder;
                                else
                                    baseFolder = xmlBase + baseFolder;
                            }
                        }
                        node = node.getParentNode(); // Back-track to parent
                    }
                    if ( baseFolder.length() > 0 ) {
                        if ( baseFolder.endsWith("/") )
                            baseFolder = baseFolder.substring(0, baseFolder.length()-1);
                        if ( !href.startsWith("/") ) href = baseFolder + "/" + href;
                        else href = baseFolder + href;
                    }

                    // Load the document and the rules
                    URI linkedDoc = new URI(href);
                    loadLinkedRules(linkedDoc, isInternal);
                }

                // Process each rule inside its:rules
                expr = xpath.compile("//"+Namespaces.ITS_NS_PREFIX+":*|//"+Namespaces.ITSX_NS_PREFIX+":*");
                NodeList nl2 = (NodeList)expr.evaluate(rulesElem, XPathConstants.NODESET);
                if ( nl2.getLength() == 0 ) break; // Nothing to do, move to next its:rules

                Element ruleElem;
                for ( int j=0; j<nl2.getLength(); j++ ) {
                    ruleElem = (Element)nl2.item(j);
                    String locName = ruleElem.getLocalName();
                    if ( "translateRule".equals(locName) ) {
                        compileTranslateRule(ruleElem, isInternal);
                    }
                    else if ( "withinTextRule".equals(locName) ) {
                        compileWithinTextRule(ruleElem, isInternal);
                    }
                    else if ( "langRule".equals(locName) ) {
                        compileLangRule(ruleElem, isInternal);
                    }
                    else if ( "dirRule".equals(locName) ) {
                        compileDirRule(ruleElem, isInternal);
                    }
                    else if ( "locNoteRule".equals(locName) ) {
                        compileLocNoteRule(ruleElem, isInternal);
                    }
                    else if ( "termRule".equals(locName) ) {
                        compileTermRule(ruleElem, isInternal);
                    }
                    else if ( "idValueRule".equals(locName) ) {
                        compileIdValueRule(ruleElem, isInternal);
                    }
                    else if ( "domainRule".equals(locName) ) {
                        compileDomainRule(ruleElem, isInternal);
                    }
                    else if ( "targetPointerRule".equals(locName) ) {
                        compileTargetPointerRule(ruleElem, isInternal);
                    }
                    else if ( "localeFilterRule".equals(locName) ) {
                        compileLocaleFilterRule(ruleElem, isInternal);
                    }
                    else if ( "preserveSpaceRule".equals(locName) ) {
                        compilePrserveSpaceRule(ruleElem, isInternal);
                    }
                    else if ( "externalResourceRefRule".equals(locName) ) {
                        compileExternalResourceRule(ruleElem, isInternal);
                    }
                    else if ( "locQualityIssueRule".equals(locName) ) {
                        compileLocQualityIssueRule(ruleElem, isInternal);
                    }
                    else if ( "provRule".equals(locName) ) {
                        compileProvRule(ruleElem, isInternal);
                    }
                    else if ( "storageSizeRule".equals(locName) ) {
                        compileStorageSizeRule(ruleElem, isInternal);
                    }
                    else if ( "allowedCharactersRule".equals(locName) ) {
                        compileAllowedCharactersRule(ruleElem, isInternal);
                    }
                    else if ( "mtConfidenceRule".equals(locName) ) {
                        compileMtConfidenceRule(ruleElem, isInternal);
                    }
                    else if ( "textAnalysisRule".equals(locName) ) {
                        compileTextAnalysisRule(ruleElem, isInternal);
                    }
                    else if ( "subFilterRule".equals(locName) ) {
                        compileSubFilterRule(ruleElem, isInternal);
                    }
                    else if ( "param".equals(locName) ) {
                        processParam(ruleElem);
                    }
                    else if ( !"rules".equals(locName)
                            && !"span".equals(locName)
                            && !"locQualityIssues".equals(locName)
                            && !"locQualityIssue".equals(locName)
                            && !"locNote".equals(locName)
                            && !"provenanceRecords".equals(locName)
                            && !"provenanceRecord".equals(locName) ) {
                        logger.warn("Unknown element '{}'.", ruleElem.getNodeName());
                    }
                }
            }
        }
        catch ( XPathExpressionException e ) {
            throw new OkapiException(e);
        }
        catch ( URISyntaxException e ) {
            throw new OkapiException(e);
        }
    }

    private void processParam (Element elem) {
        String value = elem.getTextContent();
        String name = elem.getAttribute("name");
        if ( name.isEmpty() ) {
            throw new ITSException("Invalid value for 'name' in param.");
        }
        // Do not overwrite existing values (the element defines defaults)
        varResolver.add(new QName(name), value, false);
    }

    private void loadLinkedRules (URI docURI,
                                  boolean isInternal)
    {
        try {
            Document rulesDoc = parseXMLDocument(docURI.toString());
            compileRules(rulesDoc, docURI, isInternal);
        }
        catch ( Throwable e ) {
            throw new OkapiException(e);
        }
    }

    private void compileTranslateRule (Element elem,
                                       boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_TRANSLATE);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        String value = elem.getAttribute("translate");
        if ( "yes".equals(value) ) rule.flag = true;
        else if ( "no".equals(value) ) rule.flag = false;
        else throw new ITSException("Invalid value for 'translate'.");

        // idValue extension (deprecated but supported)
        value = elem.getAttributeNS(Namespaces.ITSX_NS_URI, "idValue");
        if ( !value.isEmpty() ) {
            if ( version.equals(ITS_VERSION2) ) {
                // Warn if the extension is used in ITS 2.0
                logger.warn("This document uses the {}:idValue extension instead of the ITS 2.0 Id Value data category.",
                        Namespaces.ITSX_NS_URI);
            }
            rule.idValue = value;
        }

        // whiteSpaces extension (deprecated but supported)
        value = elem.getAttributeNS(Namespaces.ITSX_NS_URI, "whiteSpaces");
        if ( !value.isEmpty() ) {
            if ( version.equals(ITS_VERSION2) ) {
                // Warn if the extension is used in ITS 2.0
                logger.warn("This document uses the {}:whiteSpaces extension instead of the ITS 2.0 Preserve Space data category.",
                        Namespaces.ITSX_NS_URI);
            }
            if ( "preserve".equals(value) ) rule.preserveWS = true;
            else if ( "default".equals(value) ) rule.preserveWS = false;
            else throw new ITSException("Invalid value for 'whiteSpaces'.");
        }

        rules.add(rule);
    }

    private void compileDirRule (Element elem,
                                 boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_DIRECTIONALITY);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        String value = elem.getAttribute("dir");
        if ( "ltr".equals(value) ) rule.value = DIR_LTR;
        else if ( "rtl".equals(value) ) rule.value = DIR_RTL;
        else if ( "lro".equals(value) ) rule.value = DIR_LRO;
        else if ( "rlo".equals(value) ) rule.value = DIR_RLO;
        else throw new ITSException("Invalid value for 'dir'.");
        rules.add(rule);
    }

    private void compileWithinTextRule (Element elem,
                                        boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_WITHINTEXT);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        String value = elem.getAttribute("withinText");
        if ( "yes".equals(value) ) rule.value = WITHINTEXT_YES;
        else if ( "no".equals(value) ) rule.value = WITHINTEXT_NO;
        else if ( "nested".equals(value) ) rule.value = WITHINTEXT_NESTED;
        else throw new ITSException("Invalid value for 'withinText'.");

        rules.add(rule);
    }

    private void compileIdValueRule (Element elem,
                                     boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_IDVALUE);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        String value = elem.getAttribute("idValue");
        if ( value.isEmpty() ) {
            throw new ITSException("Invalid value for 'idValue'.");
        }
        rule.idValue = value;
        rules.add(rule);
    }

    private void compileDomainRule (Element elem,
                                    boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_DOMAIN);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        String pointer = elem.getAttribute("domainPointer");
        if ( pointer.isEmpty() ) {
            throw new ITSException("Invalid value for 'domainPointer'.");
        }
        rule.info = pointer;

        // Process domainMapping attribute if it's there
        rule.map = fromStringToMap(elem.getAttribute("domainMapping"));

        // Add the rule
        rules.add(rule);
    }

    private void compileExternalResourceRule (Element elem,
                                              boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_EXTERNALRES);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        String pointer = elem.getAttribute("externalResourceRefPointer");
        if ( pointer.isEmpty() ) {
            throw new ITSException("Invalid value for 'externalResourceRefPointer'.");
        }
        rule.info = pointer;

        // Add the rule
        rules.add(rule);
    }

    private void compileStorageSizeRule (Element elem,
                                         boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_STORAGESIZE);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        String np[] = retrieveStorageSizeData(elem, false, false);

        String storageSizeP = null;
        if ( elem.hasAttribute("storageSizePointer") )
            storageSizeP = elem.getAttribute("storageSizePointer");

        String storageEncodingP = null;
        if (elem.hasAttribute("storageEncodingPointer"))
            storageEncodingP = elem.getAttribute("storageEncodingPointer");

        // Check we have the mandatory attributes
        if ( Util.isEmpty(np[0]) && Util.isEmpty(storageSizeP) ) {
            throw new ITSException("You must have at least an attribute storageSize or storageSizePointer.");
        }

        rule.annotations = new GenericAnnotations();
        GenericAnnotation ann = rule.annotations.add(GenericAnnotationType.STORAGESIZE);

        // Check pointer vs non-pointers
        if ( !Util.isEmpty(np[0]) ) {
            if ( !Util.isEmpty(storageSizeP) ) {
                throw new ITSException("Cannot have both storageSize and storageSizePointer.");
            }
            ann.setString(GenericAnnotationType.STORAGESIZE_SIZE, np[0]);
        }
        else {
            ann.setString(GenericAnnotationType.STORAGESIZE_SIZE, PTRFLAG+storageSizeP);
        }

        if ( !Util.isEmpty(storageEncodingP) ) {
            ann.setString(GenericAnnotationType.STORAGESIZE_ENCODING, PTRFLAG+storageEncodingP);
        }
        else { // Always has a default
            ann.setString(GenericAnnotationType.STORAGESIZE_ENCODING, np[1]);
        }

        // No pointer for line break type (and always has a default)
        ann.setString(GenericAnnotationType.STORAGESIZE_LINEBREAK, np[2]);

        // Add the rule
        rules.add(rule);
    }

    private void compileAllowedCharactersRule (Element elem,
                                               boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_ALLOWEDCHARS);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        rule.info = retrieveAllowedCharsData(elem, false, false);

        String allowedCharsP = null;
        if ( elem.hasAttribute("allowedCharactersPointer") )
            allowedCharsP = elem.getAttribute("allowedCharactersPointer");

        // Check we have the mandatory attributes
        if ( Util.isEmpty(rule.info) && Util.isEmpty(allowedCharsP) ) {
            throw new ITSException("You must have at least an attribute allowedCharacters or allowedCharactersPointer.");
        }

        if ( !Util.isEmpty(rule.info) ) {
            if ( !Util.isEmpty(allowedCharsP) ) {
                throw new ITSException("Cannot have both allowedCharacters and allowedCharactersPointer.");
            }
        }
        else {
            rule.info = allowedCharsP;
            rule.infoType = INFOTYPE_POINTER;
        }

        // Add the rule
        rules.add(rule);
    }

    private void compileMtConfidenceRule (Element elem,
                                          boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_MTCONFIDENCE);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        rule.info = retrieveMtconfidence(elem, false, false);
        if ( rule.info == null ) return;

        rule.infoType = INFOTYPE_TEXT;
        rules.add(rule);
    }

    private void compileLocQualityIssueRule (Element elem,
                                             boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_LOCQUALITYISSUE);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        // Get the local attributes
        String np[] = retrieveLocQualityIssueData(elem, false, false);

        String issuesRefP = null;
        if ( elem.hasAttribute("locQualityIssuesRefPointer") )
            issuesRefP = elem.getAttribute("locQualityIssuesRefPointer");

        String typeP = null;
        if (elem.hasAttribute("locQualityIssueTypePointer"))
            typeP = elem.getAttribute("locQualityIssueTypePointer");

        String commentP = null;
        if ( elem.hasAttribute("locQualityIssueCommentPointer"))
            commentP = elem.getAttribute("locQualityIssueCommentPointer");

        // Check we have the mandatory attributes
        if (( Util.isEmpty(np[0]) && Util.isEmpty(issuesRefP) )
                && ( Util.isEmpty(np[1]) && Util.isEmpty(typeP) )
                && ( Util.isEmpty(np[2]) && Util.isEmpty(commentP) ))
        {
            throw new ITSException("You must have at least a type or a comment or isses reference ainformation defined.");
        }
        rule.annotations = createLQIAnnotationSet();
        GenericAnnotation ann = addIssueItem(rule.annotations);

        if ( !Util.isEmpty(np[0]) ) {
            if ( !Util.isEmpty(issuesRefP) ) {
                throw new ITSException("Cannot have both locQualityIssuesRef and locQualityIssuesRefPointer.");
            }
            rule.info = np[0];
            rule.infoType = INFOTYPE_REF;
        }
        else if ( issuesRefP != null ) {
            rule.info = issuesRefP;
            rule.infoType = INFOTYPE_REFPOINTER;
        }

        // For the annotation info, we add '@@' in front if it is a pointer

        if ( !Util.isEmpty(np[1]) ) {
            if ( !Util.isEmpty(typeP) ) {
                throw new ITSException("Cannot have both locQualityIssueType and locQualityIssueTypePointer.");
            }
            ann.setString(GenericAnnotationType.LQI_TYPE, np[1]);
            // TODO: verify the value?
        }
        else if ( typeP != null ) {
            ann.setString(GenericAnnotationType.LQI_TYPE, PTRFLAG+typeP);
        }

        // Get the comment
        if ( !Util.isEmpty(np[2]) ) {
            if ( !Util.isEmpty(commentP) ) {
                throw new ITSException("Cannot have both locQualityIssueComment and locQualityIssueCommentPointer.");
            }
            ann.setString(GenericAnnotationType.LQI_COMMENT, np[2]);
        }
        else if ( commentP != null ) {
            ann.setString(GenericAnnotationType.LQI_COMMENT, PTRFLAG+commentP);
        }

        // Get the optional severity
        String severityP = null;
        if ( elem.hasAttribute("locQualityIssueSeverityPointer") )
            severityP = elem.getAttribute("locQualityIssueSeverityPointer");
        if ( !Util.isEmpty(np[3]) ) {
            if ( !Util.isEmpty(severityP) ) {
                throw new ITSException("Cannot have both locQualityIssueSeverity and locQualityIssueSeverityPointer.");
            }
            // Do not convert the Double yet, this is done when triggering the rule
            ann.setString(GenericAnnotationType.LQI_SEVERITY, np[3]);
        }
        else if ( severityP != null ) {
            ann.setString(GenericAnnotationType.LQI_SEVERITY, PTRFLAG+severityP);
        }

        // Get the optional profile reference
        String profileRefP = null;
        if ( elem.hasAttribute("locQualityIssueProfileRefPointer"))
            profileRefP = elem.getAttribute("locQualityIssueProfileRefPointer");
        if ( !Util.isEmpty(np[4]) ) {
            if ( !Util.isEmpty(profileRefP) ) {
                throw new ITSException("Cannot have both locQualityIssueProfileRef and locQualityIssueProfileRefPointer.");
            }
            ann.setString(GenericAnnotationType.LQI_PROFILEREF, np[4]);
        }
        else if ( profileRefP != null ) {
            ann.setString(GenericAnnotationType.LQI_PROFILEREF, PTRFLAG+profileRefP);
        }

        // Get the optional enabled
        String enabledP = null;
        if ( elem.hasAttribute("locQualityIssueEnabledPointer")) {
            profileRefP = elem.getAttribute("locQualityIssueEnabledPointer");
            ann.setString(GenericAnnotationType.LQI_ENABLED, PTRFLAG+enabledP);
        }
        else { // either default or set value
            ann.setString(GenericAnnotationType.LQI_ENABLED, np[5]);
        }

        // Add the rule
        rules.add(rule);
    }

    private void compileProvRule (Element elem,
                                  boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_PROVENANCE);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        String pointer = elem.getAttribute("provenanceRecordsRefPointer");
        if ( Util.isEmpty(pointer) ) {
            throw new ITSException("You must have a provenanceRecordsRefPointer attribute defined.");
        }
        rule.info = pointer;
        rule.infoType = INFOTYPE_REFPOINTER;
        // Add the rule
        rules.add(rule);
    }

    private GenericAnnotations createLQIAnnotationSet () {
        GenericAnnotations anns = new GenericAnnotations();
        if ( idGen == null ) idGen = new IdGenerator(null, "lqi");
        anns.setData(idGen.createId());
        return anns;
    }

    private GenericAnnotations createProvenanceAnnotationSet () {
        GenericAnnotations anns = new GenericAnnotations();
        if ( idProvGen == null ) idProvGen = new IdGenerator(null, "prov");
        anns.setData(idProvGen.createId());
        return anns;
    }

    private void compileTextAnalysisRule (Element elem,
                                          boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_TEXTANALYSIS);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        // Get the local attributes
        String np[] = retrieveTextAnalysisData(elem, false, false);

        String classRefP = null;
        if ( elem.hasAttribute("taClassRefPointer") )
            classRefP = elem.getAttribute("taClassRefPointer");

        String sourceP = null;
        if (elem.hasAttribute("taSourcePointer"))
            sourceP = elem.getAttribute("taSourcePointer");

        String identP = null;
        if ( elem.hasAttribute("taIdentPointer"))
            identP = elem.getAttribute("taIdentPointer");

        String identRefP = null;
        if ( elem.hasAttribute("taIdentRefPointer"))
            identRefP = elem.getAttribute("taIdentRefPointer");

        rule.annotations = new GenericAnnotations();
        GenericAnnotation ann = rule.annotations.add(GenericAnnotationType.TA);

        // For the annotation info, we add '@@' in front if it is a pointer
        // also flag with REFFLAG if it is a ref version

        if ( classRefP != null ) {
            ann.setString(GenericAnnotationType.TA_CLASS, PTRFLAG+REFFLAG+classRefP);
        }
        else if ( np[0] != null ) {
            ann.setString(GenericAnnotationType.TA_CLASS, np[0]);
        }

        if ( sourceP != null ) {
            ann.setString(GenericAnnotationType.TA_SOURCE, PTRFLAG+sourceP);
        }
        else if ( np[1] != null ) {
            ann.setString(GenericAnnotationType.TA_SOURCE, np[1]);
        }

        if ( identP != null ) {
            ann.setString(GenericAnnotationType.TA_IDENT, PTRFLAG+identP);
        }
        else if ( np[2] != null ) {
            ann.setString(GenericAnnotationType.TA_IDENT, np[2]);
        }

        if ( identRefP != null ) {
            ann.setString(GenericAnnotationType.TA_IDENT, PTRFLAG+REFFLAG+identRefP);
        }
        else if ( np[3] != null ) {
            ann.setString(GenericAnnotationType.TA_IDENT, np[3]);
        }
        // No confidence information in global rule

        // Add the rule
        rules.add(rule);
    }


    private void compileLocaleFilterRule (Element elem,
                                          boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_LOCFILTER);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;
        // Retrieve the list
        rule.info = retrieveLocaleFilterList(elem, false, false);
        // Add the rule
        rules.add(rule);
    }

    private void compileSubFilterRule (Element elem,
                                       boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_SUBFILTER);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;
        // Retrieve the list
        rule.info = retrieveSubFilter(elem, false, false);
        // Add the rule
        rules.add(rule);
    }

    private void compilePrserveSpaceRule (Element elem,
                                          boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_PRESERVESPACE);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;
        // Get the value
        String value = elem.getAttribute("space");
        if (( !"preserve".equals(value) ) && ( !"default".equals(value) )) {
            throw new ITSException("Invalid value for 'space'.");
        }
        rule.preserveWS = "preserve".equals(value);
        // Add the rule
        rules.add(rule);
    }

    private void compileTargetPointerRule (Element elem,
                                           boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_TARGETPOINTER);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        String pointer = elem.getAttribute("targetPointer");
        if ( pointer.isEmpty() ) {
            throw new ITSException("Invalid value for 'targetPointer'.");
        }
        // Resolves the variables here
        rule.info = varResolver.replaceVariables(pointer);

        // Add the rule
        rules.add(rule);
    }

    /**
     * Converts a string like domainMapping to a map.
     * @param mapping the string to process.
     * @return the map for the given string, or null if there is no values.
     */
    private Map<String, String> fromStringToMap (String mapping) {
        if ( mapping.isEmpty() ) return null;

        Map<String, String> map = null;

        if ( !mapping.isEmpty() ) {
            // Parse the list of paired values
            // Split list on commas
            String[] pairs = mapping.split(",", 0);
            // Split the pairs
            char endQuote = 0x00; int state = 0;
            for ( String pair : pairs ) {
                pair = pair.trim();
                StringBuilder left = new StringBuilder();
                StringBuilder right = new StringBuilder();
                StringBuilder str = left;
                for ( int i=0; i<pair.length(); i++ ) {
                    char ch = pair.charAt(i);
                    switch ( ch ) {
                        case '\"':
                            if ( state == 0 )  {
                                endQuote = ch;
                                state = 1;
                            }
                            else {
                                if ( ch == endQuote ) state = 0; // End of string
                                else str.append(ch); // Else: we store
                            }
                            continue;
                        case '\'':
                            if ( state == 0 )  {
                                endQuote = ch;
                                state = 1;
                            }
                            else {
                                if ( ch == endQuote ) state = 0; // End of string
                                else str.append(ch); // Else: we store
                            }
                            continue;
                        case ' ':
                            // If it's a space inside a quoted string: we add to the string
                            // Otherwise it we change to the right side value
                            if ( state == 1 ) str.append(' ');
                            else str = right;
                            continue;
                        default:
                            str.append(pair.charAt(i));
                            break;
                    }
                }

                if (( left.length() == 0 ) || ( right.length() == 0 )) {
                    throw new ITSException("Invalid pair in mapping value.");
                }

                if ( map == null ) {
                    map = new LinkedHashMap<String, String>();
                }
                map.put(left.toString(), right.toString());
            }
        }

        return map;
    }

    private void compileTermRule (Element elem,
                                  boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_TERMINOLOGY);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        // Get the local attributes
        String np[] = retrieveTerminologyData(elem, false, false);

        rule.flag = (np[0] != null );
        rules.add(rule);

        // Return now if its not a term
        if ( !rule.flag ) return;

        String infoRefP = null;
        if ( elem.hasAttribute("termInfoRefPointer") )
            infoRefP = elem.getAttribute("termInfoRefPointer");

        String infoP = null;
        if (elem.hasAttribute("termInfoPointer"))
            infoP = elem.getAttribute("termInfoPointer");

        rule.annotations = new GenericAnnotations();
        GenericAnnotation ann = rule.annotations.add(GenericAnnotationType.TERM);

        // For the annotation info, we add '@@' in front if it is a pointer
        // also flag with REFFLAG if it is a ref version
        if ( infoRefP != null ) {
            ann.setString(GenericAnnotationType.TERM_INFO, PTRFLAG+REFFLAG+infoRefP);
        }
        if ( infoP != null ) {
            ann.setString(GenericAnnotationType.TERM_INFO, PTRFLAG+infoP);
        }
        if ( np[1] != null ) { // The REF prefix is already on this
            ann.setString(GenericAnnotationType.TERM_INFO, np[1]);
        }
        // No confidence information in global rule

        // Add the rule
        rules.add(rule);
    }

    private void compileLocNoteRule (Element elem,
                                     boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_LOCNOTE);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        rule.flag = retrieveLocNoteType(elem, false, true, false);

        // Try to get the locNote element
        String value1 = null;
        NodeList list = elem.getElementsByTagNameNS(Namespaces.ITS_NS_URI, "locNote");
        if ( list.getLength() > 0 ) {
            value1 = getTextContent(list.item(0));
        }
        // Get the attributes
        String value2 = elem.getAttribute("locNotePointer");
        String value3 = elem.getAttribute("locNoteRef");
        String value4 = elem.getAttribute("locNoteRefPointer");

        if ( value1 != null ) {
            rule.infoType = INFOTYPE_TEXT;
            rule.info = value1;
            if (( value2.length() > 0 ) || ( value3.length() > 0 ) || ( value4.length() > 0 )) {
                throw new ITSException("Too many locNote attributes specified");
            }
        }
        else {
            if ( value2.length() > 0 ) {
                rule.infoType = INFOTYPE_POINTER;
                rule.info = value2;
                if (( value3.length() > 0 ) || ( value4.length() > 0 )) {
                    throw new ITSException("Too many locNote attributes specified");
                }
            }
            else {
                if ( value3.length() > 0 ) {
                    rule.infoType = INFOTYPE_REF;
                    rule.info = value3;
                    if ( value4.length() > 0 ) {
                        throw new ITSException("Too many locNote attributes specified");
                    }
                }
                else {
                    if ( value4.length() > 0 ) {
                        rule.infoType = INFOTYPE_REFPOINTER;
                        rule.info = value4;
                    }
                }
            }
        }

        rules.add(rule);
    }

    private void compileLangRule (Element elem,
                                  boolean isInternal)
    {
        ITSRule rule = new ITSRule(IProcessor.DC_LANGINFO);
        rule.selector = elem.getAttribute("selector");
        rule.isInternal = isInternal;

        rule.info = elem.getAttribute("langPointer");
        if ( rule.info.isEmpty() ) {
            throw new ITSException("langPointer attribute missing.");
        }
        rules.add(rule);
    }

    public void applyRules (long dataCategories) {
        translatableAttributeRuleTriggered = false;
        targetPointerRuleTriggered = false;
        version = "0"; // Needs to be not null (in case there is no ITS at all in file)
        processGlobalRules(dataCategories);
        processLocalRules(dataCategories);
    }

    private void removeFlag (Node node) {
        //TODO: Any possible optimization, instead of using recursive calls
        if ( node == null ) return;
        node.setUserData(FLAGNAME, null, null);
        if ( node.hasChildNodes() )
            removeFlag(node.getFirstChild());
        if ( node.getNextSibling() != null )
            removeFlag(node.getNextSibling());
    }

    public void disapplyRules () {
        removeFlag(doc.getDocumentElement());
        translatableAttributeRuleTriggered = false;
        targetPointerRuleTriggered = false;
    }

    public boolean backTracking () {
        return backTracking;
    }

    public Node nextNode () {
        if ( startTraversal ) {
            startTraversal = false;
            // Set the initial trace with default behaviors
            ITSTrace startTrace = new ITSTrace();
            backTracking = false;
            startTrace.translate = true;
            startTrace.isChildDone = true;
            trace.push(startTrace); // For first child
            node = doc.getFirstChild();
            trace.push(new ITSTrace(trace.peek(), false));
            // Overwrite any default behaviors if needed
            updateTraceData(node);
            return node;
        }
        if ( node != null ) {
            backTracking = false;
            if ( !trace.peek().isChildDone && node.hasChildNodes() ) {
                // Change the flag for the current node
                ITSTrace tmp = new ITSTrace(trace.peek(), true);
                trace.pop();
                trace.push(tmp);
                // Get the new node and push its flag
                node = node.getFirstChild();
                trace.push(new ITSTrace(trace.peek(), false));
            }
            else {
                Node tmpNode = node.getNextSibling();
                if ( tmpNode == null ) {
                    node = node.getParentNode();
                    trace.pop();
                    backTracking = true;
//TODO: we should not have to update the trace data on backtracking					return node;
                }
                else {
                    node = tmpNode;
                    trace.pop(); // Remove flag for previous sibling
                    trace.push(new ITSTrace(trace.peek(), false)); // Set new flag for new sibling
                    // HTML5 defaults
                }
            }
        }
        updateTraceData(node);
        return node;
    }

    /**
     * Updates the trace stack.
     * @param newNode Node to update
     */
    private void updateTraceData (Node newNode) {
        // Check if the node is null
        if ( newNode == null ) return;

        // Get the flag data
        String data = (String)newNode.getUserData(FLAGNAME);

        // If this node has no ITS flags, then we leave the current states
        // as they are. They have been set by inheritance.
        if ( data == null ) {
            if ( isHTML5 ) applyHTML5Defaults(newNode);
            return;
        }

        // Otherwise: see if there are any flags to change
        if ( data.charAt(FP_TRANSLATE) != '?' ) {
            trace.peek().translate = (data.charAt(FP_TRANSLATE) == 'y');
        }

        String value = getFlagData(data, FP_IDVALUE_DATA);
        if ( !value.isEmpty() ) {
            trace.peek().idValue = value;
        }

        if ( data.charAt(FP_DOMAIN) != '?' ) {
            trace.peek().domains = getFlagData(data, FP_DOMAIN_DATA);
        }

        if ( data.charAt(FP_EXTERNALRES) != '?' ) {
            trace.peek().externalRes = getFlagData(data, FP_EXTERNALRES_DATA);
        }

        if ( data.charAt(FP_LOCFILTER) != '?' ) {
            trace.peek().localeFilter = getFlagData(data, FP_LOCFILTER_DATA);
        }

        if ( data.charAt(FP_LQISSUE) == 'y' ) {
            trace.peek().lqIssues = new GenericAnnotations(getFlagData(data, FP_LQISSUE_DATA));
        }

        if ( data.charAt(FP_PROVENANCE) == 'y' ) {
            trace.peek().prov = new GenericAnnotations(getFlagData(data, FP_PROVENANCE_DATA));
        }

        if ( data.charAt(FP_TEXTANALYSIS) == 'y' ) {
            trace.peek().ta = new GenericAnnotations(getFlagData(data, FP_TEXTANALYSIS_DATA));
        }

        if ( data.charAt(FP_LQRATING) == 'y' ) {
            trace.peek().lqRating = new GenericAnnotations(getFlagData(data, FP_LQRATING_DATA));
        }

        if ( data.charAt(FP_TERMINOLOGY) == 'y' ) {
            trace.peek().termino = new GenericAnnotations(getFlagData(data, FP_TERMINOLOGY_DATA));
        }

        if ( data.charAt(FP_STORAGESIZE) != '?' ) {
            trace.peek().storageSize = new GenericAnnotations(getFlagData(data, FP_STORAGESIZE_DATA));
        }

        if ( data.charAt(FP_MTCONFIDENCE) != '?' ) {
            value = getFlagData(data, FP_MTCONFIDENCE_DATA);
            trace.peek().mtConfidence = Double.parseDouble(value);
        }

        if ( data.charAt(FP_ALLOWEDCHARS) != '?' ) {
            trace.peek().allowedChars = getFlagData(data, FP_ALLOWEDCHARS_DATA);
        }

        if ( data.charAt(FP_TARGETPOINTER) != '?' ) {
            trace.peek().targetPointer = getFlagData(data, FP_TARGETPOINTER_DATA);
        }

        if ( data.charAt(FP_DIRECTIONALITY) != '?' ) {
            switch ( data.charAt(FP_DIRECTIONALITY) ) {
                case '0':
                    trace.peek().dir = DIR_LTR;
                    break;
                case '1':
                    trace.peek().dir = DIR_RTL;
                    break;
                case '2':
                    trace.peek().dir = DIR_LRO;
                    break;
                case '3':
                    trace.peek().dir = DIR_LRO;
                    break;
            }
        }

        if ( data.charAt(FP_WITHINTEXT) == '?' ) {
            if ( isHTML5 ) applyHTML5Defaults(newNode);
        }
        else {
            switch ( data.charAt(FP_WITHINTEXT) ) {
                case '0':
                    trace.peek().withinText = WITHINTEXT_NO;
                    break;
                case '1':
                    trace.peek().withinText = WITHINTEXT_YES;
                    break;
                case '2':
                    trace.peek().withinText = WITHINTEXT_NESTED;
                    break;
            }
        }

        if ( data.charAt(FP_LOCNOTE) != '?' ) {
            trace.peek().locNote = getFlagData(data, FP_LOCNOTE_DATA);
            trace.peek().locNoteType = (data.charAt(FP_LOCNOTE)=='a' ? "alert" : "description");
        }

        // Preserve white spaces
        if ( data.charAt(FP_PRESERVEWS) != '?' ) {
            trace.peek().preserveWS = (data.charAt(FP_PRESERVEWS) == 'y');
        }

        if ( data.charAt(FP_LANGINFO) != '?' ) {
            trace.peek().language = getFlagData(data, FP_LANGINFO_DATA);
        }

        if ( data.charAt(FP_SUBFILTER) != '?' ) {
            trace.peek().subFilter = getFlagData(data, FP_SUBFILTER_DATA);
        }

        if ( data.charAt(FP_ANNOTATORSREF) != '?' ) {
            trace.peek().annotatorsRef = ITSContent.updateAnnotatorsRef(
                    trace.peek().annotatorsRef, getFlagData(data, FP_ANNOTATORSREF_DATA));
        }
    }

    private void applyHTML5Defaults (Node newNode) {
        if ( newNode.getNodeType() == Node.ELEMENT_NODE ) {
            String search = " "+newNode.getNodeName().toLowerCase()+" ";
            if ( HTML5_WITHINTEXT_YES.indexOf(search) != -1 ) {
                trace.peek().withinText = ITraversal.WITHINTEXT_YES;
            }
            else if ( html5WithinText.indexOf(search) != -1 ) {
                trace.peek().withinText = ITraversal.WITHINTEXT_NESTED;
            }
        }
    }

    public void startTraversal () {
        node = null;
        trace = new Stack<ITSTrace>();
        //trace.push(new ITSTrace(true)); // For root #document
        startTraversal = true;
    }

    private void clearInternalGlobalRules () {
        for ( int i=0; i<rules.size(); i++ ) {
            if ( rules.get(i).isInternal ) {
                rules.remove(i);
                i--;
            }
        }
    }

    private void processGlobalRules (long dataCategories) {
        try {
            // Compile any internal global rules
            clearInternalGlobalRules();

            if ( isHTML5 ) {
                // For HTML5 global rules are in scripts
                compileRulesInScripts(doc, docURI, true);
            }
            else { // Process normal in-document global rules
                compileRules(doc, docURI, true);
            }

            // Now apply the compiled rules
            for ( ITSRule rule : rules ) {
                // Check if we should apply this type of rule
                if ( (dataCategories & rule.ruleType) == 0 ) continue;

                // Get the selected nodes for the rule
                String data1;
                XPathExpression expr = xpath.compile(rule.selector);
                NodeList NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);

                // Apply the rule specific action on the selected nodes
                // Global rules are applies before local so they should
                // always override existing flag. override should be set to false
                // only for default attributes.
                for ( int i=0; i<NL.getLength(); i++ ) {
                    if ( rule.ruleType == IProcessor.DC_TRANSLATE ) {
                        setFlag(NL.item(i), FP_TRANSLATE, (rule.flag ? 'y' : 'n'), true);
                        // Set the hasTranslatabledattribute flag if it is an attribute node
                        if ( NL.item(i).getNodeType() == Node.ATTRIBUTE_NODE ) {
                            if ( rule.flag ) translatableAttributeRuleTriggered = true;
                        }
                        if ( rule.idValue != null ) { // For deprecated extension
                            setFlag(NL.item(i), FP_IDVALUE_DATA, resolveExpressionAsString(NL.item(i), rule.idValue), true);
                        }
                        // For deprecated extension
                        setFlag(NL.item(i), FP_PRESERVEWS, (rule.preserveWS ? 'y' : '?'), true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_DIRECTIONALITY ) {
                        setFlag(NL.item(i), FP_DIRECTIONALITY,
                                String.valueOf(rule.value).charAt(0), true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_WITHINTEXT ) {
                        setFlag(NL.item(i), FP_WITHINTEXT,
                                String.valueOf(rule.value).charAt(0), true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_TERMINOLOGY ) {
                        if ( !rule.flag ) {
                            setFlag(NL.item(i), FP_TERMINOLOGY, 'n', true);
                            continue;
                        }
                        // Else it is term='yes'
                        GenericAnnotations anns = rule.annotations;
                        GenericAnnotation ann = anns.getAnnotations(GenericAnnotationType.TERM).get(0);
                        // Get and resolve 'info/infoRef'
                        data1 = ann.getString(GenericAnnotationType.TERM_INFO);
                        if ( data1 != null ) {
                            if ( data1.startsWith(PTRFLAG) ) {
                                data1 = data1.substring(PTRFLAG.length());
                                boolean ref = data1.startsWith(REFFLAG);
                                if ( ref ) data1 = data1.substring(REFFLAG.length());
                                data1 = (ref ? GenericAnnotationType.REF_PREFIX : "")+resolvePointer(NL.item(i), data1);
                            }
                            ann.setString(GenericAnnotationType.TERM_INFO, data1);
                        }
                        // There is no confidence in the global rule
                        // Decorate the node with the resolved annotation data
                        setFlag(NL.item(i), FP_TERMINOLOGY, 'y', true);
                        setFlag(NL.item(i), FP_TERMINOLOGY_DATA, anns.toString(), true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_LOCNOTE ) {
                        boolean setFlag = true;
                        switch ( rule.infoType ) {
                            case INFOTYPE_TEXT:
                                setFlag(NL.item(i), FP_LOCNOTE_DATA, rule.info, true);
                                break;
                            case INFOTYPE_POINTER:
                                String value = resolvePointer(NL.item(i), rule.info);
                                if ( value != null ) setFlag(NL.item(i), FP_LOCNOTE_DATA, value, true);
                                else setFlag = false;
                                break;
                            case INFOTYPE_REF:
                                setFlag(NL.item(i), FP_LOCNOTE_DATA, GenericAnnotationType.REF_PREFIX+rule.info, true);
                                break;
                            case INFOTYPE_REFPOINTER:
                                value = resolvePointer(NL.item(i), rule.info);
                                if ( value != null ) setFlag(NL.item(i), FP_LOCNOTE_DATA, GenericAnnotationType.REF_PREFIX+value, true);
                                else  setFlag = false;
                                break;
                        }
                        if ( setFlag ) {
                            setFlag(NL.item(i), FP_LOCNOTE, (rule.flag ? 'a' : 'd'), true); // Type alert or description
                        }
                    }

                    else if ( rule.ruleType == IProcessor.DC_LANGINFO ) {
                        String value = resolvePointer(NL.item(i), rule.info);
                        if ( value != null ) {
                            setFlag(NL.item(i), FP_LANGINFO, 'y', true);
                            setFlag(NL.item(i), FP_LANGINFO_DATA, value, true);
                        }
                    }

                    else if ( rule.ruleType == IProcessor.DC_EXTERNALRES ) {
                        String value = resolvePointer(NL.item(i), rule.info);
                        if ( value != null ) {
                            setFlag(NL.item(i), FP_EXTERNALRES, 'y', true);
                            setFlag(NL.item(i), FP_EXTERNALRES_DATA, value, true);
                        }
                    }

                    else if ( rule.ruleType == IProcessor.DC_LOCFILTER ) {
                        setFlag(NL.item(i), FP_LOCFILTER, 'y', true);
                        setFlag(NL.item(i), FP_LOCFILTER_DATA, rule.info, true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_PRESERVESPACE ) {
                        // For new ITS 2.0 rule, but deprecated extension still supported in DC_PRESERVESPACE case
                        setFlag(NL.item(i), FP_PRESERVEWS, (rule.preserveWS ? 'y' : '?'), true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_IDVALUE ) {
                        // For new ITS 2.0 rule, but deprecated extension still supported in DC_TRANSLATE case
                        if ( rule.idValue != null ) {
                            setFlag(NL.item(i), FP_IDVALUE_DATA, resolveExpressionAsString(NL.item(i), rule.idValue), true);
                        }
                    }

                    else if ( rule.ruleType == IProcessor.DC_DOMAIN ) {
                        List<String> list = resolveExpressionAsList(NL.item(i), rule.info);
                        if ( list.isEmpty() ) continue;
                        // Map the values and build the final string
                        StringBuilder tmp = new StringBuilder();
                        List<String> values = null;
                        for ( String item : list ) {
                            values = fromDomainItemToValues(item, rule.map, values);
                        }
                        for ( String value : values ) {
                            if ( tmp.length() > 0 ) tmp.append(", ");
                            tmp.append(value);
                        }
                        setFlag(NL.item(i), FP_DOMAIN, 'y', true);
                        setFlag(NL.item(i), FP_DOMAIN_DATA, tmp.toString(), true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_TARGETPOINTER ) {
                        targetPointerRuleTriggered = true;
                        setFlag(NL.item(i), FP_TARGETPOINTER, 'y', true);
                        setFlag(NL.item(i), FP_TARGETPOINTER_DATA, rule.info, true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_LOCQUALITYISSUE ) {
                        GenericAnnotations anns = null;
                        String oriRef = data1 = rule.info;
                        if ( data1 != null ) {
                            if ( rule.infoType == INFOTYPE_REFPOINTER) {
                                oriRef = data1 = resolvePointer(NL.item(i), data1);
                            }
                            // Fetch the stand-off data
                            anns = fetchLocQualityStandoffData(data1, oriRef);
                        }
                        else {
                            // Not a stand-off annotation
                            GenericAnnotation ann = rule.annotations.getAnnotations(GenericAnnotationType.LQI).get(0);
                            anns = createLQIAnnotationSet();
                            GenericAnnotation upd = addIssueItem(anns);
                            // Get and resolve 'type'
                            data1 = ann.getString(GenericAnnotationType.LQI_TYPE);
                            if ( data1 != null ) {
                                if ( data1.startsWith(PTRFLAG) ) {
                                    data1 = resolvePointer(NL.item(i), data1.substring(PTRFLAG.length()));
                                }
                                upd.setString(GenericAnnotationType.LQI_TYPE, data1);
                            }
                            // Get and resolve 'comment'
                            data1 = ann.getString(GenericAnnotationType.LQI_COMMENT);
                            if ( data1 != null ) {
                                if ( data1.startsWith(PTRFLAG) ) {
                                    data1 = resolvePointer(NL.item(i), data1.substring(PTRFLAG.length()));
                                }
                                upd.setString(GenericAnnotationType.LQI_COMMENT, data1);
                            }
                            // Get and resolve 'severity'
                            data1  = ann.getString(GenericAnnotationType.LQI_SEVERITY);
                            if ( data1 != null ) {
                                if ( data1.startsWith(PTRFLAG) ) {
                                    data1 = resolvePointer(NL.item(i), data1.substring(PTRFLAG.length()));
                                }
                                // Convert the string to the Double value
                                upd.setDouble(GenericAnnotationType.LQI_SEVERITY, Double.parseDouble(data1));
                            }
                            // Get and resolve 'profile reference'
                            data1 = ann.getString(GenericAnnotationType.LQI_PROFILEREF);
                            if ( data1 != null ) {
                                if ( data1.startsWith(PTRFLAG) ) {
                                    data1 = resolvePointer(NL.item(i), data1.substring(PTRFLAG.length()));
                                }
                                upd.setString(GenericAnnotationType.LQI_PROFILEREF, data1);
                            }
                            // Get and resolve 'enabled'
                            data1 = ann.getString(GenericAnnotationType.LQI_ENABLED);
                            if ( data1 != null ) {
                                if ( data1.startsWith(PTRFLAG) ) {
                                    data1 = resolvePointer(NL.item(i), data1.substring(PTRFLAG.length()));
                                }
                                upd.setBoolean(GenericAnnotationType.LQI_ENABLED, data1.equals("yes"));
                            }
                        }
                        validateLQIData(anns);
                        // Decorate the node with the resolved annotation data
                        setFlag(NL.item(i), FP_LQISSUE, 'y', true);
                        setFlag(NL.item(i), FP_LQISSUE_DATA, anns.toString(), true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_PROVENANCE ) {
                        GenericAnnotations anns = null;
                        String oriRef = data1 = rule.info;
                        if ( data1 != null ) {
                            if ( rule.infoType == INFOTYPE_REFPOINTER) {
                                oriRef = data1 = resolvePointer(NL.item(i), data1);
                            }
                            // Fetch the stand-off data
                            anns = fetchProvenanceStandoffData(data1, oriRef);
                        }
                        else {
                            // Not a stand-off annotation
                            // There is no pointer to resove, we can re-use the same annotation
                            anns = rule.annotations;
                        }
                        // Decorate the node with the resolved annotation data
                        setFlag(NL.item(i), FP_PROVENANCE, 'y', true);
                        setFlag(NL.item(i), FP_PROVENANCE_DATA, anns.toString(), true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_TEXTANALYSIS ) {
//TODO: handle case where rule is applied to several nodes (pointers must be reset)
                        GenericAnnotations anns = rule.annotations;
                        GenericAnnotation ann = anns.getAnnotations(GenericAnnotationType.TA).get(0);
                        // Get and resolve 'classRef'
                        data1 = ann.getString(GenericAnnotationType.TA_CLASS);
                        if ( data1 != null ) {
                            if ( data1.startsWith(PTRFLAG) ) {
                                data1 = data1.substring(PTRFLAG.length());
                                boolean ref = data1.startsWith(REFFLAG);
                                if ( ref ) data1 = data1.substring(REFFLAG.length());
                                data1 = (ref ? GenericAnnotationType.REF_PREFIX : "")+resolvePointer(NL.item(i), data1);
                            }
                            ann.setString(GenericAnnotationType.TA_CLASS, data1);
                        }
                        // Get and resolve 'source'
                        data1 = ann.getString(GenericAnnotationType.TA_SOURCE);
                        if ( data1 != null ) {
                            if ( data1.startsWith(PTRFLAG) ) {
                                data1 = resolvePointer(NL.item(i), data1.substring(PTRFLAG.length()));
                            }
                            ann.setString(GenericAnnotationType.TA_SOURCE, data1);
                        }
                        // Get and resolve 'ident'
                        data1  = ann.getString(GenericAnnotationType.TA_IDENT);
                        if ( data1 != null ) {
                            if ( data1.startsWith(PTRFLAG) ) {
                                data1 = data1.substring(PTRFLAG.length());
                                boolean ref = data1.startsWith(REFFLAG);
                                if ( ref ) data1 = data1.substring(REFFLAG.length());
                                data1 = (ref ? GenericAnnotationType.REF_PREFIX : "")+resolvePointer(NL.item(i), data1);
                            }
                            ann.setString(GenericAnnotationType.TA_IDENT, data1);
                        }

                        // Confidence is not in global rules

                        // Decorate the node with the resolved annotation data
                        setFlag(NL.item(i), FP_TEXTANALYSIS, 'y', true);
                        setFlag(NL.item(i), FP_TEXTANALYSIS_DATA, anns.toString(), true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_ALLOWEDCHARS ) {
                        if ( rule.infoType == INFOTYPE_POINTER ) {
                            data1 = resolvePointer(NL.item(i), rule.info);
                        }
                        else { // Direct expression
                            data1 = rule.info;
                        }
                        setFlag(NL.item(i), FP_ALLOWEDCHARS, 'y', true);
                        setFlag(NL.item(i), FP_ALLOWEDCHARS_DATA, data1, true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_STORAGESIZE ) {
                        GenericAnnotation ann = rule.annotations.getFirstAnnotation(GenericAnnotationType.STORAGESIZE);
                        // Create the clone (pointers need to be re-computed, etc.)
                        GenericAnnotations anns = new GenericAnnotations();
                        GenericAnnotation upd = anns.add(GenericAnnotationType.STORAGESIZE);
                        // Get and resolve 'size'
                        data1 = ann.getString(GenericAnnotationType.STORAGESIZE_SIZE);
                        if ( data1.startsWith(PTRFLAG) ) {
                            data1 = resolvePointer(NL.item(i), data1.substring(PTRFLAG.length()));
                        }
                        upd.setInteger(GenericAnnotationType.STORAGESIZE_SIZE, Integer.parseInt(data1));
                        // Get and resolve 'encoding'
                        data1 = ann.getString(GenericAnnotationType.STORAGESIZE_ENCODING);
                        if ( data1.startsWith(PTRFLAG) ) {
                            data1 = data1.substring(PTRFLAG.length());
                            data1 = resolvePointer(NL.item(i), data1);
                        }
                        upd.setString(GenericAnnotationType.STORAGESIZE_ENCODING, data1);
                        // Copy the line-break info (it's never a pointer)
                        upd.setString(GenericAnnotationType.STORAGESIZE_LINEBREAK, ann.getString(GenericAnnotationType.STORAGESIZE_LINEBREAK));
                        // set the flag and data
                        setFlag(NL.item(i), FP_STORAGESIZE, 'y', true);
                        setFlag(NL.item(i), FP_STORAGESIZE_DATA, anns.toString(), true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_SUBFILTER ) {
                        setFlag(NL.item(i), FP_SUBFILTER, 'y', true);
                        setFlag(NL.item(i), FP_SUBFILTER_DATA, rule.info, true);
                    }

                    else if ( rule.ruleType == IProcessor.DC_MTCONFIDENCE ) {
                        setFlag(NL.item(i), FP_MTCONFIDENCE, 'y', true);
                        setFlag(NL.item(i), FP_MTCONFIDENCE_DATA, rule.info, true);
                    }

                }
            }
        }
        catch ( XPathExpressionException e ) {
            throw new OkapiException(e);
        }
    }

    /**
     * Adds an issue annotation to a given set and sets its default values.
     * @param anns the set where to add the annotation.
     * @return the annotation that has been added.
     */
    private GenericAnnotation addIssueItem (GenericAnnotations anns) {
        GenericAnnotation ann = anns.add(GenericAnnotationType.LQI);
        ann.setBoolean(GenericAnnotationType.LQI_ENABLED, true); // default
        return ann;
    }

    /**
     * Adds the values found in a domain original string to a common result list.
     * following the ITS 2.0 algorithm.
     * @param text the content of the original string.
     * @param map the map where the domain Mapping values are listed (can be null)
     * The left values of the list must be in lowercase.
     * @param list the list of previously existing resulting values (can be null)
     * @return the list of the resulting values.
     */
    private List<String> fromDomainItemToValues (String text,
                                                 Map<String, String> map,
                                                 List<String> list)
    {
        if ( list == null ) list = new ArrayList<String>();
        // Split the item on commas, and remove white spaces
        String[] parts = text.split(",", 0);
        for ( int i=0; i<parts.length; i++ ) {
            parts[i] = parts[i].trim();
            if ( parts[i].startsWith("'") || parts[i].startsWith("\"") ) {
                parts[i] = parts[i].substring(1);
            }
            if ( parts[i].endsWith("'") || parts[i].endsWith("\"") ) {
                parts[i] = parts[i].substring(0, parts[i].length()-1);
            }
        }
        for ( String part : parts ) {
            // If there is a map and the part is listed in it
            if (( map != null ) && map.containsKey(part) ) {
                part = map.get(part); // Use the mapped value
            }
            if ( !list.contains(part) ) {
                list.add(part);
            }
        }
        return list;
    }

//	/**
//	 * Converts a list of strings arguments to a single string that is delimited with end-of-group characters.
//	 * @param values the values to store. Null values are ok and mean no value.
//	 * @return a single string with all values.
//	 */
//	private String toSingleString (String ... values) {
//		StringBuilder data = new StringBuilder();
//		for ( String value : values ) {
//			if ( value == null ) data.append("\u001A");
//			if ( value != null ) data.append(value);
//			data.append("\u001D");
//		}
//		return data.toString();
//	}

//	private String[] fromSingleString (String data) {
//		String[] values = data.split("\u001D", -1);
//		for ( int i=0; i<values.length; i++ ) {
//			if ( values[i].equals("\u001A") ) values[i] = null;
//		}
//		return values;
//	}

    private void processLocalRules (long dataCategories) {
        XPathExpression expr;
        NodeList NL;
        Attr attr;
        try {
            if ( (dataCategories & IProcessor.DC_TRANSLATE) > 0 ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@translate");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":translate|//"+Namespaces.ITS_NS_PREFIX+":span/@translate");
                }
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "translateRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Validate the value
                    String value = attr.getValue();
                    if ( isHTML5 ) {
                        if ( value.isEmpty() ) value = "yes"; // In HTML5 "" == "yes"
                        else value = value.toLowerCase();
                    }
                    if (( !"yes".equals(value) ) && ( !"no".equals(value) )) {
                        throw new ITSException("Invalid value for 'translate'.");
                    }
                    // Set the flag
                    setFlag(attr.getOwnerElement(), FP_TRANSLATE, value.charAt(0), attr.getSpecified());
                    // No need to update hasTranslatabledattribute here because all nodes have to be elements in locale rules
                }
            }

            if ( (dataCategories & IProcessor.DC_DIRECTIONALITY) > 0 ) {
                if ( isHTML5 ) {
                    //TODO: Do we need more than this?
                    // Values for HTML5 for sir are ltr|rtl|auto (not rlo|lro)
                    expr = xpath.compile("//*/@dir");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":dir|//"+Namespaces.ITS_NS_PREFIX+":span/@dir");
                }
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "dirRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Set the flag on the others
                    int n = DIR_LTR;
                    if ( "ltr".equals(attr.getValue()) ) n = DIR_LTR;
                    else if ( "rtl".equals(attr.getValue()) ) n = DIR_RTL;
                    else if ( "lro".equals(attr.getValue()) ) n = DIR_RLO;
                    else if ( "rlo".equals(attr.getValue()) ) n = DIR_LRO;
                    else throw new ITSException("Invalid value for 'dir'.");
                    setFlag(attr.getOwnerElement(), FP_DIRECTIONALITY,
                            String.format("%d", n).charAt(0), attr.getSpecified());
                }
            }

            if ( (dataCategories & IProcessor.DC_TERMINOLOGY) > 0 ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-term");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":term|//"+Namespaces.ITS_NS_PREFIX+":span/@term");
                }
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "termRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Set the flag
                    boolean qualified = true;
                    String ns = attr.getOwnerElement().getNamespaceURI();
                    if ( !Util.isEmpty(ns) ) qualified = !ns.equals(Namespaces.ITS_NS_URI);
                    String[] values = retrieveTerminologyData(attr.getOwnerElement(), qualified, isHTML5);
                    if ( values[0] == null ) {
                        setFlag(attr.getOwnerElement(), FP_TERMINOLOGY, 'n', attr.getSpecified());
                        continue;
                    }
                    // Else: term is set. Convert the values into an annotation
                    GenericAnnotations anns = new GenericAnnotations();
                    GenericAnnotation ann = anns.add(GenericAnnotationType.TERM);
                    if ( values[1] != null ) ann.setString(GenericAnnotationType.TERM_INFO, values[1]);
                    if ( values[2] != null ) ann.setDouble(GenericAnnotationType.TERM_CONFIDENCE, Double.parseDouble(values[2]));
                    // Set the updated flags
                    setFlag(attr.getOwnerElement(), FP_TERMINOLOGY, 'y', attr.getSpecified());
                    setFlag(attr.getOwnerElement(), FP_TERMINOLOGY_DATA, anns.toString(), attr.getSpecified());
                }
            }

            if ( (dataCategories & IProcessor.DC_LOCNOTE) > 0 ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-loc-note|//*/@its-loc-note-ref");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":locNote|//"+Namespaces.ITS_NS_PREFIX+":span/@locNote"
                            +"|//*/@"+Namespaces.ITS_NS_PREFIX+":locNoteRef|//"+Namespaces.ITS_NS_PREFIX+":span/@locNoteRef");
                }
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                String localName;
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    localName = attr.getLocalName();
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "locNoteRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Retrieve the type of note
                    boolean qualified = true;
                    String ns = attr.getOwnerElement().getNamespaceURI();
                    if ( !Util.isEmpty(ns) ) qualified = !ns.equals(Namespaces.ITS_NS_URI);
                    boolean alert = retrieveLocNoteType(attr.getOwnerElement(), qualified, false, isHTML5);
                    // Set the flags/data
                    setFlag(attr.getOwnerElement(), FP_LOCNOTE, (alert ? 'a' : 'd'), attr.getSpecified());
                    if ( localName.equals("locNote") || localName.equals("its-loc-note") ) {
                        setFlag(attr.getOwnerElement(), FP_LOCNOTE_DATA, attr.getValue(), attr.getSpecified());
                    }
                    else if ( localName.equals("locNoteRef") || localName.equals("its-loc-note-ref") ) {
                        setFlag(attr.getOwnerElement(), FP_LOCNOTE_DATA,
                                GenericAnnotationType.REF_PREFIX+attr.getValue(), attr.getSpecified());
                    }
                }
            }

            if ( (dataCategories & IProcessor.DC_LANGINFO) > 0 ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@lang|//*/@xmlU00003Alang"); // correct: //*/@"+XML_NS_PREFIX+":lang");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.XML_NS_PREFIX+":lang");
                }
                //TODO: xml;lang takes precedence over lang in XHTML if both are on the same node
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    if ( isHTML5 ) {
                        if ( attr.getNodeName().equals("lang") ) { // It's lang, see if there is a xml;lang
                            //TODO: fix the getAttribute, this makes no sense!
                            //Attr xlang = attr.getOwnerElement().getAttributeNodeNS(XML_NS_URI, "lang");
                            Attr xlang = (Attr)attr.getOwnerElement().getAttributes().getNamedItem("xmlU00003Alang");
                            if ( xlang != null ) {
                                if ( !attr.getValue().equals(xlang.getValue()) ) {
                                    logger.warn("You should not have lang and xml:lang with different values ('{}' and '{}') on the same node.", attr.getValue(), xlang.getValue());
                                }
                                attr = xlang; // Use xml;lang, it overrides lang
                            }
                        }
                    }
                    // Set the flag
                    setFlag(attr.getOwnerElement(), FP_LANGINFO, 'y', attr.getSpecified());
                    setFlag(attr.getOwnerElement(), FP_LANGINFO_DATA,
                            attr.getValue(), attr.getSpecified());
                }
            }

            // Local withinText attribute (ITS 2.0 only)
            if (( (dataCategories & IProcessor.DC_WITHINTEXT) > 0 ) && isVersion2() ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-within-text");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":withinText|//"+Namespaces.ITS_NS_PREFIX+":span/@withinText");
                }
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "withinTextRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Set the flag
                    String value = attr.getValue();
                    if ( isHTML5 ) value = value.toLowerCase();
                    char ch;
                    if ( "no".equals(value) ) ch='0'; // WITHINTEXT_NO;
                    else if ( "yes".equals(value) ) ch='1'; // WITHINTEXT_YES;
                    else if ( "nested".equals(value) ) ch='2'; // WITHINTEXT_NESTED;
                    else throw new ITSException("Invalid value for 'withinText'.");
                    setFlag(attr.getOwnerElement(), FP_WITHINTEXT, ch, attr.getSpecified());
                }
            }

            // xml:space always applied
            expr = xpath.compile("//*/@"+Namespaces.XML_NS_PREFIX+":space");
            NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
            for ( int i=0; i<NL.getLength(); i++ ) {
                attr = (Attr)NL.item(i);
                // Validate the value
                String value = attr.getValue();
                if (( !"preserve".equals(value) ) && ( !"default".equals(value) )) {
                    throw new ITSException("Invalid value for 'xml:space'.");
                }
                // Set the flag
                setFlag(attr.getOwnerElement(), FP_PRESERVEWS,
                        ("preserve".equals(value) ? 'y' : 'n'), attr.getSpecified());
            }

            // its:annotatorsRef always applied
            if ( isHTML5 ) {
                expr = xpath.compile("//*/@its-annotators-ref");
            }
            else {
                expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":annotatorsRef|//"+Namespaces.ITS_NS_PREFIX+":span/@annotatorsRef");
            }
            NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
            for ( int i=0; i<NL.getLength(); i++ ) {
                attr = (Attr)NL.item(i);
                // Validate the value
                String value = attr.getValue();
                // Validate the values
                Map<String, String> map = ITSContent.annotatorsRefToMap(value);
                for ( String dc : map.keySet() ) {
                    validateDataCategoryNames(dc);
                }
                // Set the flag
                setFlag(attr.getOwnerElement(), FP_ANNOTATORSREF,
                        (value!=null ? 'y' : '?'), attr.getSpecified());
                setFlag(attr.getOwnerElement(), FP_ANNOTATORSREF_DATA,
                        value, attr.getSpecified());
            }

            // Locale filter
            if (( (dataCategories & IProcessor.DC_LOCFILTER) > 0 ) && isVersion2() ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-locale-filter-list");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":localeFilterList|//"+Namespaces.ITS_NS_PREFIX+":span/@localeFilterList");
                }
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "localeFilterRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Set the flag
                    boolean qualified = true;
                    String ns = attr.getOwnerElement().getNamespaceURI();
                    if ( !Util.isEmpty(ns) ) qualified = !ns.equals(Namespaces.ITS_NS_URI);
                    String value = retrieveLocaleFilterList(attr.getOwnerElement(), qualified, isHTML5);
                    setFlag(attr.getOwnerElement(), FP_LOCFILTER, 'y', attr.getSpecified());
                    setFlag(attr.getOwnerElement(), FP_LOCFILTER_DATA,
                            value, attr.getSpecified());
                }
            }

            // xml:id always applied
            expr = xpath.compile("//*/@"+Namespaces.XML_NS_PREFIX+":id");
            NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
            for ( int i=0; i<NL.getLength(); i++ ) {
                attr = (Attr)NL.item(i);
                String value = attr.getValue();
                if (( value != null ) && ( value.length() > 0 )) {
                    setFlag(attr.getOwnerElement(), FP_IDVALUE_DATA,
                            value, attr.getSpecified());
                }
            }

            // Localization quality issue
            if (( (dataCategories & IProcessor.DC_LOCQUALITYISSUE) > 0 ) && isVersion2() ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-loc-quality-issue-type|//*/@its-loc-quality-issue-comment|//*/@its-loc-quality-issues-ref");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":locQualityIssueType|//"+Namespaces.ITS_NS_PREFIX+":span/@locQualityIssueType"
                            +"|//*/@"+Namespaces.ITS_NS_PREFIX+":locQualityIssueComment|//"+Namespaces.ITS_NS_PREFIX+":span/@locQualityIssueComment"
                            +"|//*/@"+Namespaces.ITS_NS_PREFIX+":locQualityIssuesRef|//"+Namespaces.ITS_NS_PREFIX+":span/@locQualityIssuesRef");
                }

                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "locQualityIssueRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Set the flag
                    boolean qualified = true;
                    String ns = attr.getOwnerElement().getNamespaceURI();
                    if ( !Util.isEmpty(ns) ) qualified = !ns.equals(Namespaces.ITS_NS_URI);
                    String[] values = retrieveLocQualityIssueData(attr.getOwnerElement(), qualified, isHTML5);
                    // Convert the values into an annotation
                    GenericAnnotations anns = null;
                    if ( values[0] != null ) { // stand-off reference
                        // Fetch the stand-off data
                        anns = fetchLocQualityStandoffData(values[0], values[0]);
                    }
                    else { // Not an stand-off reference
                        anns = createLQIAnnotationSet();
                        GenericAnnotation ann = addIssueItem(anns);
                        if ( values[1] != null ) ann.setString(GenericAnnotationType.LQI_TYPE, values[1]);
                        if ( values[2] != null ) ann.setString(GenericAnnotationType.LQI_COMMENT, values[2]);
                        if ( values[3] != null ) ann.setDouble(GenericAnnotationType.LQI_SEVERITY, Double.parseDouble(values[3]));
                        if ( values[4] != null ) ann.setString(GenericAnnotationType.LQI_PROFILEREF, values[4]);
                        if ( values[5] != null ) ann.setBoolean(GenericAnnotationType.LQI_ENABLED, values[5].equals("yes"));
                    }
                    // Set the updated flags
                    validateLQIData(anns);
                    setFlag(attr.getOwnerElement(), FP_LQISSUE, 'y', attr.getSpecified());
                    setFlag(attr.getOwnerElement(), FP_LQISSUE_DATA, anns.toString(), attr.getSpecified());
                }
            }

            // provenance
            if ( isVersion2() && ( (dataCategories & IProcessor.DC_PROVENANCE) > 0 )) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-person|//*/@its-org|//*/@its-tool"
                            + "|//*/@its-person-ref|//*/@its-org-ref|//*/@its-tool-ref"
                            + "|//*/@its-rev-person|//*/@its-rev-org|//*/@its-rev-tool"
                            + "|//*/@its-rev-person-ref|//*/@its-rev-org-ref|//*/@its-rev-tool-ref"
                            + "|//*/@its-prov-ref|//*/@its-provenance-records-ref");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":person|//"+Namespaces.ITS_NS_PREFIX+":span/@person"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":personRef|//"+Namespaces.ITS_NS_PREFIX+":span/@personRef"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":org|//"+Namespaces.ITS_NS_PREFIX+":span/@org"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":orgRef|//"+Namespaces.ITS_NS_PREFIX+":span/@orgRef"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":tool|//"+Namespaces.ITS_NS_PREFIX+":span/@tool"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":toolRef|//"+Namespaces.ITS_NS_PREFIX+":span/@toolRef"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":revPerson|//"+Namespaces.ITS_NS_PREFIX+":span/@revPerson"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":revPersonRef|//"+Namespaces.ITS_NS_PREFIX+":span/@revPersonRef"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":revOrg|//"+Namespaces.ITS_NS_PREFIX+":span/@revOrg"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":revOrgRef|//"+Namespaces.ITS_NS_PREFIX+":span/@revOrgRef"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":revTool|//"+Namespaces.ITS_NS_PREFIX+":span/@revTool"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":revToolRef|//"+Namespaces.ITS_NS_PREFIX+":span/@revToolRef"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":provRef|//"+Namespaces.ITS_NS_PREFIX+":span/@provRef"
                            + "|//*/@"+Namespaces.ITS_NS_PREFIX+":provenanceRecordsRef|//"+Namespaces.ITS_NS_PREFIX+":span/@provenanceRecordsRef");
                }

                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "provRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Set the flag
                    boolean qualified = true;
                    String ns = attr.getOwnerElement().getNamespaceURI();
                    if ( !Util.isEmpty(ns) ) qualified = !ns.equals(Namespaces.ITS_NS_URI);
                    String[] values = retrieveProvenanceData(attr.getOwnerElement(), qualified, isHTML5);
                    // Convert the values into an annotation
                    GenericAnnotations anns = null;
                    if ( values[0] != null ) { // stand-off reference
                        // Fetch the stand-off data
                        anns = fetchProvenanceStandoffData(values[0], values[0]);
                    }
                    else { // Not an stand-off reference
                        anns = createProvenanceAnnotationSet();
                        GenericAnnotation ann = anns.add(GenericAnnotationType.PROV);
                        if ( values[1] != null ) ann.setString(GenericAnnotationType.PROV_PERSON, values[1]);
                        if ( values[2] != null ) ann.setString(GenericAnnotationType.PROV_ORG, values[2]);
                        if ( values[3] != null ) ann.setString(GenericAnnotationType.PROV_TOOL, values[3]);
                        if ( values[4] != null ) ann.setString(GenericAnnotationType.PROV_REVPERSON, values[4]);
                        if ( values[5] != null ) ann.setString(GenericAnnotationType.PROV_REVORG, values[5]);
                        if ( values[6] != null ) ann.setString(GenericAnnotationType.PROV_REVTOOL, values[6]);
                        if ( values[7] != null ) ann.setString(GenericAnnotationType.PROV_PROVREF, values[7]);
                    }
                    // Set the updated flags
                    setFlag(attr.getOwnerElement(), FP_PROVENANCE, 'y', attr.getSpecified());
                    setFlag(attr.getOwnerElement(), FP_PROVENANCE_DATA, anns.toString(), attr.getSpecified());
                }
            }

            // Text Analysis
            if (( (dataCategories & IProcessor.DC_TEXTANALYSIS) > 0 ) && isVersion2() ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-ta-class-ref|//*/@its-ta-ident|//*/@its-ta-ident-ref");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":taClassRef|//"+Namespaces.ITS_NS_PREFIX+":span/@taClassRef"
                            +"|//*/@"+Namespaces.ITS_NS_PREFIX+":taIdent|//"+Namespaces.ITS_NS_PREFIX+":span/@taIdent"
                            +"|//*/@"+Namespaces.ITS_NS_PREFIX+":taIdentRef|//"+Namespaces.ITS_NS_PREFIX+":span/@taIdentRef");
                }

                // This may catch elements twice (e.g. if the have class-ref and ident-ref)
                // So the work may be duplicated
//TODO: Remove duplicated items from the list before processing
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "textAnalysisRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Set the flag
                    boolean qualified = true;
                    String ns = attr.getOwnerElement().getNamespaceURI();
                    if ( !Util.isEmpty(ns) ) qualified = !ns.equals(Namespaces.ITS_NS_URI);
                    String[] values = retrieveTextAnalysisData(attr.getOwnerElement(), qualified, isHTML5);
                    // Convert the values into an annotation
                    GenericAnnotations anns = new GenericAnnotations();
                    GenericAnnotation ann = anns.add(GenericAnnotationType.TA);
                    if ( values[0] != null ) ann.setString(GenericAnnotationType.TA_CLASS, values[0]);
                    if ( values[1] != null ) ann.setString(GenericAnnotationType.TA_SOURCE, values[1]);
                    if ( values[2] != null ) ann.setString(GenericAnnotationType.TA_IDENT, values[2]);
                    if ( values[3] != null ) ann.setDouble(GenericAnnotationType.TA_CONFIDENCE, Double.parseDouble(values[3]));
                    // Set the updated flags
                    setFlag(attr.getOwnerElement(), FP_TEXTANALYSIS, 'y', attr.getSpecified());
                    setFlag(attr.getOwnerElement(), FP_TEXTANALYSIS_DATA, anns.toString(), attr.getSpecified());
                }
            }

            // Localization Quality Rating
            if (( (dataCategories & IProcessor.DC_LOCQUALITYRATING) > 0 ) && isVersion2() ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-loc-quality-rating-score|//*/@its-loc-quality-rating-vote");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":locQualityRatingScore|//"+Namespaces.ITS_NS_PREFIX+":span/@locQualityRatingScore"
                            +"|//*/@"+Namespaces.ITS_NS_PREFIX+":locQualityRatingVote|//"+Namespaces.ITS_NS_PREFIX+":span/@locQualityRatingVote");
                }

                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // No irrelevant nodes to skip as there is no global rules
                    // Next: Set the flag
                    boolean qualified = true;
                    String ns = attr.getOwnerElement().getNamespaceURI();
                    if ( !Util.isEmpty(ns) ) qualified = !ns.equals(Namespaces.ITS_NS_URI);
                    String[] values = retrieveLocQualityRatingData(attr.getOwnerElement(), qualified, isHTML5);
                    // Convert the values into an annotation
                    GenericAnnotations anns = new GenericAnnotations();
                    GenericAnnotation ann = anns.add(GenericAnnotationType.LQR);
                    if ( values[0] != null ) ann.setDouble(GenericAnnotationType.LQR_SCORE, Double.parseDouble(values[0]));
                    if ( values[1] != null ) ann.setInteger(GenericAnnotationType.LQR_VOTE, Integer.parseInt(values[1]));
                    if ( values[2] != null ) ann.setDouble(GenericAnnotationType.LQR_SCORETHRESHOLD, Double.parseDouble(values[2]));
                    if ( values[3] != null ) ann.setInteger(GenericAnnotationType.LQR_VOTETHRESHOLD, Integer.parseInt(values[3]));
                    if ( values[4] != null ) ann.setString(GenericAnnotationType.LQR_PROFILEREF, values[4]);
                    // Set the updated flags
                    setFlag(attr.getOwnerElement(), FP_LQRATING, 'y', attr.getSpecified());
                    setFlag(attr.getOwnerElement(), FP_LQRATING_DATA, anns.toString(), attr.getSpecified());
                }
            }

            // Allowed characters
            if (( (dataCategories & IProcessor.DC_ALLOWEDCHARS) > 0 ) && isVersion2() ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-allowed-characters");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":allowedCharacters|//"+Namespaces.ITS_NS_PREFIX+":span/@allowedCharacters");
                }
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "allowedCharactersRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Set the flag
                    boolean qualified = true;
                    String ns = attr.getOwnerElement().getNamespaceURI();
                    if ( !Util.isEmpty(ns) ) qualified = !ns.equals(Namespaces.ITS_NS_URI);
                    String value = retrieveAllowedCharsData(attr.getOwnerElement(), qualified, isHTML5);
                    // Set the updated flags
                    setFlag(attr.getOwnerElement(), FP_ALLOWEDCHARS, 'y', attr.getSpecified());
                    setFlag(attr.getOwnerElement(), FP_ALLOWEDCHARS_DATA, value, attr.getSpecified());
                }
            }

            // Storage size
            if (( (dataCategories & IProcessor.DC_STORAGESIZE) > 0 ) && isVersion2() ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-storage-size");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":storageSize|//"+Namespaces.ITS_NS_PREFIX+":span/@storageSize");
                }
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "storageSizeRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Set the flag
                    boolean qualified = true;
                    String ns = attr.getOwnerElement().getNamespaceURI();
                    if ( !Util.isEmpty(ns) ) qualified = !ns.equals(Namespaces.ITS_NS_URI);
                    String[] values = retrieveStorageSizeData(attr.getOwnerElement(), qualified, isHTML5);
                    // Set the updated flags
                    GenericAnnotations anns = new GenericAnnotations(
                            new GenericAnnotation(GenericAnnotationType.STORAGESIZE,
                                    GenericAnnotationType.STORAGESIZE_SIZE, Integer.parseInt(values[0]),
                                    GenericAnnotationType.STORAGESIZE_ENCODING, values[1],
                                    GenericAnnotationType.STORAGESIZE_LINEBREAK, values[2]
                            )
                    );
                    setFlag(attr.getOwnerElement(), FP_STORAGESIZE, 'y', attr.getSpecified());
                    setFlag(attr.getOwnerElement(), FP_STORAGESIZE_DATA, anns.toString(), attr.getSpecified());
                }
            }

            // MT Confidence
            if (( (dataCategories & IProcessor.DC_MTCONFIDENCE) > 0 ) && isVersion2() ) {
                if ( isHTML5 ) {
                    expr = xpath.compile("//*/@its-mt-confidence");
                }
                else {
                    expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":mtConfidence|//"+Namespaces.ITS_NS_PREFIX+":span/@mtConfidence");
                }
                NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                for ( int i=0; i<NL.getLength(); i++ ) {
                    attr = (Attr)NL.item(i);
                    // Skip irrelevant nodes
                    if ( Namespaces.ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
                            && "mtConfidenceRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
                    // Set the flag
                    boolean qualified = true;
                    String ns = attr.getOwnerElement().getNamespaceURI();
                    if ( !Util.isEmpty(ns) ) qualified = !ns.equals(Namespaces.ITS_NS_URI);
                    // Get, validate and set the value
                    String value = retrieveMtconfidence(attr.getOwnerElement(), qualified, isHTML5);
                    if ( value != null ) {
                        setFlag(attr.getOwnerElement(), FP_MTCONFIDENCE, 'y', attr.getSpecified());
                        setFlag(attr.getOwnerElement(), FP_MTCONFIDENCE_DATA, value, attr.getSpecified());
                    }
                }
            }

//			// sub filter
//			if ( (dataCategories & IProcessor.DC_SUBFILTER) > 0 ) {
//				if ( isHTML5 ) {
//					expr = xpath.compile("//*/@data-itsx-sub-filter");
//				}
//				else {
//					// Not on inline its:span
//					ERROR
//					expr = xpath.compile("//*/@"+ITSX_NS_PREFIX+":subFilter");
//				}
//				NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
//				for ( int i=0; i<NL.getLength(); i++ ) {
//					attr = (Attr)NL.item(i);
//					// Skip irrelevant nodes
//					if ( ITSX_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
//						&& "subFilterRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
//					// Set the flag
//					boolean qualified = true;
//					String ns = attr.getOwnerElement().getNamespaceURI();
//					if ( !Util.isEmpty(ns) ) qualified = !ns.equals(ITSX_NS_URI);
//					String value = retrieveSubFilter(attr.getOwnerElement(), qualified, isHTML5);
//					setFlag(attr.getOwnerElement(), FP_SUBFILTER, 'y', attr.getSpecified());
//					setFlag(attr.getOwnerElement(), FP_SUBFILTER_DATA,
//						value, attr.getSpecified());
//				}
//			}

        }
        catch ( XPathExpressionException e ) {
            throw new OkapiException(e);
        }
    }

    private boolean validateDouble (String value,
                                    Double minimum,
                                    Double maximum,
                                    String name)
    {
        try {
            Double f = Double.parseDouble(value);
            if (( f < minimum ) || ( f > maximum )) {
                logger.error("Invalid value for {}: {}. It should be between [{} and {}]", name, value, minimum, maximum);
            }
            return true;
        }
        catch ( NullPointerException e ) {
            logger.error("Not value defined for '{}'.", name);
            return false;
        }
        catch ( NumberFormatException e ) {
            logger.error("Invalid rational value for {}: {}", name, value);
            return false;
        }
    }

    private void validateLQIData (GenericAnnotations anns) {
        for ( GenericAnnotation ann : anns ) {
            if ( !ann.getType().equals(GenericAnnotationType.LQI) ) continue;
            String type = ann.getString(GenericAnnotationType.LQI_TYPE);
            if (( type != null ) && type.equals("uncategorized") ) {
                if ( Util.isEmpty(ann.getString(GenericAnnotationType.LQI_COMMENT)) ) {
                    logger.error("Issue of type '{}' must have a comment.", type);
                }
            }
        }
    }

    private void validateDataCategoryNames (String dc) {
        if ( Util.isEmpty(dc) || ( ("translate|localization-note|terminology|directionality|ruby|language-information|elements-within-text|"
                + "domain|text-analysis|locale-filter|provenance|external-resource|target-pointer|id-value|preserve-space|"
                + "localization-quality-issue|localization-quality-rating|mt-confidence|allowed-characters|storage-size").indexOf(dc)==-1 )) {
            // Log an error, but don't stop the process
            logger.error("Invalid data category name: '{}'.", dc);
        }
    }

    /**
     * Retrieve the final list to use for a locale filter data category
     * @param elem the element where the attributes are defined.
     * @param qualified true if the attributes are expected to be qualified (local markup)
     * @return the list of the locale, with an optional '!' pefix (when the type is 'exclude').
     */
    private String retrieveLocaleFilterList (Element elem,
                                             boolean qualified,
                                             boolean useHTML5)
    {
        String[] data = new String[2];
        if ( useHTML5 ) {
            data[0] = elem.getAttribute("its-locale-filter-list").trim();
            if ( elem.hasAttribute("its-locale-filter-type") )
                data[1] = elem.getAttribute("its-locale-filter-type").toLowerCase();
        }
        else if ( qualified ) { // Locally
            data[0] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "localeFilterList").trim();
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "localeFilterType") )
                data[1] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "localeFilterType");
        }
        else { // Inside a global rule
            data[0] = elem.getAttribute("localeFilterList").trim();
            if ( elem.hasAttribute("localeFilterType") )
                data[1] = elem.getAttribute("localeFilterType");
        }

        if ( data[1] == null ) data[1] = "include"; // Default
        else if ( !data[1].equals("include") && !data[1].equals("exclude") ) {
            logger.error("Invalid locale filter type '{}'.", data[1]);
            return "*"; // Default if an error occurs.
        }
        // Return with optional prefix for exclude
        if ( data[1].equals("exclude") ) return "!"+data[0];
        else return data[0];
    }

    /**
     * Retrieves the local values for Terminology.
     * @param elem the element where to get the data.
     * @param qualified true if the attributes are expected to be qualified.
     * @param useHTML5 true if this is in HTML.
     * @return an array of the value: term (null means no), info, confidence.
     */
    private String[] retrieveTerminologyData (Element elem,
                                              boolean qualified,
                                              boolean useHTML5)
    {
        String[] data = new String[3];
        if ( useHTML5 ) {
            if ( elem.hasAttribute("its-term") )
                data[0] = elem.getAttribute("its-term").toLowerCase();
            if ( elem.hasAttribute("its-term-info-ref") )
                data[1] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("its-term-info-ref");
            if ( elem.hasAttribute("its-term-confidence") )
                data[2] = elem.getAttribute("its-term-confidence");
        }
        else if ( qualified ) {
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "term") )
                data[0] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "term");
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "termInfoRef") )
                data[1] = GenericAnnotationType.REF_PREFIX+elem.getAttributeNS(Namespaces.ITS_NS_URI, "termInfoRef");
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "termConfidence") )
                data[2] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "termConfidence");
        }
        else {
            if ( elem.hasAttribute("term") )
                data[0] = elem.getAttribute("term");
            if ( elem.hasAttribute("termInfoRef") )
                data[1] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("termInfoRef");
            if ( elem.hasAttribute("termConfidence") )
                data[2] = elem.getAttribute("termConfidence");
        }
        if (( data[0] != null ) && !data[0].equals("yes") ) {
            data[0] = null;
        }
        return data;
    }

    private String retrieveSubFilter (Element elem,
                                      boolean qualified,
                                      boolean useHTML5)
    {
        if ( useHTML5 ) {
            return elem.getAttribute("data-itsx-sub-filter").trim();
        }
        else if ( qualified ) { // Locally
            return elem.getAttributeNS(Namespaces.ITSX_NS_URI, "subFilter").trim();
        }
        else { // Inside a global rule
            return elem.getAttribute("subFilter").trim();
        }
    }

    private boolean retrieveLocNoteType (Element elem,
                                         boolean qualified,
                                         boolean required,
                                         boolean useHTML5)
    {
        String type;
        if ( useHTML5 ) {
            type = elem.getAttribute("its-loc-note-type").toLowerCase();
        }
        else if ( qualified ) {
            type = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locNoteType");
        }
        else {
            type = elem.getAttribute("locNoteType");
        }

        if ( type.isEmpty() && required ) {
            throw new ITSException(String.format("%s attribute missing.", (isHTML5 ? "its-loc-note-type" : "locNoteType")));
        }
        else if ( type.equals("alert") ) {
            return true; // alert
        }
        else if ( !type.equals("description") && !type.isEmpty() ) {
            throw new ITSException(String.format("Invalide value '%s' for localozation note type.", type));
        }
        return false; // description
    }

    private String retrieveAllowedCharsData (Element elem,
                                             boolean qualified,
                                             boolean useHTML5)
    {
        String regex = null;
        if ( useHTML5 ) {
            if ( elem.hasAttribute("its-allowed-characters") )
                regex = elem.getAttribute("its-allowed-characters");
        }
        else if ( qualified ) {
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "allowedCharacters") )
                regex = elem.getAttributeNS(Namespaces.ITS_NS_URI, "allowedCharacters");
        }
        else {
            if ( elem.hasAttribute("allowedCharacters") )
                regex = elem.getAttribute("allowedCharacters");
        }

        // Verify it uses the allowed syntax
        if ( regex != null ) {
            if ( validAllowedChars == null ) {
                validAllowedChars = Pattern.compile(VALIDALLOWEDCHARS);
            }
            if ( !validAllowedChars.matcher(regex).matches() ) {
                logger.error("The pattern '{}' is not valid for the Allowed Characters data category.", regex);
            }
        }

        // Return it
        return regex;
    }

    private String retrieveMtconfidence (Element elem,
                                         boolean qualified,
                                         boolean useHTML5)
    {
        String value = null;
        String name = "mtConfidence";
        if ( useHTML5 ) {
            name = "its-mt-confidence";
            if ( elem.hasAttribute(name) )
                value = elem.getAttribute(name);
        }
        else if ( qualified ) {
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, name) )
                value = elem.getAttributeNS(Namespaces.ITS_NS_URI, name);
        }
        else {
            if ( elem.hasAttribute(name) )
                value = elem.getAttribute(name);
        }
        if ( validateDouble(value, 0.0, 1.0, name) ) {
            return value;
        }
        return null; // No or bad value
    }

    private String[] retrieveStorageSizeData (Element elem,
                                              boolean qualified,
                                              boolean useHTML5)
    {
        String[] data = new String[3];
        if ( useHTML5 ) {
            if ( elem.hasAttribute("its-storage-size") )
                data[0] = elem.getAttribute("its-storage-size");
            if ( elem.hasAttribute("its-storage-encoding") )
                data[1] = elem.getAttribute("its-storage-encoding");
            if ( elem.hasAttribute("its-line-break-type") )
                data[2] = elem.getAttribute("its-line-break-type").toLowerCase();
        }
        else if ( qualified ) {
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "storageSize") )
                data[0] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "storageSize");
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "storageEncoding") )
                data[1] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "storageEncoding");
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "lineBreakType") )
                data[2] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "lineBreakType");
        }
        else {
            if ( elem.hasAttribute("storageSize") )
                data[0] = elem.getAttribute("storageSize");
            if ( elem.hasAttribute("storageEncoding") )
                data[1] = elem.getAttribute("storageEncoding");
            if ( elem.hasAttribute("lineBreakType") )
                data[2] = elem.getAttribute("lineBreakType");
        }
        // Defaults
        if ( data[1] == null ) data[1] = "UTF-8";
        if ( data[2] == null ) data[2] = "lf";
        return data;
    }

    private XPath createXPath () {
        XPath xpath = xpFact.newXPath();
        NSContextManager nsc = new NSContextManager();
        nsc.addNamespace(Namespaces.ITS_NS_PREFIX, Namespaces.ITS_NS_URI);
        nsc.addNamespace(Namespaces.HTML_NS_PREFIX, Namespaces.HTML_NS_URI);
        nsc.addNamespace(Namespaces.XML_NS_PREFIX, Namespaces.XML_NS_URI);
        xpath.setNamespaceContext(nsc);
        return xpath;
    }

    /**
     * Retrieves the non-pointer information of the Localization Quality issue data category.
     * @param elem the element where to get the data.
     * @param qualified true if the attributes are expected to be qualified.
     * @return an array of the value: issues reference, type, comment, severity, profile reference, enabled.
     */
    private String[] retrieveLocQualityIssueData (Element elem,
                                                  boolean qualified,
                                                  boolean useHTML5)
    {
        String[] data = new String[6];

        if ( useHTML5 ) {
            if ( elem.hasAttribute("its-loc-quality-issues-ref") )
                data[0] = elem.getAttribute("its-loc-quality-issues-ref");
            if ( elem.hasAttribute("its-loc-quality-issue-type") )
                data[1] = elem.getAttribute("its-loc-quality-issue-type").toLowerCase();
            if ( elem.hasAttribute("its-loc-quality-issue-comment") )
                data[2] = elem.getAttribute("its-loc-quality-issue-comment");
            if ( elem.hasAttribute("its-loc-quality-issue-severity") )
                data[3] = elem.getAttribute("its-loc-quality-issue-severity");
            if ( elem.hasAttribute("its-loc-quality-issue-profile-ref") )
                data[4] = elem.getAttribute("its-loc-quality-issue-profile-ref");
            if ( elem.hasAttribute("its-loc-quality-issue-enabled") )
                data[5] = elem.getAttribute("its-loc-quality-issue-enabled").toLowerCase();
            else
                data[5] = "yes"; // Default
        }
        else if ( qualified ) {
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssuesRef") )
                data[0] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssuesRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssueType") )
                data[1] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssueType");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssueComment") )
                data[2] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssueComment");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssueSeverity") )
                data[3] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssueSeverity");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssueProfileRef") )
                data[4] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssueProfileRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssueEnabled") )
                data[5] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityIssueEnabled");
            else
                data[5] = "yes"; // Default
        }
        else {
            if ( elem.hasAttribute("locQualityIssuesRef") )
                data[0] = elem.getAttribute("locQualityIssuesRef");

            if ( elem.hasAttribute("locQualityIssueType") )
                data[1] = elem.getAttribute("locQualityIssueType");

            if ( elem.hasAttribute("locQualityIssueComment") )
                data[2] = elem.getAttribute("locQualityIssueComment");

            if ( elem.hasAttribute("locQualityIssueSeverity") )
                data[3] = elem.getAttribute("locQualityIssueSeverity");

            if ( elem.hasAttribute("locQualityIssueProfileRef") )
                data[4] = elem.getAttribute("locQualityIssueProfileRef");

            if ( elem.hasAttribute("locQualityIssueEnabled") )
                data[5] = elem.getAttribute("locQualityIssueEnabled");
            else
                data[5] = "yes"; // Default
        }

        // Do not check for complete set of required characters
        // This because we could have a global pointer that defines a non-native way to get the data

        return data;
    }

    /**
     * Retrieves the non-pointer information of the Provenance data category.
     * @param elem the element where to get the data.
     * @param qualified true if the attributes are expected to be qualified.
     * @return an array of the value: records reference, person, org, tool, revPerson, revOrg, revTool, provRef
     */
    private String[] retrieveProvenanceData (Element elem,
                                             boolean qualified,
                                             boolean useHTML5)
    {
        String[] data = new String[8];

        if ( useHTML5 ) {
            if ( elem.hasAttribute("its-provenance-records-ref") )
                data[0] = elem.getAttribute("its-provenance-records-ref");

            if ( elem.hasAttribute("its-person") )
                data[1] = elem.getAttribute("its-person");
            else if ( elem.hasAttribute("its-person-ref") )
                data[1] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("its-person-ref");

            if ( elem.hasAttribute("its-org") )
                data[2] = elem.getAttribute("its-org");
            else if ( elem.hasAttribute("its-org-ref") )
                data[2] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("its-org-ref");

            if ( elem.hasAttribute("its-tool") )
                data[3] = elem.getAttribute("its-tool");
            else if ( elem.hasAttribute("its-tool-ref") )
                data[3] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("its-tool-ref");

            if ( elem.hasAttribute("its-rev-person") )
                data[4] = elem.getAttribute("its-rev-person");
            else if ( elem.hasAttribute("its-rev-person-ref") )
                data[4] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("its-rev-person-ref");

            if ( elem.hasAttribute("its-rev-org") )
                data[5] = elem.getAttribute("its-rev-org");
            else if ( elem.hasAttribute("its-rev-org-ref") )
                data[5] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("its-rev-org-ref");

            if ( elem.hasAttribute("its-rev-tool") )
                data[6] = elem.getAttribute("its-rev-tool");
            else if ( elem.hasAttribute("its-rev-tool-ref") )
                data[6] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("its-rev-tool-ref");

            if ( elem.hasAttribute("its-prov-ref") )
                data[7] = elem.getAttribute("its-prov-ref");
        }
        else if ( qualified ) {
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "provenanceRecordsRef") )
                data[0] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "provenanceRecordsRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "person") )
                data[1] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "person");
            else if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "personRef") )
                data[1] = GenericAnnotationType.REF_PREFIX+elem.getAttributeNS(Namespaces.ITS_NS_URI, "personRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "org") )
                data[2] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "org");
            else if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "orgRef") )
                data[2] = GenericAnnotationType.REF_PREFIX+elem.getAttributeNS(Namespaces.ITS_NS_URI, "orgRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "tool") )
                data[3] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "tool");
            else if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "toolRef") )
                data[3] = GenericAnnotationType.REF_PREFIX+elem.getAttributeNS(Namespaces.ITS_NS_URI, "toolRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "revPerson") )
                data[4] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "revPerson");
            else if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "revPersonRef") )
                data[4] = GenericAnnotationType.REF_PREFIX+elem.getAttributeNS(Namespaces.ITS_NS_URI, "revPersonRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "revOrg") )
                data[5] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "revOrg");
            else if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "revOrgRef") )
                data[5] = GenericAnnotationType.REF_PREFIX+elem.getAttributeNS(Namespaces.ITS_NS_URI, "revOrgRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "revTool") )
                data[6] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "revTool");
            else if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "revToolRef") )
                data[6] = GenericAnnotationType.REF_PREFIX+elem.getAttributeNS(Namespaces.ITS_NS_URI, "revToolRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "provRef") )
                data[7] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "provRef");
        }
        else {
            if ( elem.hasAttribute("provenanceRecordsRef") )
                data[0] = elem.getAttribute("provenanceRecordsRef");

            if ( elem.hasAttribute("person") )
                data[1] = elem.getAttribute("person");
            else if ( elem.hasAttribute("personRef") )
                data[1] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("personRef");

            if ( elem.hasAttribute("org") )
                data[2] = elem.getAttribute("org");
            else if ( elem.hasAttribute("orgRef") )
                data[2] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("orgRef");

            if ( elem.hasAttribute("tool") )
                data[3] = elem.getAttribute("tool");
            else if ( elem.hasAttribute("toolRef") )
                data[3] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("toolRef");

            if ( elem.hasAttribute("revPerson") )
                data[4] = elem.getAttribute("revPerson");
            else if ( elem.hasAttribute("revPersonRef") )
                data[4] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("revPersonRef");

            if ( elem.hasAttribute("revOrg") )
                data[5] = elem.getAttribute("revOrg");
            else if ( elem.hasAttribute("revOrgRef") )
                data[5] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("revOrgRef");

            if ( elem.hasAttribute("revTool") )
                data[6] = elem.getAttribute("revTool");
            else if ( elem.hasAttribute("revToolRef") )
                data[6] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("revToolRef");

            if ( elem.hasAttribute("provRef") )
                data[7] = elem.getAttribute("provRef");
        }
        return data;
    }

    /**
     * Retrieves the non-pointer information of the text analysis data category.
     * @param elem the element where to get the data.
     * @param qualified true if the attributes are expected to be qualified.
     * @return an array of the value: classRef, source, ident/identRef
     */
    private String[] retrieveTextAnalysisData (Element elem,
                                               boolean qualified,
                                               boolean useHTML5)
    {
        String[] data = new String[5];

        if ( useHTML5 ) {
            if ( elem.hasAttribute("its-ta-class-ref") )
                data[0] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("its-ta-class-ref");

            if ( elem.hasAttribute("its-ta-source") )
                data[1] = elem.getAttribute("its-ta-source");

            if ( elem.hasAttribute("its-ta-ident") )
                data[2] = elem.getAttribute("its-ta-ident");
                // OR the ref version
            else if ( elem.hasAttribute("its-ta-ident-ref") )
                data[2] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("its-ta-ident-ref");

            if ( elem.hasAttribute("its-ta-confidence") )
                data[3] = elem.getAttribute("its-ta-confidence");
        }
        else if ( qualified ) {
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "taClassRef") )
                data[0] = GenericAnnotationType.REF_PREFIX+elem.getAttributeNS(Namespaces.ITS_NS_URI, "taClassRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "taSource") )
                data[1] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "taSource");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "taIdent") )
                data[2] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "taIdent");
                // OR the ref version
            else if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "taIdentRef") )
                data[2] = GenericAnnotationType.REF_PREFIX+elem.getAttributeNS(Namespaces.ITS_NS_URI, "taIdentRef");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "taConfidence") )
                data[3] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "taConfidence");
        }
        else {
            if ( elem.hasAttribute("taClassRef") )
                data[0] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("taClassRef");

            if ( elem.hasAttribute("taSource") )
                data[1] = elem.getAttribute("taSource");

            if ( elem.hasAttribute("taIdent") )
                data[2] = elem.getAttribute("taIdent");
                // OR the ref version
            else if ( elem.hasAttribute("taIdentRef") )
                data[2] = GenericAnnotationType.REF_PREFIX+elem.getAttribute("taIdentRef");

            if ( elem.hasAttribute("taConfidence") )
                data[3] = elem.getAttribute("taConfidence");
        }

        //TODO: Validation

        return data;
    }

    /**
     * Retrieves the non-pointer information of the Localization Quality Rating data category.
     * @param elem the element where to get the data.
     * @param qualified true if the attributes are expected to be qualified.
     * @return an array of the value: score, vote, scoreThreshold, voteThreshold, profileRef
     */
    private String[] retrieveLocQualityRatingData (Element elem,
                                                   boolean qualified,
                                                   boolean useHTML5)
    {
        String[] data = new String[5];

        if ( useHTML5 ) {
            if ( elem.hasAttribute("its-loc-quality-rating-score") )
                data[0] = elem.getAttribute("its-loc-quality-rating-score");

            if ( elem.hasAttribute("its-loc-quality-rating-vote") )
                data[1] = elem.getAttribute("its-loc-quality-rating-vote");

            if ( elem.hasAttribute("its-loc-quality-rating-score-threshold") )
                data[2] = elem.getAttribute("its-loc-quality-rating-score-threshold");

            if ( elem.hasAttribute("its-loc-quality-rating-vote-threshold") )
                data[3] = elem.getAttribute("its-loc-quality-rating-vote-threshold");

            if ( elem.hasAttribute("its-loc-quality-rating-profile-ref") )
                data[4] = elem.getAttribute("its-loc-quality-rating-profile-ref");
        }
        else if ( qualified ) {
            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityRatingScore") )
                data[0] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityRatingScore");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityRatingVote") )
                data[1] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityRatingVote");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityRatingScoreThreshold") )
                data[2] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityRatingScoreThreshold");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityRatingVoteThreshold") )
                data[3] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityRatingVoteThreshold");

            if ( elem.hasAttributeNS(Namespaces.ITS_NS_URI, "locQualityRatingProfileRef") )
                data[4] = elem.getAttributeNS(Namespaces.ITS_NS_URI, "locQualityRatingProfileRef");
        }
        else {
            if ( elem.hasAttribute("locQualityRatingScore") )
                data[0] = elem.getAttribute("locQualityRatingScore");

            if ( elem.hasAttribute("locQualityRatingVote") )
                data[1] = elem.getAttribute("locQualityRatingVote");

            if ( elem.hasAttribute("locQualityRatingScoreThreshold") )
                data[2] = elem.getAttribute("locQualityRatingScoreThreshold");

            if ( elem.hasAttribute("locQualityRatingVoteThreshold") )
                data[3] = elem.getAttribute("locQualityRatingVoteThreshold");

            if ( elem.hasAttribute("locQualityRatingProfileRef") )
                data[4] = elem.getAttribute("locQualityRatingProfileRef");
        }

        // Basic validation
        if (( data[0] != null ) && ( data[1] != null )) {
            logger.error("Cannot have localization quality rating score and vote at the same time.");
            data[1] = null;
        }
        if (( data[0] != null ) && ( data[3] != null )) {
            logger.error("Cannot have localization quality rating score with a vote threshold.");
            data[3] = null;
        }
        if (( data[1] != null ) && ( data[2] != null )) {
            logger.error("Cannot have localization quality rating vote with a score threshold.");
            data[2] = null;
        }

        return data;
    }

    private GenericAnnotations fetchLocQualityStandoffData (String ref,
                                                            String originalRef)
    {
        if ( Util.isEmpty(ref) ) {
            throw new InvalidParameterException("The reference URI cannot be null or empty.");
        }
        // Identify the type of reference (internal/external)
        // and get the element
        int n = ref.lastIndexOf('#');
        String id = null;
        String firstPart = null;
        if ( n > -1 ) {
            id = ref.substring(n+1);
            firstPart = ref.substring(0, n);
        }
        else {
            // No ID in the URI
            throw new OkapiException(String.format("URI to standoff markup does not have an id: '%s'.", ref));
        }

        boolean useHTML5 = isHTML5;
        Document containerDoc = null;
        XPath containerXPath = null;

        if ( !Util.isEmpty(firstPart) ) {
            // Load the document and the rules
            try {
                String baseFolder = "";
                if ( docURI != null) baseFolder = FileUtil.getPartBeforeFile(docURI);
                if ( baseFolder.length() > 0 ) {
                    if ( baseFolder.endsWith("/") )
                        baseFolder = baseFolder.substring(0, baseFolder.length()-1);
                    if ( !ref.startsWith("/") ) ref = baseFolder + "/" + ref;
                    else ref = baseFolder + ref;
                }

                // Remove the ID if any
                int p = ref.lastIndexOf('#');
                if ( p > -1 ) {
                    ref = ref.substring(0, p);
                }
                // Detect format based on extension: .html and .html as HTML, everything else as XML.
                useHTML5 = ( ref.endsWith(".html") || ref.endsWith(".htm") );
                if ( useHTML5 ) {
                    containerDoc = parseHTMLDocument(ref);
                }
                else {
                    containerDoc = parseXMLDocument(ref);
                }
                containerXPath = createXPath();
            }
            catch ( Throwable e) {
                throw new OkapiException(String.format("Error with URI '%s'.\n"+e.getMessage(), ref));
            }
        }
        else {
            // Else, the standoff markup is in the same document as the annotated content
            containerDoc = doc;
            containerXPath = xpath;
        }

        // Create the new annotation set
        GenericAnnotations anns = createLQIAnnotationSet();
        Document issuesDoc = null;
        XPath issuesXPath = null;

        if ( useHTML5 ) {
            // If the standoff markup is inside an HTML file:
            // Get the script that holds the its:locQualityIssues element
            try {
                String tmp = String.format("//%s:script[@id='%s']", Namespaces.HTML_NS_PREFIX, id);
                //String tmp = String.format("//script[@id='%s']", id);
                XPathExpression expr = containerXPath.compile(tmp);
                Element scriptElem = (Element)expr.evaluate(containerDoc, XPathConstants.NODE);
                if ( scriptElem == null ) {
                    logger.warn("Cannot find standoff script element for '{}'", id);
                    GenericAnnotation ann = addIssueItem(anns);
                    ann.setString(GenericAnnotationType.LQI_ISSUESREF, ref); // For information only
                    return anns;
                }
                // Else: parse the locQualityIssues element inside the script
                String content = scriptElem.getTextContent();
                // Strip white spaces
                content = content.trim();
                // Create the context and XPath engine
                issuesXPath = createXPath();
                // Parse the content
                try {
                    InputSource is = new InputSource(new ByteArrayInputStream(docEncoding != null ? content.getBytes(docEncoding) : content.getBytes()));
                    issuesDoc = parseXMLDocument(is);
                }
                catch ( Throwable e ) {
                    throw new OkapiException("Error parsing a script element.", e);
                }
            }
            catch ( XPathExpressionException e ) {
                throw new OkapiException("XPath error.", e);
            }
        }
        else {
            issuesDoc = containerDoc;
            issuesXPath = containerXPath;
        }

        // Now get the element holding the list of issues
        Element elem1;
        try {
            String tmp = String.format("//%s:locQualityIssues[@xml:id='%s']", Namespaces.ITS_NS_PREFIX, id);
            XPathExpression expr = issuesXPath.compile(tmp);
            elem1 = (Element)expr.evaluate(issuesDoc, XPathConstants.NODE);
        }
        catch ( XPathExpressionException e ) {
            throw new OkapiException("XPath error.", e);
        }
        if ( elem1 == null ) {
            // Entry not found
            logger.warn("Cannot find standoff markup for '{}'", originalRef);
            GenericAnnotation ann = addIssueItem(anns);
            ann.setString(GenericAnnotationType.LQI_ISSUESREF, originalRef); // For information only
            return anns;
        }

        // Then get the list of items in the element
        NodeList items = elem1.getElementsByTagNameNS(Namespaces.ITS_NS_URI, "locQualityIssue");
        for ( int i=0; i<items.getLength(); i++ ) {
            // For each entry
            Element elem2 = (Element)items.item(i);
            // Add the annotation to the set
            GenericAnnotation ann = addIssueItem(anns);
            ann.setString(GenericAnnotationType.LQI_ISSUESREF, originalRef); // For information only
            // Gather the local information (never in HTML since if it's HTML it's inside a script)
            String[] values = retrieveLocQualityIssueData(elem2, false, false);
            if ( values[0] != null ) {
                logger.warn("Cannot have a standoff reference in a standoff element (reference='{}').", ref);
            }
            if ( values[1] != null ) ann.setString(GenericAnnotationType.LQI_TYPE, values[1]);
            if ( values[2] != null ) ann.setString(GenericAnnotationType.LQI_COMMENT, values[2]);
            if ( values[3] != null ) ann.setDouble(GenericAnnotationType.LQI_SEVERITY, Double.parseDouble(values[3]));
            if ( values[4] != null ) ann.setString(GenericAnnotationType.LQI_PROFILEREF, values[4]);
            if ( values[5] != null ) ann.setBoolean(GenericAnnotationType.LQI_ENABLED, values[5].equals("yes"));
        }

        return anns;
    }

    private GenericAnnotations fetchProvenanceStandoffData (String ref,
                                                            String originalRef)
    {
        if ( Util.isEmpty(ref) ) {
            throw new InvalidParameterException("The reference URI cannot be null or empty.");
        }
        // Identify the type of reference (internal/external)
        // and get the element
        int n = ref.lastIndexOf('#');
        String id = null;
        String firstPart = null;
        if ( n > -1 ) {
            id = ref.substring(n+1);
            firstPart = ref.substring(0, n);
        }
        else {
            // No ID in the URI
            throw new OkapiException(String.format("URI to standoff markup does not have an id: '%s'.", ref));
        }

        boolean useHTML5 = isHTML5;
        Document containerDoc = null;
        XPath containerXPath = null;

        if ( !Util.isEmpty(firstPart) ) {
            // Load the document and the rules
            try {
                String baseFolder = "";
                if ( docURI != null) baseFolder = FileUtil.getPartBeforeFile(docURI);
                if ( baseFolder.length() > 0 ) {
                    if ( baseFolder.endsWith("/") )
                        baseFolder = baseFolder.substring(0, baseFolder.length()-1);
                    if ( !ref.startsWith("/") ) ref = baseFolder + "/" + ref;
                    else ref = baseFolder + ref;
                }

                // Remove the ID if any
                int p = ref.lastIndexOf('#');
                if ( p > -1 ) {
                    ref = ref.substring(0, p);
                }
                // Detect format based on extension: .html and .html as HTML, everything else as XML.
                useHTML5 = ( ref.endsWith(".html") || ref.endsWith(".htm") );
                if ( useHTML5 ) {
                    containerDoc = parseHTMLDocument(ref);
                }
                else {
                    containerDoc = parseXMLDocument(ref);
                }
                containerXPath = createXPath();
            }
            catch ( Throwable e) {
                throw new OkapiException(String.format("Error with URI '%s'.\n"+e.getMessage(), ref));
            }
        }
        else {
            // Else, the standoff markup is in the same document as the annotated content
            containerDoc = doc;
            containerXPath = xpath;
        }

        // Create the new annotation set
        GenericAnnotations anns = createProvenanceAnnotationSet();
        Document issuesDoc = null;
        XPath issuesXPath = null;

        if ( useHTML5 ) {
            // If the standoff markup is inside an HTML file:
            // Get the script that holds the its:locQualityIssues element
            try {
                String tmp = String.format("//%s:script[@id='%s']", Namespaces.HTML_NS_PREFIX, id);
                //String tmp = String.format("//script[@id='%s']", id);
                XPathExpression expr = containerXPath.compile(tmp);
                Element scriptElem = (Element)expr.evaluate(containerDoc, XPathConstants.NODE);
                if ( scriptElem == null ) {
                    logger.warn("Cannot find standoff script element for '{}'", id);
                    GenericAnnotation ann = anns.add(GenericAnnotationType.PROV);
                    ann.setString(GenericAnnotationType.PROV_RECSREF, ref); // For information only
                    return anns;
                }
                // Else: parse the locQualityIssues element inside the script
                String content = scriptElem.getTextContent();
                // Strip white spaces
                content = content.trim();
                // Create the context and XPath engine
                issuesXPath = createXPath();
                // Parse the content
                try {
                    InputSource is = new InputSource(new ByteArrayInputStream(docEncoding != null ? content.getBytes(docEncoding) : content.getBytes()));
                    issuesDoc = parseXMLDocument(is);
                }
                catch ( Throwable e ) {
                    throw new OkapiException("Error parsing a script element: ."+e.getMessage(), e);
                }
            }
            catch ( XPathExpressionException e ) {
                throw new OkapiException("XPath error.", e);
            }
        }
        else {
            issuesDoc = containerDoc;
            issuesXPath = containerXPath;
        }

        // Now get the element holding the list of issues
        Element elem1;
        try {
            String tmp = String.format("//%s:provenanceRecords[@xml:id='%s']", Namespaces.ITS_NS_PREFIX, id);
            XPathExpression expr = issuesXPath.compile(tmp);
            elem1 = (Element)expr.evaluate(issuesDoc, XPathConstants.NODE);
        }
        catch ( XPathExpressionException e ) {
            throw new OkapiException("XPath error.", e);
        }
        if ( elem1 == null ) {
            // Entry not found
            logger.warn("Cannot find standoff markup for '{}'", originalRef);
            GenericAnnotation ann = anns.add(GenericAnnotationType.PROV);
            ann.setString(GenericAnnotationType.PROV_RECSREF, originalRef); // For information only
            return anns;
        }

        // Then get the list of items in the element
        NodeList items = elem1.getElementsByTagNameNS(Namespaces.ITS_NS_URI, "provenanceRecord");
        for ( int i=0; i<items.getLength(); i++ ) {
            // For each entry
            Element elem2 = (Element)items.item(i);
            // Add the annotation to the set
            GenericAnnotation ann = anns.add(GenericAnnotationType.PROV);
            ann.setString(GenericAnnotationType.PROV_RECSREF, originalRef); // For information only
            // Gather the local information (never in HTML since if it's HTML it's inside a script)
            String[] values = retrieveProvenanceData(elem2, false, false);
            if ( values[0] != null ) {
                logger.warn("Cannot have a standoff reference in a standoff element (reference='{}').", ref);
            }
            if ( values[1] != null ) ann.setString(GenericAnnotationType.PROV_PERSON, values[1]);
            if ( values[2] != null ) ann.setString(GenericAnnotationType.PROV_ORG, values[2]);
            if ( values[3] != null ) ann.setString(GenericAnnotationType.PROV_TOOL, values[3]);
            if ( values[4] != null ) ann.setString(GenericAnnotationType.PROV_REVPERSON, values[4]);
            if ( values[5] != null ) ann.setString(GenericAnnotationType.PROV_REVORG, values[5]);
            if ( values[6] != null ) ann.setString(GenericAnnotationType.PROV_REVTOOL, values[6]);
            if ( values[7] != null ) ann.setString(GenericAnnotationType.PROV_PROVREF, values[7]);
        }

        return anns;
    }

    private boolean isVersion2 () throws XPathExpressionException {
        // If the version is not detected yet: detect it.
        if ( version.equals("0") ) {
            XPathExpression expr = xpath.compile("//*/@"+Namespaces.ITS_NS_PREFIX+":version|//"+Namespaces.ITS_NS_PREFIX+":span/@version");
            NodeList NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
            if (( NL == null ) || ( NL.getLength() == 0 )) {
                // No version detected: we assume it's a 2.0 behavior
                version = "2.0";
            }
            else {
                if ( NL.getLength() > 0 ) {
                    version = ((Attr)NL.item(0)).getValue();
                    if ( !version.equals(ITS_VERSION1) && !version.equals(ITS_VERSION2) ) {
                        throw new ITSException(String.format("Invalid or missing ITS version (\"%s\")", version));
                    }
                }
                if ( NL.getLength() > 1 ) {
                    throw new ITSException("More than one ITS version is defined in this document.");
                }
            }
        }
        return version.equals(ITS_VERSION2);
    }

    /**
     * Gets the text content of the first TEXT child of an element node.
     * This is to use instead of node.getTextContent() which does not work with some
     * Macintosh Java VMs. Note this work-around get <b>only the first TEXT node</b>.
     * @param node the container element.
     * @return the text of the first TEXT child node.
     */
    public static String getTextContent (Node node) {
        Node tmp = node.getFirstChild();
        while ( true ) {
            if ( tmp == null ) return "";
            if ( tmp.getNodeType() == Node.TEXT_NODE ) {
                return tmp.getNodeValue();
            }
            tmp = tmp.getNextSibling();
        }
    }

    public String getCommentContent(NodeList list) {
        logger.error("SAVAGE PATCHING");
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < list.getLength(); i++) {
            stringBuilder.append(list.item(i).getNodeValue());
        }

        return stringBuilder.toString();
    }

    private String resolvePointer (Node node,
                                   String pointer)
    {
        try {
            if ( pointer.contains("id(") ) {
                markDefaultIdentifiers();
            }
            XPathExpression expr = xpath.compile(pointer);
            NodeList list = (NodeList)expr.evaluate(node, XPathConstants.NODESET);
            if (( list == null ) || ( list.getLength() == 0 )) {
                logger.debug("No node match the pointer '{}'.", pointer);
                return null;
            }
            switch ( list.item(0).getNodeType() ) {
                case Node.ELEMENT_NODE:
                    return getTextContent(list.item(0));
                case Node.ATTRIBUTE_NODE:
                    return list.item(0).getNodeValue();
                case Node.COMMENT_NODE:
                    return getCommentContent(list);
            }
        }
        catch ( XPathExpressionException e ) {
            logger.error("Bad XPath expression in pointer '{}:\n{}'.", pointer, e.getMessage());
        }
        return null;
    }

    /**
     * mark all xml:id attribute in a document as of ID-type, so they can be used
     * with the id() XPath and similar functions.
     * This method is executed only once.
     * @throws XPathExpressionException
     */
    private void markDefaultIdentifiers ()
            throws XPathExpressionException
    {
        if ( defaultIdsDone ) return;
        // For xml:id (for HTML5 too, just in case it's used)
        XPathExpression expr = xpath.compile("//*[@xml:id]");
        NodeList list = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
        if ( list != null ) {
            for ( int i=0; i<list.getLength(); i++ ) {
                Element elem = (Element)list.item(i);
                elem.setIdAttributeNS(Namespaces.XML_NS_URI, "id", true);
            }
        }
        // For HTML id
        if ( isHTML5 ) {
            expr = xpath.compile("//*[@xml:id]");
            list = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
            if ( list != null ) {
                for ( int i=0; i<list.getLength(); i++ ) {
                    Element elem = (Element)list.item(i);
                    elem.setIdAttributeNS(Namespaces.HTML_NS_URI, "id", true);
                }
            }
        }
        defaultIdsDone = true;
    }

    private String resolveExpressionAsString (Node node,
                                              String expression)
    {
        try {
            if ( expression.contains("id(") ) {
                markDefaultIdentifiers();
            }
            XPathExpression expr = xpath.compile(expression);
            return (String)expr.evaluate(node, XPathConstants.STRING);
        }
        catch (XPathExpressionException e) {
            return "Bad XPath expression \""+expression+"\".";
        }
    }

    private List<String> resolveExpressionAsList (Node node,
                                                  String expression)
    {
        ArrayList<String> list = new ArrayList<String>();
        try {
            if ( expression.contains("id(") ) {
                markDefaultIdentifiers();
            }
            XPathExpression expr = xpath.compile(expression);
            NodeList nl = (NodeList)expr.evaluate(node, XPathConstants.NODESET);
            for ( int i=0; i<nl.getLength(); i++ ) {
                Node tmpNode = nl.item(i);
                if ( tmpNode.getNodeType() == Node.ELEMENT_NODE ) {
                    list.add(tmpNode.getTextContent());
                }
                else { // Attribute
                    list.add(tmpNode.getNodeValue());
                }
            }
        }
        catch (XPathExpressionException e) {
            list.add("Bad XPath expression \""+expression+"\".");
        }
        return list;
    }

    /**
     * Sets the flag for a given node.
     * @param node The node to flag.
     * @param position The position for the data category.
     * @param value The value to set.
     * @param override True if the value should override an existing value.
     * False should be used only for default attribute values.
     */
    private void setFlag (Node node,
                          int position,
                          char value,
                          boolean override)
    {
        StringBuilder data = new StringBuilder();
        if ( node.getUserData(FLAGNAME) == null )
            data.append(FLAGDEFAULTDATA);
        else
            data.append((String)node.getUserData(FLAGNAME));
        // Set the new value (if not there yet or override requested)
        if ( override || ( data.charAt(position) != '?' ))
            data.setCharAt(position, value);
        node.setUserData(FLAGNAME, data.toString(), null);
    }

    /**
     * Sets the data for a flag for a given node.
     * @param node The node where to set the data.
     * @param position The position for the data category.
     * @param value The value to set.
     * @param override True if the value should override an existing value.
     * False should be used only for default attribute values.
     */
    private void setFlag (Node node,
                          int position,
                          String value,
                          boolean override)
    {
        StringBuilder data = new StringBuilder();
        if ( node.getUserData(FLAGNAME) == null )
            data.append(FLAGDEFAULTDATA);
        else
            data.append((String)node.getUserData(FLAGNAME));
        // Get the data
        int n1 = 0;
        int n2 = data.indexOf(FLAGSEP, 0);
        for ( int i=0; i<=position; i++ ) {
            n1 = n2;
            n2 = data.indexOf(FLAGSEP, n1+1);
        }
        // Set the new value (if not there yet or override requested)
        if ( override || ( n2>n1+1 )) {
            data.replace(n1+1, n2, value);
        }
        node.setUserData(FLAGNAME, data.toString(), null);
    }

    private String getFlagData (String data,
                                int position)
    {
        int n1 = 0;
        int n2 = data.indexOf(FLAGSEP, 0);
        for ( int i=0; i<=position; i++ ) {
            n1 = n2;
            n2 = data.indexOf(FLAGSEP, n1+1);
        }
        if ( n2>n1+1 ) return data.substring(n1+1, n2);
        else return "";
    }

    @Override
    public boolean getTranslate (Attr attribute) {
        if ( attribute == null ) return trace.peek().translate;
        // Else: check the attribute
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) {
            if ( !isHTML5 ) return false;
            // Else: for HTML5: some attributes are deemed 'translatable':
            //
            // abbr, alt, download, placeholder, title
            // content on meta elements, if the name attribute specifies a metadata name whose value is known to be translatable
            // srcdoc (value is in HTML)
            // style (Value is in CSS)
            //
            String name = attribute.getName().toLowerCase();
            if ( html5TransAttr.indexOf(" "+name+" ") != -1 ) {
                // They take the translate property of the element where they are.
                return trace.peek().translate;
            }
            else if ( name.equals("content") ) {
                String nameValue = attribute.getOwnerElement().getAttribute("name");
                if ( " keywords description ".indexOf(" "+nameValue.toLowerCase()+" ") != -1 ) {
                    // It is one of the 'translatable' attribute so it takes the translate property of its parent element.
                    return trace.peek().translate;
                }
                else return false;
            }
            else {
                return false; // Other HTML5 attributes
            }
        }
        // '?' and 'n' will return (correctly) false
        return (tmp.charAt(FP_TRANSLATE) == 'y');
    }

    @Override
    public String getTargetPointer (Attr attribute) {
        if ( attribute == null ) return trace.peek().targetPointer;
        // Else: check the attribute
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_TARGETPOINTER) != 'y' ) return null;
        return getFlagData(tmp, FP_TARGETPOINTER_DATA);
    }

    @Override
    public String getIdValue (Attr attribute) {
        if ( attribute == null ) return trace.peek().idValue;
        // Else: check the attribute
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        return getFlagData(tmp, FP_IDVALUE_DATA);
    }

    @Override
    public int getDirectionality (Attr attribute) {
        if ( attribute == null ) return trace.peek().dir;
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return trace.peek().dir;
        return Integer.valueOf(tmp.charAt(FP_DIRECTIONALITY));
    }

    @Override
    public int getWithinText () {
        return trace.peek().withinText;
    }

    @Override
    public boolean getTerm (Attr attribute) {
        if ( attribute == null ) {
            return (trace.peek().termino != null);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return false;
        return (tmp.charAt(FP_TERMINOLOGY) == 'y');
    }

    @Override
    public String getTermInfo (Attr attribute) {
        if ( attribute == null ) {
            if ( trace.peek().termino == null ) return null;
            return trace.peek().termino.getAnnotations(GenericAnnotationType.TERM).get(0).getString(GenericAnnotationType.TERM_INFO);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_TERMINOLOGY) != 'y' ) return null;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_TERMINOLOGY_DATA));
        return anns.getAnnotations(GenericAnnotationType.TERM).get(0).getString(GenericAnnotationType.TERM_INFO);
    }

    @Override
    public Double getTermConfidence (Attr attribute) {
        if ( attribute == null ) {
            if ( trace.peek().termino == null ) return null;
            return trace.peek().termino.getAnnotations(GenericAnnotationType.TERM).get(0).getDouble(GenericAnnotationType.TERM_CONFIDENCE);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_TERMINOLOGY) != 'y' ) return null;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_TERMINOLOGY_DATA));
        return anns.getAnnotations(GenericAnnotationType.TERM).get(0).getDouble(GenericAnnotationType.TERM_CONFIDENCE);
    }

    /**
     * Gets the terminology annotation set for the current element
     * or one of its attributes.
     * Note that the returned object is not the same at each each call.
     * @param attribute the attribute to look up, or null for the element.
     * @return the annotation set for the queried node (can be null).
     */
    public GenericAnnotations getTerminologyAnnotation (Attr attribute) {
        if ( attribute == null ) {
            return trace.peek().termino;
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_TERMINOLOGY) != 'y' ) return null;
        return new GenericAnnotations(getFlagData(tmp, FP_TERMINOLOGY_DATA));
    }

    @Override
    public String getLocNote (Attr attribute) {
        if ( attribute == null ) return trace.peek().locNote;
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_LOCNOTE) == '?' ) return null;
        return getFlagData(tmp, FP_LOCNOTE_DATA);
    }

    @Override
    public String getLocNoteType (Attr attribute) {
        if ( attribute == null ) return trace.peek().locNoteType;
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_LOCNOTE) == '?' ) return null;
        if ( tmp.charAt(FP_LOCNOTE) == 'a' ) return "alert";
        return "description";
    }

    @Override
    public String getDomains (Attr attribute) {
        if ( attribute == null ) return trace.peek().domains;
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return trace.peek().domains;
        if ( tmp.charAt(FP_DOMAIN) != 'y' ) return trace.peek().domains;
        return getFlagData(tmp, FP_DOMAIN_DATA);
    }

    @Override
    public boolean preserveWS (Attr attribute) {
        if ( attribute == null ) return trace.peek().preserveWS;
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return trace.peek().preserveWS;
        return (tmp.charAt(FP_PRESERVEWS) == 'y' );
    }

    @Override
    public String getLanguage () {
        return trace.peek().language;
    }

    @Override
    public String getExternalResourceRef (Attr attribute) {
        if ( attribute == null ) return trace.peek().externalRes;
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_EXTERNALRES) != 'y' ) return null;
        return getFlagData(tmp, FP_EXTERNALRES_DATA);
    }

    @Override
    public String getLocaleFilter () {
        return trace.peek().localeFilter;
    }

    @Override
    public String getLocQualityIssuesRef (Attr attribute) {
        return getLQIValue(GenericAnnotationType.LQI_ISSUESREF, attribute, 0);
    }

    @Override
    public int getLocQualityIssueCount (Attr attribute) {
        if ( attribute == null ) {
            GenericAnnotations lqi = trace.peek().lqIssues;
            if ( lqi == null ) return 0;
            return lqi.size();
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return 0;
        if ( tmp.charAt(FP_LQISSUE) != 'y' ) return 0;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_LQISSUE_DATA));
        return anns.getAnnotations(GenericAnnotationType.LQI).size();
    }

    /**
     * Gets the localization quality issue annotations for the current element
     * or one of its attributes.
     * Note that the returned objects are not the same at each each call.
     * @param attribute the attribute to look up, or null for the element.
     * @return the annotation set for the queried node (can be null).
     */
    public GenericAnnotations getLocQualityIssueAnnotations (Attr attribute) {
        if ( attribute == null ) {
            return trace.peek().lqIssues;
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_LQISSUE) != 'y' ) return null;
        return new GenericAnnotations(getFlagData(tmp, FP_LQISSUE_DATA));
    }

    @Override
    public String getLocQualityIssueType (Attr attribute,
                                          int index)
    {
        return getLQIValue(GenericAnnotationType.LQI_TYPE, attribute, index);
    }

    @Override
    public String getLocQualityIssueComment (Attr attribute,
                                             int index)
    {
        return getLQIValue(GenericAnnotationType.LQI_COMMENT, attribute, index);
    }

    @Override
    public Double getLocQualityIssueSeverity (Attr attribute,
                                              int index)
    {
        if ( attribute == null ) {
            if ( trace.peek().lqIssues == null ) return null;
            return trace.peek().lqIssues.getAnnotations(GenericAnnotationType.LQI).get(index).getDouble(GenericAnnotationType.LQI_SEVERITY);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_LQISSUE) != 'y' ) return null;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_LQISSUE_DATA));
        return anns.getAnnotations(GenericAnnotationType.LQI).get(index).getDouble(GenericAnnotationType.LQI_SEVERITY);
    }

    @Override
    public String getLocQualityIssueProfileRef (Attr attribute,
                                                int index)
    {
        return getLQIValue(GenericAnnotationType.LQI_PROFILEREF, attribute, index);
    }

    @Override
    public Boolean getLocQualityIssueEnabled (Attr attribute,
                                              int index)
    {
        if ( attribute == null ) {
            if ( trace.peek().lqIssues == null ) return null;
            return trace.peek().lqIssues.getAnnotations(GenericAnnotationType.LQI).get(index).getBoolean(GenericAnnotationType.LQI_ENABLED);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_LQISSUE) != 'y' ) return null;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_LQISSUE_DATA));
        return anns.getAnnotations(GenericAnnotationType.LQI).get(index).getBoolean(GenericAnnotationType.LQI_ENABLED);
    }

    private String getLQIValue (String fieldName,
                                Attr attribute,
                                int index)
    {
        if ( attribute == null ) {
            if ( trace.peek().lqIssues == null ) return null;
            return trace.peek().lqIssues.getAnnotations(GenericAnnotationType.LQI).get(index).getString(fieldName);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_LQISSUE) != 'y' ) return null;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_LQISSUE_DATA));
        return anns.getAnnotations(GenericAnnotationType.LQI).get(index).getString(fieldName);
    }

    /**
     * Gets the text analysis annotations for the current element
     * or one of its attributes.
     * Note that the returned objects are not the same instances at each each call.
     * @param attribute the attribute to look up, or null for the element.
     * @return the annotations for the queried node (can be null).
     */
    public GenericAnnotations getTextAnalysisAnnotation (Attr attribute) {
        if ( attribute == null ) {
            return trace.peek().ta;
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_TEXTANALYSIS) != 'y' ) return null;
        return new GenericAnnotations(getFlagData(tmp, FP_TEXTANALYSIS_DATA));
    }

    @Override
    public String getTextAnalysisClass (Attr attribute) {
        return getTAValue(GenericAnnotationType.TA_CLASS, attribute);
    }

    @Override
    public String getTextAnalysisSource (Attr attribute) {
        return getTAValue(GenericAnnotationType.TA_SOURCE, attribute);
    }

    @Override
    public String getTextAnalysisIdent (Attr attribute) {
        return getTAValue(GenericAnnotationType.TA_IDENT, attribute);
    }

    @Override
    public Double getTextAnalysisConfidence (Attr attribute) {
        if ( attribute == null ) {
            if ( trace.peek().ta == null ) return null;
            return trace.peek().ta.getAnnotations(GenericAnnotationType.TA).get(0).getDouble(GenericAnnotationType.TA_CONFIDENCE);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_TEXTANALYSIS) != 'y' ) return null;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_TEXTANALYSIS_DATA));
        return anns.getAnnotations(GenericAnnotationType.TA).get(0).getDouble(GenericAnnotationType.TA_CONFIDENCE);
    }

    private String getTAValue (String fieldName,
                               Attr attribute)
    {
        if ( attribute == null ) {
            if ( trace.peek().ta == null ) return null;
            return trace.peek().ta.getAnnotations(GenericAnnotationType.TA).get(0).getString(fieldName);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_TEXTANALYSIS) != 'y' ) return null;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_TEXTANALYSIS_DATA));
        return anns.getAnnotations(GenericAnnotationType.TA).get(0).getString(fieldName);
    }

    /**
     * Gets the localization quality rating annotation set for the current element.
     * @return the annotation set for the queried node (can be null).
     */
    public GenericAnnotations getLocQualityRatingAnnotation () {
        return trace.peek().lqRating;
    }

    @Override
    public Double getLocQualityRatingScore (Attr attribute) {
        if ( attribute != null ) return null;
        if ( trace.peek().lqRating == null ) return null;
        return trace.peek().lqRating.getAnnotations(GenericAnnotationType.LQR).get(0).getDouble(GenericAnnotationType.LQR_SCORE);
    }

    @Override
    public Integer getLocQualityRatingVote (Attr attribute) {
        if ( attribute != null ) return null;
        if ( trace.peek().lqRating == null ) return null;
        return trace.peek().lqRating.getAnnotations(GenericAnnotationType.LQR).get(0).getInteger(GenericAnnotationType.LQR_VOTE);
    }

    @Override
    public Double getLocQualityRatingScoreThreshold (Attr attribute) {
        if ( attribute != null ) return null;
        if ( trace.peek().lqRating == null ) return null;
        return trace.peek().lqRating.getAnnotations(GenericAnnotationType.LQR).get(0).getDouble(GenericAnnotationType.LQR_SCORETHRESHOLD);
    }

    @Override
    public Integer getLocQualityRatingVoteThreshold (Attr attribute) {
        if ( attribute != null ) return null;
        if ( trace.peek().lqRating == null ) return null;
        return trace.peek().lqRating.getAnnotations(GenericAnnotationType.LQR).get(0).getInteger(GenericAnnotationType.LQR_VOTETHRESHOLD);
    }

    @Override
    public String getLocQualityRatingProfileRef (Attr attribute) {
        if ( attribute != null ) return null;
        if ( trace.peek().lqRating == null ) return null;
        return trace.peek().lqRating.getAnnotations(GenericAnnotationType.LQR).get(0).getString(GenericAnnotationType.LQR_PROFILEREF);
    }

    public GenericAnnotations getStorageSizeAnnotation (Attr attribute) {
        if ( attribute == null ) {
            return trace.peek().storageSize;
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_STORAGESIZE) != 'y' ) return null;
        return new GenericAnnotations(getFlagData(tmp, FP_STORAGESIZE_DATA));
    }

    @Override
    public Integer getStorageSize (Attr attribute) {
        if ( attribute == null ) {
            if ( trace.peek().storageSize == null ) return null;
            return trace.peek().storageSize.getFirstAnnotation(GenericAnnotationType.STORAGESIZE).getInteger(GenericAnnotationType.STORAGESIZE_SIZE);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_STORAGESIZE) != 'y' ) return null;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_STORAGESIZE_DATA));
        return anns.getFirstAnnotation(GenericAnnotationType.STORAGESIZE).getInteger(GenericAnnotationType.STORAGESIZE_SIZE);
    }

    @Override
    public String getStorageEncoding (Attr attribute) {
        if ( attribute == null ) {
            if ( trace.peek().storageSize == null ) return "UTF-8";
            return trace.peek().storageSize.getFirstAnnotation(GenericAnnotationType.STORAGESIZE).getString(GenericAnnotationType.STORAGESIZE_ENCODING);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return "UTF-8";
        if ( tmp.charAt(FP_STORAGESIZE) != 'y' ) return "UTF-8";
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_STORAGESIZE_DATA));
        return anns.getFirstAnnotation(GenericAnnotationType.STORAGESIZE).getString(GenericAnnotationType.STORAGESIZE_ENCODING);
    }

    @Override
    public String getLineBreakType (Attr attribute) {
        if ( attribute == null ) {
            if ( trace.peek().storageSize == null ) return "lf";
            return trace.peek().storageSize.getFirstAnnotation(GenericAnnotationType.STORAGESIZE).getString(GenericAnnotationType.STORAGESIZE_LINEBREAK);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return "lf";
        if ( tmp.charAt(FP_STORAGESIZE) != 'y' ) return "lf";
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_STORAGESIZE_DATA));
        return anns.getFirstAnnotation(GenericAnnotationType.STORAGESIZE).getString(GenericAnnotationType.STORAGESIZE_LINEBREAK);
    }

    @Override
    public String getAllowedCharacters (Attr attribute) {
        if ( attribute == null ) return trace.peek().allowedChars;
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_ALLOWEDCHARS) != '?' ) return getFlagData(tmp, FP_ALLOWEDCHARS_DATA);
        return null;
    }

    public String getSubFilter (Attr attribute) {
        if ( attribute == null ) return trace.peek().subFilter;
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_SUBFILTER) != 'y' ) return null;
        return getFlagData(tmp, FP_SUBFILTER_DATA);
    }

    @Override
    public String getAnnotatorsRef () {
        return trace.peek().annotatorsRef;
    }

    @Override
    public String getAnnotatorRef (String dc) {
        validateDataCategoryNames(dc);
        String tmp = trace.peek().annotatorsRef;
        if ( tmp == null ) return null;
        Map<String, String> map = ITSContent.annotatorsRefToMap(tmp);
        return map.get(dc);
    }

    @Override
    public Double getMtConfidence (Attr attribute) {
        if ( attribute == null ) return trace.peek().mtConfidence;
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return null;
        if ( tmp.charAt(FP_MTCONFIDENCE) != 'y' ) return trace.peek().mtConfidence;
        return Double.parseDouble(getFlagData(tmp, FP_MTCONFIDENCE_DATA));
    }

//	/**
//	 * Prepares the document for using target pointers.
//	 * <p>Because of the way the skeleton is constructed and because target pointer can result in the target
//	 * location being anywhere in the document, we need to perform a first pass to create the targetTable
//	 * table. That table lists all the source nodes that have a target pointer and the corresponding target
//	 * node with its status.
//	 */
//	private void prepareTargetPointers () {
//		hasTargetPointer = false;
//		try {
//			// If there is no target pointers, just reset the flag
//			if ( !getTargetPointerRuleTriggered() ) {
//				return;
//			}
//			// Else: gather the target locations
//			startTraversal();
//
//			// Go through the document
//			Node srcNode;
//			while ( (srcNode = nextNode()) != null ) {
//				if ( srcNode.getNodeType() == Node.ELEMENT_NODE ) {
//					// Use !backTracking() to get to the elements only once
//					// and to include the empty elements (for attributes).
//					if ( !backTracking() ) {
//						// Check the element
//						if ( getTranslate(null) ) {
//							String pointer = getTargetPointer(null);
//							if ( pointer != null ) {
//								resolveTargetPointer(getXPath(), srcNode, pointer);
//							}
//						}
//						// Check the attributes
//						NamedNodeMap map = ((Element)srcNode).getAttributes();
//						for ( int i=0; i<map.getLength(); i++ ) {
//							Attr attr = (Attr)map.item(i);
//							if ( getTranslate(attr) ) {
//								String pointer = getTargetPointer(attr);
//								if ( pointer != null ) {
//									resolveTargetPointer(getXPath(), (Node)attr, pointer);
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		finally {
//			// Reset the traversal
//			startTraversal();
//		}
//	}
//
//	/**
//	 * Resolves the target pointer for a given source node and creates its
//	 * entry in targetTable, and set flag on the node
//	 * @param xpath the XPath object to use for the resolution.
//	 * @param srcNode the source node.
//	 * @param pointer the XPath expression pointing to the target node
//	 */
//	private void resolveTargetPointer (XPath xpath,
//		Node srcNode,
//		String pointer)
//	{
//		try {
//			XPathExpression expr = xpath.compile(pointer);
//			Node trgNode = (Node)expr.evaluate(srcNode, XPathConstants.NODE);
//			if ( trgNode == null ) {
//				// No entry available
//				//TODO: try to create the needed node
//				return;
//			}
//			// Check the type
//			if ( srcNode.getNodeType() != trgNode.getNodeType() ) {
//				logger.warn("Potential issue with target pointer '{}'.\nThe source and target node are of different types. "
//					+ "Depending on the content of the source, this may or may not be an issue.", pointer);
//			}
//			// Create the entry
//			TargetPointerEntry tpe = new TargetPointerEntry(srcNode, trgNode);
//			// Set the flags on each nod
//			srcNode.setUserData(SRC_TRGPTRFLAGNAME, tpe, null);
//			trgNode.setUserData(TRG_TRGPTRFLAGNAME, tpe, null);
//			hasTargetPointer = true;
//		}
//		catch ( XPathExpressionException e ) {
//			throw new OkapiIOException(String.format("Bad XPath expression in target pointer '%s'.", pointer));
//		}
//	}
//
//	/**
//	 * Indicates if the decorated document has at least one node with a target pointer.
//	 * @return true if the decorated document has at least one node with a target pointer,
//	 * false otherwise.
//	 */
//	public boolean hasTargetPointer () {
//		return hasTargetPointer;
//	}
//
//	/**
//	 * Gets the target pointer entry for a given node.
//	 * @param node the node to examine.
//	 * @return the target pointer entry for that node, or null if there is none.
//	 */
//	public TargetPointerEntry getTargetPointerEntry (Node node) {
//		TargetPointerEntry tpe = (TargetPointerEntry)node.getUserData(TRG_TRGPTRFLAGNAME);
//		if ( tpe != null ) {
//			// This node is a target location
//			//TODO
//		}
//		else {
//			tpe = (TargetPointerEntry)node.getUserData(SRC_TRGPTRFLAGNAME);
//			if ( tpe != null ) {
//				// This node is a source with a target location
//				// TODO
//			}
//		}
//		return tpe;
//	}

    /**
     * Gets the annotations for the Provenance data category for the current element
     * or for one of its attributes.
     * @param attribute the attribute to query, or null to query the current element.
     * @return the annotations for the queried part, or null if there is none.
     */
    public GenericAnnotations getProvenanceAnnotations (Attr attribute) {
        if ( attribute == null ) {
            return trace.peek().prov;
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) {
            return trace.peek().prov;
        }
        if ( tmp.charAt(FP_PROVENANCE) != 'y' ) return null;
        return new GenericAnnotations(getFlagData(tmp, FP_PROVENANCE_DATA));
    }

    @Override
    public String getProvRecordsRef (Attr attribute) {
        return getProvValue(GenericAnnotationType.PROV_RECSREF, attribute, 0);
    }

    @Override
    public int getProvRecordCount (Attr attribute) {
        if ( attribute == null ) {
            GenericAnnotations anns = trace.peek().prov;
            if ( anns == null ) return 0;
            return anns.size();
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) {
            GenericAnnotations anns = trace.peek().prov;
            if ( anns == null ) return 0;
            return anns.size();
        }
        if ( tmp.charAt(FP_PROVENANCE) != 'y' ) return 0;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_PROVENANCE_DATA));
        return anns.getAnnotations(GenericAnnotationType.PROV).size();
    }

    @Override
    public String getProvPerson (Attr attribute,
                                 int index)
    {
        return getProvValue(GenericAnnotationType.PROV_PERSON, attribute, index);
    }

    @Override
    public String getProvOrg (Attr attribute,
                              int index)
    {
        return getProvValue(GenericAnnotationType.PROV_ORG, attribute, index);
    }

    @Override
    public String getProvTool (Attr attribute,
                               int index)
    {
        return getProvValue(GenericAnnotationType.PROV_TOOL, attribute, index);
    }

    @Override
    public String getProvRevPerson(Attr attribute,
                                   int index)
    {
        return getProvValue(GenericAnnotationType.PROV_REVPERSON, attribute, index);
    }

    @Override
    public String getProvRevOrg (Attr attribute,
                                 int index)
    {
        return getProvValue(GenericAnnotationType.PROV_REVORG, attribute, index);
    }

    @Override
    public String getProvRevTool (Attr attribute,
                                  int index)
    {
        return getProvValue(GenericAnnotationType.PROV_REVTOOL, attribute, index);
    }

    @Override
    public String getProvRef (Attr attribute,
                              int index)
    {
        return getProvValue(GenericAnnotationType.PROV_PROVREF, attribute, index);
    }

    private String getProvValue (String fieldName,
                                 Attr attribute,
                                 int index)
    {
        if ( attribute == null ) {
            if ( trace.peek().prov == null ) return null;
            return trace.peek().prov.getAnnotations(GenericAnnotationType.PROV).get(index).getString(fieldName);
        }
        String tmp;
        if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) {
            if ( trace.peek().prov == null ) return null;
            return trace.peek().prov.getAnnotations(GenericAnnotationType.PROV).get(index).getString(fieldName);
        }
        // Attribute-specific info
        if ( tmp.charAt(FP_PROVENANCE) != 'y' ) return null;
        GenericAnnotations anns = new GenericAnnotations(getFlagData(tmp, FP_PROVENANCE_DATA));
        return anns.getAnnotations(GenericAnnotationType.PROV).get(index).getString(fieldName);
    }

}
