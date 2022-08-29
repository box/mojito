package com.box.l10n.mojito.slack.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Channel {

  String id;

  long created;

  @JsonProperty("is_im")
  boolean isIm;

  @JsonProperty("is_org_shared")
  boolean isOrgShared;

  String user;

  @JsonProperty("last_read")
  String lastRead;

  Object latest;

  @JsonProperty("unread_count")
  long unreadCount;

  @JsonProperty("unread_count_display")
  long unreadCountDisplay;

  @JsonProperty("is_open")
  boolean isOpen;

  long priority;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getCreated() {
    return created;
  }

  public void setCreated(long created) {
    this.created = created;
  }

  public boolean isIm() {
    return isIm;
  }

  public void setIm(boolean im) {
    this.isIm = im;
  }

  public boolean isOrgShared() {
    return isOrgShared;
  }

  public void setOrgShared(boolean orgShared) {
    this.isOrgShared = orgShared;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getLastRead() {
    return lastRead;
  }

  public void setLastRead(String lastRead) {
    this.lastRead = lastRead;
  }

  public Object getLatest() {
    return latest;
  }

  public void setLatest(Object latest) {
    this.latest = latest;
  }

  public long getUnreadCount() {
    return unreadCount;
  }

  public void setUnreadCount(long unreadCount) {
    this.unreadCount = unreadCount;
  }

  public long getUnreadCountDisplay() {
    return unreadCountDisplay;
  }

  public void setUnreadCountDisplay(long unreadCountDisplay) {
    this.unreadCountDisplay = unreadCountDisplay;
  }

  public boolean isOpen() {
    return isOpen;
  }

  public void setOpen(boolean open) {
    this.isOpen = open;
  }

  public long getPriority() {
    return priority;
  }

  public void setPriority(long priority) {
    this.priority = priority;
  }
}
