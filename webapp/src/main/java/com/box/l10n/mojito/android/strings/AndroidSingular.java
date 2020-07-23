package com.box.l10n.mojito.android.strings;

public class AndroidSingular {

    private Long id;
    private String comment;
    private String content;
    private String name;

    public AndroidSingular(Long id, String name, String content, String comment) {
        this.id = id;
        this.comment = comment;
        this.content = content;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getComment() {
        return comment;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
}
