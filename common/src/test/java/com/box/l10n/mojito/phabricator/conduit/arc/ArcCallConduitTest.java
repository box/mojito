package com.box.l10n.mojito.phabricator.conduit.arc;

import com.box.l10n.mojito.phabricator.conduit.payload.Data;
import com.box.l10n.mojito.phabricator.conduit.payload.ResponseWithError;
import com.box.l10n.mojito.phabricator.conduit.payload.RevisionSearchFields;
import com.box.l10n.mojito.shell.Shell;
import com.box.l10n.mojito.test.IOTestBase;
import com.google.common.io.Files;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ArcCallConduitTest extends IOTestBase {

    static Logger logger = LoggerFactory.getLogger(ArcCallConduitTest.class);

    @Test
    public void getRevisionForTargetPhid() {
        ArcCallConduitShell arcCallConduitShell = getStubWithRecorder();
        ArcCallConduit arcCallConduit = new ArcCallConduit(arcCallConduitShell);
        Data<RevisionSearchFields> revisionForTargetPhid = arcCallConduit.getRevisionForTargetPhid("PHID-HMBT-sometest");
        assertEquals(49, (long) revisionForTargetPhid.getId());
    }

    @Test
    public void getBuildPHID() {
        ArcCallConduitShell arcCallConduitShell = getStubWithRecorder();
        ArcCallConduit arcCallConduit = new ArcCallConduit(arcCallConduitShell);
        String buildPHID = arcCallConduit.getBuildPHID("PHID-HMBT-sometest");
        assertEquals("PHID-HMBD-sometest", buildPHID);
    }

    @Test
    public void getBuildablePHID() {
        ArcCallConduitShell arcCallConduitShell = getStubWithRecorder();
        ArcCallConduit arcCallConduit = new ArcCallConduit(arcCallConduitShell);
        String buildablePHID = arcCallConduit.getBuildablePHID("PHID-HMBD-sometest");
        assertEquals("PHID-HMBB-sometest", buildablePHID);
    }

    @Test
    public void getObjectPHID() {
        ArcCallConduitShell arcCallConduitShell = getStubWithRecorder();
        ArcCallConduit arcCallConduit = new ArcCallConduit(arcCallConduitShell);
        String buildablePHID = arcCallConduit.getObjectPHID("PHID-HMBB-sometest");
        assertEquals("PHID-DIFF-sometest", buildablePHID);
    }

    @Test
    public void getRevisionPHID() {
        ArcCallConduitShell arcCallConduitShell = getStubWithRecorder();
        ArcCallConduit arcCallConduit = new ArcCallConduit(arcCallConduitShell);
        String buildablePHID = arcCallConduit.getRevisionPHID("PHID-DIFF-sometest");
        assertEquals("PHID-DREV-sometest", buildablePHID);
    }

    @Test
    public void getRevision() {
        ArcCallConduitShell arcCallConduitShell = getStubWithRecorder();
        ArcCallConduit arcCallConduit = new ArcCallConduit(arcCallConduitShell);
        Data<RevisionSearchFields> revisionSearchFieldsData = arcCallConduit.getRevision("PHID-DREV-sometest");
        assertEquals(49, (long) revisionSearchFieldsData.getId());
    }

    @Test
    public void getConstraintsForPHID() {
    }

    @Test(expected = RuntimeException.class)
    public void throwIfError() {
        ArcCallConduit arcCallConduit = new ArcCallConduit(null);
        arcCallConduit.throwIfError(new ResponseWithError() {
            @Override
            public String getErrorMessage() {
                return "some error";
            }
        });
    }

    /**
     * A stub that potentially makes real call and save the result into files for replay
     *
     * @return
     */
    ArcCallConduitShell getStubWithRecorder() {
        return new ArcCallConduitShell(new Shell()) {

            boolean overrideFiles = false;
            boolean shouldWrite = false;
            boolean overrideIds = true;

            @Override
            String callShell(String command) {
                try {
                    String output;
                    String filename = command.replaceAll(".*(PHID-.*?-.*?)\".*arc call-conduit (.*)", "$2_$1") + ".json";

                    // substitue with real ID for testing against a real fabricator instance
                    command = command.
                            replace("PHID-HMBT-sometest", "PHID-HMBT-").
                            replace("PHID-HMBT-sometest", "PHID-HMBT-").
                            replace("PHID-HMBD-sometest", "PHID-HMBD-").
                            replace("PHID-HMBB-sometest", "PHID-HMBB-").
                            replace("PHID-DIFF-sometest", "PHID-DIFF-").
                            replace("PHID-DREV-sometest", "PHID-DREV-");

                    File outputFile = getInputResourcesTestDir().toPath().resolve(filename).toFile();
                    if (overrideFiles || !outputFile.exists()) {
                        if (!shouldWrite) {
                            fail("Stub not setup to write, fail, you can set shouldWrite = true.");
                        }

                        logger.info("No local data for this test, call shell");
                        outputFile.getParentFile().mkdirs();
                        output = super.callShell(command);

                        if (overrideIds) {
                            output = output.
                                    replaceAll("\"PHID-(.*?)-(.*?)\"", "\"PHID-$1-sometest\"").
                                    replaceAll("id\":(\\d\\d)\\d+", "id\": $1").
                                    replaceAll("identifier\":\"(\\w)\\w*?(\\w)", "identifier\":\"$1$2");
                        }

                        Files.write(output, outputFile, StandardCharsets.UTF_8);
                    } else {
                        logger.debug("Read local data from file");
                        output = Files.toString(outputFile, StandardCharsets.UTF_8);
                    }

                    return output;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }
}