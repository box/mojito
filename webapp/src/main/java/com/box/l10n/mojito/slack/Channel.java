package com.box.l10n.mojito.slack;

public class Channel {
    String id;
    long created;
    boolean is_im;
    boolean is_org_shared;
    String user;
    String last_read;
    Object latest;
    long unread_count;
    long unread_count_display;
    boolean is_open;
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

    public boolean isIs_im() {
        return is_im;
    }

    public void setIs_im(boolean is_im) {
        this.is_im = is_im;
    }

    public boolean isIs_org_shared() {
        return is_org_shared;
    }

    public void setIs_org_shared(boolean is_org_shared) {
        this.is_org_shared = is_org_shared;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getLast_read() {
        return last_read;
    }

    public void setLast_read(String last_read) {
        this.last_read = last_read;
    }

    public Object getLatest() {
        return latest;
    }

    public void setLatest(Object latest) {
        this.latest = latest;
    }

    public long getUnread_count() {
        return unread_count;
    }

    public void setUnread_count(long unread_count) {
        this.unread_count = unread_count;
    }

    public long getUnread_count_display() {
        return unread_count_display;
    }

    public void setUnread_count_display(long unread_count_display) {
        this.unread_count_display = unread_count_display;
    }

    public boolean isIs_open() {
        return is_open;
    }

    public void setIs_open(boolean is_open) {
        this.is_open = is_open;
    }

    public long getPriority() {
        return priority;
    }

    public void setPriority(long priority) {
        this.priority = priority;
    }
}
