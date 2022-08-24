package com.box.l10n.mojito.rest.resttemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 * <p/>
 * Provider that obtain the user name from the system and password from the prompt.
 * <p/>
 * It caches the first result, so subsequent request for credential does not require
 * user to re-enter in the console.
 */
@Component
public class SystemPromptCredentialProvider implements CredentialProvider {

    @Value("${user.name}")
    String systemUserName;

    @Value("${user.password}")
    String password;

    @Override
    public String getUsername() {
        return systemUserName;
    }

    @Override
    public String getPassword() {
        if (password == null) {
            System.out.println("Enter password for mojito user " + systemUserName + ": ");
            char[] readPassword = System.console().readPassword();
            password = new String(readPassword);
        }

        return password;
    }
}
