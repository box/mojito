package com.box.l10n.mojito.rest.resttemplate;

import org.springframework.stereotype.Component;

/**
 * @author wyau
 *
 * This is an interface to provide the username and password value
 */
@Component
public interface CredentialProvider {

    String getUsername();

    String getPassword();
}
