package com.box.l10n.mojito.thirdpartynotification.phabricator;

public enum PhabricatorIcon {
    INFO("\u2139\uFE0F"),
    WARNING("\u26A0\uFE0F"),
    STOP("\uD83D\uDED1");
    String str;

    PhabricatorIcon(String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }

    @Override
    public String toString() {
        return str;
    }
}
