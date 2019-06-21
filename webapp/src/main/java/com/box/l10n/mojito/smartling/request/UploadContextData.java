package com.box.l10n.mojito.smartling.request;

public class UploadContextData {

    String contextType;
    String contextUid;
    String created;
    String name;

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public String getContextUid() {
        return contextUid;
    }

    public void setContextUid(String contextUid) {
        this.contextUid = contextUid;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
