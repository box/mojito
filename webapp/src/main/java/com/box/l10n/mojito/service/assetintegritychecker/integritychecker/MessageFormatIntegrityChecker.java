package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.ibm.icu.text.MessageFormat;
import java.text.Format;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks the validity of the message format in the target content. 
 * 
 * <p>Checks that the target can be compiled into a message format. Then it
 * compares the number of format/placeholders in the target and the source 
 * content. If the number is different the an exception is thrown. To do this
 * the source is also compile into a message format. If the source is not valid
 * an error is thrown though not directly related to the target (still the 
 * string needs review).
 * 
 * @author wyau
 */
public class MessageFormatIntegrityChecker implements TextUnitIntegrityChecker {
    
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(MessageFormatIntegrityChecker.class);

    @Override
    public void check(String sourceContent, String targetContent) throws MessageFormatIntegrityCheckerException {
        
        MessageFormat targetMessageFormat = null;
        MessageFormat sourceMessageFormat = null;
        
        logger.debug("Check if the target pattern is valid");
        try {
            targetMessageFormat = new MessageFormat(targetContent);
        } catch (IllegalArgumentException iae) {
            throw new MessageFormatIntegrityCheckerException("Invalid pattern", iae);
        }
        
        logger.debug("Check if the source pattern is valid to compare the number of format/placeholder");
        try {
            sourceMessageFormat = new MessageFormat(sourceContent);
        } catch (IllegalArgumentException iae) {
            throw new MessageFormatIntegrityCheckerException("Invalid source pattern", iae);
        }
        
        logger.debug("Check number of format/placeholder in the source and target message formats is the same");
        int numberSourceFormats = sourceMessageFormat.getFormats().length;
        int numberTargetFormats = targetMessageFormat.getFormats().length;
                
        if(numberSourceFormats != numberTargetFormats) {
            throw new MessageFormatIntegrityCheckerException("Number of placeholders in source (" + numberSourceFormats + ") and target (" 
                    + numberTargetFormats + ") is different");
        }
            
        Set<String> sourceArgumentNames = sourceMessageFormat.getArgumentNames();
        Set<String> targetArgumentNames = targetMessageFormat.getArgumentNames();
        
        if(!sourceArgumentNames.equals(targetArgumentNames)) {
           throw new MessageFormatIntegrityCheckerException("Different placeholder name in source and target");
        }
    }
}