package com.box.l10n.mojito.smartling.request;

import java.util.Map;

public class StringToContextBinding {
    String bindingUid;
    String contextUid;
    String stringHashcode;
    Map coordinates;

    public String getBindingUid() {
        return bindingUid;
    }

    public void setBindingUid(String bindingUid) {
        this.bindingUid = bindingUid;
    }

    public String getContextUid() {
        return contextUid;
    }

    public void setContextUid(String contextUid) {
        this.contextUid = contextUid;
    }

    public String getStringHashcode() {
        return stringHashcode;
    }

    public void setStringHashcode(String stringHashcode) {
        this.stringHashcode = stringHashcode;
    }

    public Map getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Map coordinates) {
        this.coordinates = coordinates;
    }
}
