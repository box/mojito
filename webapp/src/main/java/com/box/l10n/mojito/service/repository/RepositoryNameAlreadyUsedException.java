package com.box.l10n.mojito.service.repository;

/**
 *
 * @author jyi
 */
public class RepositoryNameAlreadyUsedException extends Exception {

    public RepositoryNameAlreadyUsedException(String message) {
        super(message);
    }

    public RepositoryNameAlreadyUsedException(Throwable cause) {
        super(cause);
    }
    
}
