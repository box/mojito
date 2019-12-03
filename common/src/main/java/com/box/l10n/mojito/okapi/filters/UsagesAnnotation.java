package com.box.l10n.mojito.okapi.filters;

import java.util.Set;
import net.sf.okapi.common.annotation.IAnnotation;

/**
 *
 * @author jaurambault
 */
public class UsagesAnnotation implements IAnnotation {

    Set<String> usages;

    public UsagesAnnotation(Set<String> usageLocations) {
        this.usages = usageLocations;
    }

    public Set<String> getUsages() {
        return usages;
    }

}
