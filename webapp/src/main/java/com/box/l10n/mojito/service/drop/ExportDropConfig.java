package com.box.l10n.mojito.service.drop;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.TranslationKit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Configuration to trigger the export drop process, see
 * {@link DropService#startDropExportProcess(ExportDropConfig, PollableTask)}
 *
 * @author jaurambault
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportDropConfig {

    @JsonProperty(required = true)
    Long repositoryId;

    Long dropId;

    @JsonProperty("locales")
    List<String> bcp47Tags = new ArrayList<>();

    Date uploadTime;

    PollableTask pollableTask;

    TranslationKit.Type type = TranslationKit.Type.TRANSLATION;

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Long getDropId() {
        return dropId;
    }

    public void setDropId(Long dropId) {
        this.dropId = dropId;
    }

    public List<String> getBcp47Tags() {
        return bcp47Tags;
    }

    public void setBcp47Tags(List<String> bcp47Tags) {
        this.bcp47Tags = bcp47Tags;
    }

    public Date getUploadTime() {

        if (uploadTime == null) {
            uploadTime = new Date();
        }

        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    @JsonProperty
    public PollableTask getPollableTask() {
        return pollableTask;
    }

    public TranslationKit.Type getType() {
        return type;
    }

    public void setType(TranslationKit.Type type) {
        this.type = type;
    }

    /**
     * @JsonIgnore because this pollableTask is read only data generated by the
     * server side, it is not aimed to by external process via WS
     *
     * @param pollableTask
     */
    @JsonIgnore
    public void setPollableTask(PollableTask pollableTask) {
        this.pollableTask = pollableTask;
    }

}
