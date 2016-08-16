package com.box.l10n.mojito.cli;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Just to flag code that should be removed when we decommissioned the
 * Webapp TMS
 *
 * @author jaurambault
 */
@Retention(RetentionPolicy.SOURCE)
public @interface WebappTMS {

}
