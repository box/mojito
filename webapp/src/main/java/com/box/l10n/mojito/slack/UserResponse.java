package com.box.l10n.mojito.slack;

public class UserResponse extends BaseResponse {

    User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
