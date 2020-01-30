package com.box.l10n.mojito.phabricator.conduit.arc;

import com.box.l10n.mojito.phabricator.conduit.Method;
import com.box.l10n.mojito.phabricator.conduit.payload.BuildableSearchResponse;
import com.box.l10n.mojito.phabricator.conduit.payload.Constraints;
import com.box.l10n.mojito.shell.Result;
import com.box.l10n.mojito.shell.Shell;
import com.box.l10n.mojito.test.IOTestBase;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ArcCallConduitShellTest extends IOTestBase {

    @Test
    public void callConduit() {
        String command = "echo '{\"constraints\":{\"phids\":[\"PHID-HMBB-sometest\"]}}' | arc call-conduit harbormaster.buildable.search";
        String shellOutput = readInputResource("harbormaster.buildable.search_PHID-HMBB-sometest.json");

        Shell mock = Mockito.mock(Shell.class);
        doReturn(new Result(0, shellOutput, null)).when(mock).exec(command);

        ArcCallConduitShell arcCallConduitShell = new ArcCallConduitShell(mock);

        Constraints constraints = new Constraints();
        constraints.setPhids(Arrays.asList("PHID-HMBB-sometest"));
        BuildableSearchResponse buildableSearchResponse = arcCallConduitShell.callConduit(Method.HARBORMASTER_BUILDABLE_SEARCH, constraints, BuildableSearchResponse.class);
        assertEquals("PHID-DIFF-sometest", buildableSearchResponse.getResponse().getData().get(0).getFields().getObjectPHID());
    }

    @Test
    public void callShell() throws IOException, InterruptedException {
        Shell mock = Mockito.mock(Shell.class);
        doReturn(new Result(0, "output", null)).when(mock).exec("testcommand");

        ArcCallConduitShell arcCallConduitShell = new ArcCallConduitShell(mock);
        assertEquals("output", arcCallConduitShell.callShell("testcommand"));
        verify(mock, times(1)).exec("testcommand");
    }

    @Test(expected = RuntimeException.class)
    public void callShellWrongCommand() {
        ArcCallConduitShell arcCallConduitShell = new ArcCallConduitShell(new Shell());
        arcCallConduitShell.callShell("wrong command");
    }

    @Test
    public void getCommand() {
        ArcCallConduitShell arcCallConduitShell = new ArcCallConduitShell(new Shell());

        Constraints constraints = new Constraints();
        constraints.setPhids(Arrays.asList("some-phid"));
        String command = arcCallConduitShell.getCommand(Method.HARBORMASTER_BUILDABLE_SEARCH, constraints);
        assertEquals("echo '{\"constraints\":{\"phids\":[\"some-phid\"]}}' | arc call-conduit harbormaster.buildable.search", command);
    }
}