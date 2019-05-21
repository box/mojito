package com.box.l10n.mojito.smartling.request;

public class File {
    String fileUri;
    String lastUploaded;
    String created;
    String fileType;
    Boolean hasInstructions;

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public String getLastUploaded() {
        return lastUploaded;
    }

    public void setLastUploaded(String lastUploaded) {
        this.lastUploaded = lastUploaded;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Boolean getHasInstructions() {
        return hasInstructions;
    }

    public void setHasInstructions(Boolean hasInstructions) {
        this.hasInstructions = hasInstructions;
    }

}
