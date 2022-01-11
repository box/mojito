package com.box.l10n.mojito.slack.response;

import com.box.l10n.mojito.slack.request.User;

public class UserResponse extends BaseResponse {

    User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
