package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.resource.TextFragment;

/**
 * Indicates if codes in text should be converted to Html code as provided by {@link
 * net.sf.okapi.lib.translation.QueryUtil#toCodedHTML(TextFragment)}
 *
 * @author jaurambault
 */
public class ConvertToHtmlCodesAnnotation implements IAnnotation {}
