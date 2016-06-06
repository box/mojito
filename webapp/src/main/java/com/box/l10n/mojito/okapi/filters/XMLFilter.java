package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.service.assetExtraction.extractor.AssetPathToFilterConfigMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.okapi.common.BOMNewlineEncodingDetector;
import net.sf.okapi.common.DefaultEntityResolver;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author jyi
 */
public class XMLFilter extends net.sf.okapi.filters.xml.XMLFilter {

    public static final String FILTER_CONFIG_ID = "okf_xml@box_webapp";
    public static final String ANDROIDSTRINGS_CONFIG_FILE_NAME = "AndroidStrings_box_webapp.fprm";
    public static final String RESX_CONFIG_FILE_NAME = "resx_box_webapp.fprm";
    
    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    /**
     * Overriding to include only resx and AndroidStrings filters
     * @return 
     */
    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<>();
        list.add(new FilterConfiguration(getName() + "-resx",
                getMimeType(),
                getClass().getName(),
                "RESX",
                "Configuration for Microsoft RESX documents (without binary data).",
                RESX_CONFIG_FILE_NAME,
                ".resx;"));
        list.add(new FilterConfiguration(getName() + "-AndroidStrings",
                getMimeType(),
                getClass().getName(),
                "Android Strings",
                "Configuration for Android Strings XML documents.",
                ANDROIDSTRINGS_CONFIG_FILE_NAME));
        return list;
    }

    /**
     * Overriding to fix the parsing exception when the input encoding is UTF-16
     */
    @Override
    protected void initializeDocument() {
        // Create the document builder factory
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        fact.setNamespaceAware(true);
        fact.setValidating(false);
        // Expand entity references only if we do not protect them
        // "Expand entity" means don't have ENTITY_REFERENCE
        fact.setExpandEntityReferences(!params.protectEntityRef);

        // Create the document builder
        DocumentBuilder docBuilder;
        try {
            docBuilder = fact.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new OkapiIOException(e);
        }
        //TODO: Do this only as an option
        // Avoid DTD declaration
        docBuilder.setEntityResolver(new DefaultEntityResolver());

        input.setEncoding("UTF-8"); // Default for XML, other should be auto-detected
        BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(input.getStream(), input.getEncoding());
        detector.detectAndRemoveBom();

        if (detector.isAutodetected()) {
            encoding = detector.getEncoding();
            //--Start workaround issue with XML Parser
            // "UTF-16xx" are not handled as expected, using "UTF-16" alone 
            // seems to resolve the issue.
            if ((encoding.equals("UTF-16LE")) || (encoding.equals("UTF-16BE"))) {
                encoding = "UTF-16";
            }
            //--End workaround
            input.setEncoding(encoding);
        }

        try {
            InputSource is = new InputSource(input.getStream());
            //InputSource is = new InputSource(new BOMInputStream(input.getStream(), false));
            
            //--Fix to handle UTF-16 encoding
            //If input encoding is UTF-16, we need to explictly set encoding on the InputSource
            is.setEncoding(encoding);
            
            doc = docBuilder.parse(is);
        } catch (SAXException e) {
            throw new OkapiIOException("Parsing error.\n" + e.getMessage(), e);
        } catch (IOException e) {
            throw new OkapiIOException("IO Error when reading the document.\n" + e.getMessage(), e);
        }

        encoding = doc.getXmlEncoding();
        if (encoding == null) {
            encoding = detector.getEncoding();
        }
        srcLang = input.getSourceLocale();
        if (srcLang == null) {
            throw new NullPointerException("Source language not set.");
        }
        hasUTF8BOM = detector.hasUtf8Bom();
        lineBreak = detector.getNewlineType().toString();
        if (input.getInputURI() != null) {
            docName = input.getInputURI().getPath();
        }
    }

    /**
     * Overriding to use xml encoding from the input xml document
     * 
     * @param startDoc 
     */
    @Override
    protected void createStartDocumentSkeleton(StartDocument startDoc) {
        // Add the XML declaration
        skel = new GenericSkeleton();
        if (!params.omitXMLDeclaration) {
            skel.add("<?xml version=\"" + doc.getXmlVersion() + "\"");
            skel.add(" encoding=\"" + doc.getXmlEncoding() + "\"");
            if (doc.getXmlStandalone()) {
                skel.add(" standalone=\"yes\"");
            }
            skel.add("?>" + lineBreak);
        }
    }
    
    @Override
    public Event next() {
        Event event = super.next();
        
        if (input.getFilterConfigId().equals(AssetPathToFilterConfigMapper.ANDROIDSTRINGS_FILTER_CONFIG_ID) 
                && event.getEventType() == EventType.TEXT_UNIT) {
            // if source has escaped double-quotes, single-quotes, \r or \n, unescape
            TextUnit textUnit = (TextUnit) event.getTextUnit();
            String sourceString = textUnit.getSource().toString();
            String unescapedSourceString = unescape(sourceString);
            TextContainer source = new TextContainer(unescapedSourceString);
            textUnit.setSource(source);   
        }
        
        return event;
    }

    private String unescape(String text) {
        String unescapedText = text.replaceAll("(\\\\)(\"|')", "$2");
        unescapedText = unescapedText.replaceAll("\\\\n", "\n");
        unescapedText = unescapedText.replaceAll("\\\\r", "\r");
        return unescapedText;
    }
    
    @Override
    public EncoderManager getEncoderManager() {
        if (encoderManager == null) {
            encoderManager = new EncoderManager();  
        }
        encoderManager.setMapping(getMimeType(), "com.box.l10n.mojito.okapi.filters.XMLEncoder");
        return encoderManager;
    }
}
