package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.phabricator.DifferentialDiff;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.phabricator.payload.QueryDiffsFields;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static com.box.l10n.mojito.cli.command.PhabricatorDiffInfoCommand.SKIP_I18N_CHECKS_FLAG;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    public void testCheckWithPhabricatorOverride() {
        QueryDiffsFields queryDiffsFields = new QueryDiffsFields();
        queryDiffsFields.setAuthorEmail("test@test.com");
        queryDiffsFields.setSourceControlBaseRevision("123");
        queryDiffsFields.setRevisionId("D123456");
        PhabricatorDiffInfoCommand phabricatorDiffInfoCommand = new PhabricatorDiffInfoCommand();
        DifferentialDiff differentialDiffMock = Mockito.mock(DifferentialDiff.class);
        DifferentialRevision differentialRevisionMock = Mockito.mock(DifferentialRevision.class);
        ConsoleWriter consoleWriterMock = Mockito.mock(ConsoleWriter.class);
        phabricatorDiffInfoCommand.diffId = "123456";
        phabricatorDiffInfoCommand.differentialDiff = differentialDiffMock;
        phabricatorDiffInfoCommand.differentialRevision = differentialRevisionMock;
        phabricatorDiffInfoCommand.consoleWriterAnsiCodeEnabledFalse = consoleWriterMock;
        when(differentialDiffMock.queryDiff("123456")).thenReturn(queryDiffsFields);
        when(differentialRevisionMock.getTestPlan(isA(String.class))).thenReturn("A string containing " + SKIP_I18N_CHECKS_FLAG);
        when(consoleWriterMock.a(isA(String.class))).thenReturn(consoleWriterMock);
        when(consoleWriterMock.println()).thenReturn(consoleWriterMock);
        phabricatorDiffInfoCommand.execute();
        verify(consoleWriterMock, times(1)).a("MOJITO_SKIP_I18N_CHECKS=true");
    }

    @Test
    public void testCheckWithNoPhabricatorOverride() {
        QueryDiffsFields queryDiffsFields = new QueryDiffsFields();
        queryDiffsFields.setAuthorEmail("test@test.com");
        queryDiffsFields.setSourceControlBaseRevision("123");
        queryDiffsFields.setRevisionId("D123456");
        PhabricatorDiffInfoCommand phabricatorDiffInfoCommand = new PhabricatorDiffInfoCommand();
        DifferentialDiff differentialDiffMock = Mockito.mock(DifferentialDiff.class);
        DifferentialRevision differentialRevisionMock = Mockito.mock(DifferentialRevision.class);
        ConsoleWriter consoleWriterMock = Mockito.mock(ConsoleWriter.class);
        phabricatorDiffInfoCommand.diffId = "123456";
        phabricatorDiffInfoCommand.differentialDiff = differentialDiffMock;
        phabricatorDiffInfoCommand.differentialRevision = differentialRevisionMock;
        phabricatorDiffInfoCommand.consoleWriterAnsiCodeEnabledFalse = consoleWriterMock;
        when(differentialDiffMock.queryDiff("123456")).thenReturn(queryDiffsFields);
        when(differentialRevisionMock.getTestPlan(isA(String.class))).thenReturn("A string containing test string things.");
        when(consoleWriterMock.a(isA(String.class))).thenReturn(consoleWriterMock);
        when(consoleWriterMock.println()).thenReturn(consoleWriterMock);
        phabricatorDiffInfoCommand.execute();
        verify(consoleWriterMock, times(0)).a("MOJITO_SKIP_I18N_CHECKS=true");
        verify(consoleWriterMock, times(1)).a("MOJITO_SKIP_I18N_CHECKS=false");
    }
}
