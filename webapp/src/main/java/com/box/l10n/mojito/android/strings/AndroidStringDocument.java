package com.box.l10n.mojito.android.strings;

import org.w3c.dom.Node;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class AndroidStringDocument {

    private List<AndroidSingular> singulars = new LinkedList<>();
    private List<AndroidPlural> plurals = new LinkedList<>();

    public List<AndroidSingular> getSingulars() {
        return singulars;
    }

    public List<AndroidPlural> getPlurals() {
        return plurals;
    }

    public void addSingular(AndroidSingular string) {
        singulars.add(string);
    }

    public void addSingular(AndroidStringElement element, Node comment) {

        checkArgument(element.isSingular(), "element should be singular");

        addSingular(new AndroidSingular(element.getIdAttribute(),
                element.getNameAttribute(),
                element.getUnescapedContent(),
                comment.getTextContent()));
    }

    public void addPlural(AndroidPlural plural) {
        plurals.add(plural);
    }

    public void addPlural(AndroidStringElement element, Node comment) {

        checkArgument(element.isPlural(), "element should be plural");

        AndroidPlural.AndroidPluralBuilder builder = AndroidPlural.builder();
        builder.setName(element.getNameAttribute());
        builder.setComment(comment.getTextContent());

        element.forEachPluralItem(builder::addItem);

        addPlural(builder.build());
    }

    public void addElement(Node node, Node comment){

        AndroidStringElement element = new AndroidStringElement(node);

        if (element.isSingular()) {
            addSingular(element, comment);
        }
        else if (element.isPlural()) {
            addPlural(element, comment);
        }
    }
}
