package com.box.l10n.mojito.android.strings;

import com.google.common.base.Strings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static com.box.l10n.mojito.android.strings.AndroidStringDocumentUtils.TRANSFORMER_FACTORY;
import static com.box.l10n.mojito.android.strings.AndroidStringDocumentUtils.documentBuilder;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.ID_ATTRIBUTE_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.NAME_ATTRIBUTE_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.PLURAL_ELEMENT_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.PLURAL_ITEM_ELEMENT_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.QUANTITY_ATTRIBUTE_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.ROOT_ELEMENT_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.SINGULAR_ELEMENT_NAME;
import static java.util.Objects.requireNonNull;

public class AndroidStringDocumentWriter {

    private AndroidStringDocument source;
    private DOMSource domSource;
    private Document document;
    private Node root;

    public AndroidStringDocumentWriter(final AndroidStringDocument source) throws ParserConfigurationException {
        this.source = requireNonNull(source);
        buildDomSource();
    }

    public void buildDomSource() throws ParserConfigurationException {
        document = documentBuilder().newDocument();
        root = document.createElement(ROOT_ELEMENT_NAME);
        document.setXmlStandalone(true);
        document.appendChild(root);
        source.getStrings().forEach(this::addString);
        domSource = new DOMSource(document);
    }

    private <W extends Writer> W buildWriter(W writer) throws TransformerException {

        Transformer transformer = TRANSFORMER_FACTORY.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(domSource, new StreamResult(writer));

        return writer;
    }

    public String toText() throws TransformerException {
        return buildWriter(new StringWriter()).toString();
    }

    public void toFile(String path) throws IOException, TransformerException {
        buildWriter(new FileWriter(new File(path)));
    }

    private void addPlural(AndroidPlural plural) {
        addComment(root, plural.getComment());

        Element element = addPluralChild(root);
        setNameAttribute(element, plural.getName());
        plural.forEachItemSorted(item -> addPluralItemChild(element, item));
    }

    private void addString(AbstractAndroidString string) {
        if (string.isSingular()){
            addSingular((AndroidSingular) string);
        } else if (string.isPlural()) {
            addPlural((AndroidPlural) string);
        }
    }

    private void addSingular(AndroidSingular singular) {
        addComment(root, singular.getComment());
        addSingularChild(root, singular);
    }

    private void addSingularChild(Node node, AndroidSingular singular) {
        Element element = addChild(node, SINGULAR_ELEMENT_NAME);
        setContent(element, singular.getContent());
        setNameAttribute(element, singular.getName());
        setIdAttribute(element, singular.getId().toString());
    }

    private Element addPluralChild(Node node) {
        return addChild(node, PLURAL_ELEMENT_NAME);
    }

    private void addPluralItemChild(Node node, AndroidPluralItem item){
        Element element = addChild(node, PLURAL_ITEM_ELEMENT_NAME);
        setContent(element, item.getContent());
        setQuantityAttribute(element, item.getQuantity());
    }

    private void addComment(Node node, String comment){
        if (!Strings.isNullOrEmpty(comment)){
            node.appendChild(document.createComment(comment));
        }
    }

    private Element addChild(Node node, String name) {
        Element result = document.createElement(name);
        node.appendChild(result);
        return result;
    }

    private void setContent(Element element, String content) {
        if (!Strings.isNullOrEmpty(content)){
            element.setTextContent(escapeQuotes(content));
        }
    }

    private void setNameAttribute(Element element, String string) {
        setAttribute(element, NAME_ATTRIBUTE_NAME, string);
    }

    private void setIdAttribute(Element element, String string) {
        setAttribute(element, ID_ATTRIBUTE_NAME, string);
    }


    private void setQuantityAttribute(Element element, AndroidPluralQuantity quantity) {
        setAttribute(element, QUANTITY_ATTRIBUTE_NAME, quantity.toString());
    }

    private void setAttribute(Element element, String name, String value) {
        if (!Strings.isNullOrEmpty(value)) {
            element.setAttribute(name, value);
        }
    }

    static String escapeQuotes(String str) {
        return Strings.nullToEmpty(str)
                .replaceAll("\"", "\\\\\"")
                .replaceAll("\n", "\\\\n");
    }
}
