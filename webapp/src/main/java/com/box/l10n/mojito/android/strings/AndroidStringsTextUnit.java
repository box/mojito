package com.box.l10n.mojito.android.strings;

public class AndroidStringsTextUnit {

    private String comment;
    private String name;
    private String content;
    private String id;
    private String pluralForm;
    private String pluralFormOther;

    public AndroidStringsTextUnit(String name, String content, String comment, String id, String pluralForm, String pluralFormOther) {
        this.comment = comment;
        this.name = name;
        this.content = content;
        this.id = id;
        this.pluralForm = pluralForm;
        this.pluralFormOther = pluralFormOther;
    }

    public String getComment() {
        return comment;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public String getPluralForm() {
        return pluralForm;
    }

    public String getPluralFormOther() {
        return pluralFormOther;
    }
}
