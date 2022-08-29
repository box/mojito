package com.box.l10n.mojito.slack.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class Message {

  String channel;
  String text;
  List<Attachment> attachments = new ArrayList<>();

  @JsonProperty("thread_ts")
  String threadTs;

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments;
  }

  public String getThreadTs() {
    return threadTs;
  }

  public void setThreadTs(String threadTs) {
    this.threadTs = threadTs;
  }
}
