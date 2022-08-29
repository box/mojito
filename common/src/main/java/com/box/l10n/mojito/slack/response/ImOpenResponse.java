package com.box.l10n.mojito.slack.response;

import com.box.l10n.mojito.slack.request.Channel;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImOpenResponse extends BaseResponse {

  @JsonProperty("no_op")
  boolean noOp;

  boolean alreadyOpen;

  Channel channel;

  public boolean isNoOp() {
    return noOp;
  }

  public void setNoOp(boolean noOp) {
    this.noOp = noOp;
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
