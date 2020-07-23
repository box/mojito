package com.box.l10n.mojito.android.strings;

public class AndroidSingular extends AbstractAndroidString {

    private Long id;
    private String content;

    public AndroidSingular(Long id, String name, String content, String comment) {
        super(name, comment);
        this.id = id;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
