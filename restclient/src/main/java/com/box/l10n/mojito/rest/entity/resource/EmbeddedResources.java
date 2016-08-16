package com.box.l10n.mojito.rest.entity.resource;

import org.springframework.hateoas.hal.ResourcesMixin;
import java.util.Collection;

/**
 * @author wyau
 */
public class EmbeddedResources<T> extends ResourcesMixin<T> {

    private Collection<T> content;

    public void setContent(Collection<T> content) {
        this.content = content;
    }

    @Override
    public Collection<T> getContent() {
        return content;
    }

    /**
     * Checks to see if there is actual content
     * @return
     */
    public boolean hasContent() {
        return (content != null && content.size() > 0);
    }
}
