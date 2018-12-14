package com.box.l10n.mojito.slack;

public class ImOpenResponse extends BaseResponse {
    boolean no_op;
    boolean alreadyOpen;
    Channel channel;

    public boolean isNo_op() {
        return no_op;
    }

    public void setNo_op(boolean no_op) {
        this.no_op = no_op;
    }

    public boolean isAlreadyOpen() {
        return alreadyOpen;
    }

    public void setAlreadyOpen(boolean alreadyOpen) {
        this.alreadyOpen = alreadyOpen;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
