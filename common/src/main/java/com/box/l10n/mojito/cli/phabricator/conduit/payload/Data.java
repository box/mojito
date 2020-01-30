package com.box.l10n.mojito.cli.phabricator.conduit.payload;

public class Data<FieldsT> {
    Long id;
    String type;
    FieldsT fields;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FieldsT getFields() {
        return fields;
    }

    public void setFields(FieldsT fields) {
        this.fields = fields;
    }
}
