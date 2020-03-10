package com.box.l10n.mojito.phabricator.payload;

import java.util.List;

public class ListResult<FieldsT> {
    List<Data<FieldsT>> data;

    public List<Data<FieldsT>> getData() {
        return data;
    }

    public void setData(List<Data<FieldsT>> data) {
        this.data = data;
    }
}
