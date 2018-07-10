package com.box.l10n.mojito.rest.entity;

/**
 *
 * @author jaurambault
 */
public class XliffExportBody {

    String content;
    PollableTask pollableTask;

    public XliffExportBody() {
    }

    public XliffExportBody(String content) {
        this.content = content;
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
