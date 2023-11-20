package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.Page;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

import static org.junit.Assert.assertTrue;

public class DropViewCommandTest extends CLITestBase {
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropViewCommandTest.class);

    @Autowired
    DropClient dropClient;

    @Test
    public void dropView() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(), "-s",
                getInputResourcesTestDir("source").getAbsolutePath());

        waitForRepositoryToHaveStringsForTranslations(repository.getId());

        getL10nJCommander().run("drop-export", "-r", repository.getName());

        Page<Drop> allDrops = dropClient.getDrops(repository.getId(), null, null, null);

        L10nJCommander l10nJCommander = getL10nJCommander();

        l10nJCommander.run("drop-view", "-r", repository.getName(), "--number-drop-fetched", "1000");

        String outputString = outputCapture.toString();

        for (Drop drop : allDrops.getContent()) {
            assertTrue(outputString.contains("name: " + drop.getName()));
            assertTrue(outputString.contains("id: " + drop.getId()));
        }
    }

    @Test
    public void dropViewJson() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(), "-s",
                getInputResourcesTestDir("source").getAbsolutePath());

        waitForRepositoryToHaveStringsForTranslations(repository.getId());

        getL10nJCommander().run("drop-export", "-r", repository.getName());

        Collection<Drop> drops = dropClient.getDrops(repository.getId(), null, null, null).getContent();

        L10nJCommander l10nJCommander = getL10nJCommander();

        l10nJCommander.run("drop-view", "-r", repository.getName(), "--number-drop-fetched", "1000", "--json");

        String outputString = outputCapture.toString();

        assertTrue(outputString.contains(getJsonOutput(drops)));
    }

    private String getJsonOutput(Collection<Drop> drops) {
        JSONArray jsonDrops = new JSONArray();
        for (Drop drop : drops) {
            JSONObject jsonDrop = new JSONObject();
            jsonDrop.put("id", drop.getId());
            jsonDrop.put("name", drop.getName());
            jsonDrop.put("lastImportedDate", drop.getLastImportedDate());
            jsonDrop.put("canceled", drop.getCanceled());
            jsonDrops.add(jsonDrop);
        }

        return jsonDrops.toJSONString();
    }

    @Test
    public void dropViewCsv() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(), "-s",
                getInputResourcesTestDir("source").getAbsolutePath());

        waitForRepositoryToHaveStringsForTranslations(repository.getId());

        getL10nJCommander().run("drop-export", "-r", repository.getName());

        Collection<Drop> drops = dropClient.getDrops(repository.getId(), null, null, null).getContent();

        L10nJCommander l10nJCommander = getL10nJCommander();

        l10nJCommander.run("drop-view", "-r", repository.getName(), "--number-drop-fetched", "1000", "--csv");

        String outputString = outputCapture.toString();

        assertTrue(outputString.contains(getCsvOutput(drops)));
    }

    private String getCsvOutput(Collection<Drop> drops) {
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,importStatus").append(System.getProperty("line.separator"));;

        for (Drop drop : drops) {
            csv.append(drop.getId()).append(",").append(drop.getName());

            if (Boolean.TRUE.equals(drop.getCanceled())) {
                csv.append(",CANCELED");
            } else if (drop.getLastImportedDate() == null) {
                csv.append(",NEW");
            } else {
                csv.append(",").append(drop.getLastImportedDate());
            }
            csv.append(System.getProperty("line.separator"));
        }

        return csv.toString();
    }
}
