
package com.box.l10n.mojito.cli.filefinder;

import java.io.IOException;

/**
 * @author jaurambault
 */
public class FileFinderException extends Exception {

    public FileFinderException(String error_while_looking_for_source_files, IOException ex) {
        super(error_while_looking_for_source_files, ex);
    }

}
