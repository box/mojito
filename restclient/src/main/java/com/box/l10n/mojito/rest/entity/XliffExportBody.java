package com.box.l10n.mojito.rest.entity;

/**
 *
 * @author jaurambault
 */
public class XliffExportBody {

    Long tmXliffId;
    String content;
    PollableTask pollableTask;

    public XliffExportBody() {
    }

    public XliffExportBody(String content) {
        this.content = content;
    }

    public Long getTmXliffId() {
        return tmXliffId;
    }

    public void setTmXliffId(Long tmXliffId) {
        this.tmXliffId = tmXliffId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PollableTask getPollableTask() {
        return pollableTask;
    }

    public void setPollableTask(PollableTask pollableTask) {
        this.pollableTask = pollableTask;
    }

}
