package com.box.l10n.mojito.okapi.steps;

import net.sf.okapi.common.annotation.IAnnotation;

import java.util.function.Function;

public class OutputDocumentPostProcessingAnnotation implements IAnnotation {
    Function<String, String> postProcessing;
    boolean enabled;

    public OutputDocumentPostProcessingAnnotation(Function<String, String> postProcessing, boolean enabled) {
        this.postProcessing = postProcessing;
        this.enabled = enabled;
    }

    public Function<String, String> getPostProcessing() {
        return postProcessing;
    }

    public void setPostProcessing(Function<String, String> postProcessing) {
        this.postProcessing = postProcessing;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
