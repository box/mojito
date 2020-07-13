package com.box.l10n.mojito.okapi.extractor;

import java.util.Set;

public class AssetExtractorTextUnit {
    String name;
    String source;
    String comments;
    String pluralForm;
    String pluralFormOther;
    Set<String> usages;

    // TODO the best place to get refactoring information is when using the CLI diff command, since we can actually
    // see a name change, or contentn change.
    //
    // Look amongst the removed text units for a match to newly added text units
    // in other words, the previous text unit must be in "removed" list from a diff and match should be unique
    // match first by content and comment --> change of id (adding cnotext in PO files)
    // match by name --> changing the english, this is not refactoring though ... but we maybe to keep the previous string for
    // match by content --> changed both the ID and added comments
    // previous is only set if there is a unique match, else we can't get for a fact what happened and will just do
    // some kind of leveraging..
    String previousMd5;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getPluralForm() {
        return pluralForm;
    }

    public void setPluralForm(String pluralForm) {
        this.pluralForm = pluralForm;
    }

    public String getPluralFormOther() {
        return pluralFormOther;
    }

    public void setPluralFormOther(String pluralFormOther) {
        this.pluralFormOther = pluralFormOther;
    }

    public Set<String> getUsages() {
        return usages;
    }

    public void setUsages(Set<String> usages) {
        this.usages = usages;
    }
}
