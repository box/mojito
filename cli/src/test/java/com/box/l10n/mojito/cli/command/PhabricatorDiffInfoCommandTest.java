package com.box.l10n.mojito.cli.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PhabricatorDiffInfoCommandTest {

    @Test
    public void testGetUsernameForAuthorEmailNull() {
        PhabricatorDiffInfoCommand phabricatorDiffInfoCommand = new PhabricatorDiffInfoCommand();
        String usernameForAuthorEmail = phabricatorDiffInfoCommand.getUsernameForAuthorEmail(null);
        assertEquals(null, usernameForAuthorEmail);
    }

    @Test
    public void testGetUsernameForAuthorEmail() {
        PhabricatorDiffInfoCommand phabricatorDiffInfoCommand = new PhabricatorDiffInfoCommand();
        String usernameForAuthorEmail = phabricatorDiffInfoCommand.getUsernameForAuthorEmail("username@test.com");
        assertEquals("username", usernameForAuthorEmail);
    }

    @Test
    public void testGetUsernameForAuthorInvalid() {
        PhabricatorDiffInfoCommand phabricatorDiffInfoCommand = new PhabricatorDiffInfoCommand();
        String usernameForAuthorEmail = phabricatorDiffInfoCommand.getUsernameForAuthorEmail("notanemail");
        assertEquals("notanemail", usernameForAuthorEmail);
    }
}