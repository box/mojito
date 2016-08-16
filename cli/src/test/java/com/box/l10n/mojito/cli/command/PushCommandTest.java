package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author wyau
 */
public class PushCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PushCommandTest.class);

    @Autowired
    AssetClient assetClient;
    
    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Test
    public void testCommandName() throws Exception {
        
        Repository repository = createTestRepoUsingRepoService();
        File sourceDirectory = getInputResourcesTestDir("source");
        
        logger.debug("Source directory is [{}]", sourceDirectory.getAbsoluteFile());   
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", sourceDirectory.getAbsolutePath());

        String outputString = outputCapture.toString();
        assertTrue(outputString.contains("--> asset id"));

        Matcher matcher = Pattern.compile("- Uploading:\\s*(.*?)\\s").matcher(outputString);
        matcher.find();
        String sourcePath = matcher.group(1);
        logger.debug("Source path is [{}]", sourcePath);

        Asset assetByPathAndRepositoryId = assetClient.getAssetByPathAndRepositoryId(sourcePath, repository.getId());
        assertEquals(sourcePath, assetByPathAndRepositoryId.getPath());
    }

    @Test
    public void testDeleteAsset() throws Exception {
        
        Repository repository = createTestRepoUsingRepoService();
        File parentSourceDirectory = getInputResourcesTestDir("delete");
        File childSourceDirectory = getInputResourcesTestDir("delete/dir");
        List<String> locales = Arrays.asList("fr-FR");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", parentSourceDirectory.getAbsolutePath());
        String outputString = outputCapture.toString();
        assertTrue(outputString.contains("--> asset id"));

        // delete/source-xliff.xliff has 5 strings
        // delete/dir/source-xliff.xliff has 5 strings
        checkNumberOfUsedUntranslatedTextUnit(repository, locales, 10);
        checkNumberOfUnusedUntranslatedTextUnit(repository, locales, 0);
        
        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", childSourceDirectory.getAbsolutePath());
        outputString = outputCapture.toString();
        assertTrue(outputString.contains("--> asset id"));
        
        // delete/source-xliff.xliff should be marked as deleted
        // delete/dir/source-xliff.xliff has 5 strings
        checkNumberOfUsedUntranslatedTextUnit(repository, locales, 5);
        checkNumberOfUnusedUntranslatedTextUnit(repository, locales, 5);

    }
    
    @Test
    public void testRenameAsset() throws Exception {
        
        Repository repository = createTestRepoUsingRepoService();
        File originalSourceDirectory = getInputResourcesTestDir("source");
        File renamedSourceDirectory = getInputResourcesTestDir("rename");
        List<String> locales = Arrays.asList("fr-FR");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", originalSourceDirectory.getAbsolutePath());
        String outputString = outputCapture.toString();
        assertTrue(outputString.contains("--> asset id"));

        // source/source-xliff.xliff has 5 strings
        checkNumberOfUsedUntranslatedTextUnit(repository, locales, 5);
        checkNumberOfUnusedUntranslatedTextUnit(repository, locales, 0);
        
        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", renamedSourceDirectory.getAbsolutePath());
        outputString = outputCapture.toString();
        assertTrue(outputString.contains("--> asset id"));
        
        // source/source-xliff.xliff should be marked as deleted
        // rename/source-xliff.xliff has 5 strings
        checkNumberOfUsedUntranslatedTextUnit(repository, locales, 5);
        checkNumberOfUnusedUntranslatedTextUnit(repository, locales, 5);
        
    }
    
    private void checkNumberOfUsedUntranslatedTextUnit(Repository repository, List<String> locales, int expectedNumberOfUnstranslated) {
        checkNumberOfUntranslatedTextUnit(repository, locales, true, expectedNumberOfUnstranslated);
    }
    
    private void checkNumberOfUnusedUntranslatedTextUnit(Repository repository, List<String> locales, int expectedNumberOfUnstranslated) {
        checkNumberOfUntranslatedTextUnit(repository, locales, false, expectedNumberOfUnstranslated);
    }
    
    private void checkNumberOfUntranslatedTextUnit(Repository repository, List<String> locales, boolean used, int expectedNumberOfUnstranslated) {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.UNTRANSLATED);
        textUnitSearcherParameters.setLocaleTags(locales);

        if (used) {
            textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        } else {
            textUnitSearcherParameters.setUsedFilter(UsedFilter.UNUSED);
        }
        
        List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
        assertEquals(expectedNumberOfUnstranslated, search.size());
        for (TextUnitDTO textUnitDTO : search) {
            assertEquals(used, textUnitDTO.isUsed());
        }
    }
    
}
