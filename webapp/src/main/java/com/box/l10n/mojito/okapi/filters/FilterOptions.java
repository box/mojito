package com.box.l10n.mojito.okapi.filters;

import com.google.common.base.Splitter;
import net.sf.okapi.common.annotation.IAnnotation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FilterOptions implements IAnnotation {

    Map<String, String> options;

    public FilterOptions(String options) {
        if (options != null) {
            this.options = Splitter.on(";").withKeyValueSeparator("=").split(options);
        } else {
            this.options = new HashMap<>();
        }
    }

    public void getString(String key, Consumer<String> consumer) {
        if (this.options.containsKey(key)) {
            consumer.accept(this.options.get(key));
        }
    }

    public void getBoolean(String key, Consumer<Boolean> consumer) {
        if (this.options.containsKey(key)) {
            consumer.accept(Boolean.valueOf(this.options.get(key)));
        }
    }
}
