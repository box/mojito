package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author aloison
 */
public class AssetExtractionServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = getLogger(AssetExtractionServiceTest.class);

    @Autowired
    AssetService assetService;

    @Autowired
    AssetContentService assetContentService;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    BranchService branchService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    /**
     * Set up for tests
     * @param content   content to be processed
     * @param assetPath path to asset
     * @param branch
     * @return A list of AssetTextUnits from the content
     * @throws Exception
     */
    private List<AssetTextUnit> getAssetTextUnits(String content, String assetPath, String branch) throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        return getAssetTextUnits(repository, content, assetPath, branch);
    }

    private List<AssetTextUnit> getAssetTextUnits(Repository repository, String content, String assetPath, String branch) throws Exception {

        Asset asset = assetService.createAsset(repository.getId(), assetPath, false);
        AssetContent assetContent = assetContentService.createAssetContent(asset, content);

        assetExtractionService.processAssetAsync(null, assetContent.getId(), null, null).get();

        Asset processedAsset = assetRepository.findOne(asset.getId());

        return getAssetTextUnitsWithUsages(processedAsset);
    }

    /**
     * Fetches data from proxy and keeps session open for all tests
     * @param processedAsset    asset that has previously been processed
     * @return A list of AssetTextUnits with usages that have been extracted from the processedAsset
     */
    @Transactional
    List<AssetTextUnit> getAssetTextUnitsWithUsages(Asset processedAsset) {

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());
        for (AssetTextUnit assetTextUnit: assetTextUnits) {
            // fetch data from proxy
            assetTextUnit.getUsages().isEmpty();
        }
        return assetTextUnits;
    }

    @Test
    public void testProcessAssetShouldProcessCsvFiles() throws Exception {

        String csvContent = "german_shepherd,german shepherd,Berger allemand,,\n"
                + "husky,husky,Husky,,\n"
                + "fox_cub,fox cub,renardeau,fox cub description,";

        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(csvContent, "path/to/fake/file.csv", null);

        assertEquals("Processing should have extracted 3 text units", 3, assetTextUnits.size());

        AssetTextUnit assetTextUnit = assetTextUnits.get(2);
        assertEquals("fox_cub", assetTextUnit.getName());
        assertEquals("fox cub", assetTextUnit.getContent());
        assertEquals("fox cub description", assetTextUnit.getComment());
        assertNotNull(assetTextUnit.getMd5());
        assertNotNull(assetTextUnit.getContentMd5());
    }

    @Test
    public void testProcessAssetShouldProcessXliffFiles() throws Exception {

        String xliffContent = xliffDataFactory.generateSourceXliff(Arrays.asList(
                xliffDataFactory.createTextUnit(1L, "2_factor_challenge_buttom", "Submit", null),
                xliffDataFactory.createTextUnit(2L, "2fa_confirmation_code", "Confirmation code", null),
                xliffDataFactory.createTextUnit(3L, "Account_security_and_password_settings", "Account security and password settings", "Label on table header")
        ));

        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(xliffContent, "path/to/fake/file.xliff", null);

        assertEquals("Processing should have extracted 3 text units", 3, assetTextUnits.size());

        AssetTextUnit assetTextUnit = assetTextUnits.get(2);
        assertEquals("Account_security_and_password_settings", assetTextUnit.getName());
        assertEquals("Account security and password settings", assetTextUnit.getContent());
        assertEquals("Label on table header", assetTextUnit.getComment());
        assertNotNull(assetTextUnit.getMd5());
        assertNotNull(assetTextUnit.getContentMd5());
    }

    @Test
    public void testSkipDuplicates() throws Exception {

        String xliffContent = xliffDataFactory.generateSourceXliff(Arrays.asList(
                xliffDataFactory.createTextUnit(1L, "Account_security_and_password_settings", "Account security and password settings", "Label on table header"),
                xliffDataFactory.createTextUnit(2L, "Account_security_and_password_settings", "Account security and password settings", "Label on table header")
        ));

        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(xliffContent, "path/to/fake/file.xliff", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());

        AssetTextUnit assetTextUnit = assetTextUnits.get(0);
        assertEquals("Account_security_and_password_settings", assetTextUnit.getName());
        assertEquals("Account security and password settings", assetTextUnit.getContent());
        assertEquals("Label on table header", assetTextUnit.getComment());
        assertNotNull(assetTextUnit.getMd5());
        assertNotNull(assetTextUnit.getContentMd5());
    }

    @Test
    public void testProcessAssetShouldThrowIfUnsupportedAssetType() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        Asset asset = assetService.createAsset(repository.getId(), "path/to/fake/file-with-unsupported.ext", false);
        AssetContent assetContent = assetContentService.createAssetContent(asset, "fake-content");
        PollableFuture pollableTaskResult = assetExtractionService.processAssetAsync(null, assetContent.getId(), null, null);

        // Wait for the processing to finish
        try {
            pollableTaskResult.get();
            fail("An exception should have been thrown");
        } catch (ExecutionException e) {
            Throwable originalException = e.getCause();
            assertTrue(originalException instanceof PollableTaskException);

            logger.debug("\n===============\nThe exception thrown above is expected. Do not panic!\n===============\n");
        }
    }

    @Test
    public void testResx() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<root>\n"
                + "  <data name=\"Test\" xml:space=\"preserve\">\n"
                + "    <value>You must test your changes</value>\n"
                + "	<comment>Test label</comment>\n"
                + "  </data>\n"
                + "</root>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/Test.resx", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("You must test your changes", assetTextUnits.get(0).getContent());
        assertEquals("Test label", assetTextUnits.get(0).getComment());

    }

    @Test
    public void testResw() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<root>\n"
                + "  <data name=\"Test\" xml:space=\"preserve\">\n"
                + "    <value>You must test your changes</value>\n"
                + "	<comment>Test label</comment>\n"
                + "  </data>\n"
                + "</root>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/Test.resw", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("You must test your changes", assetTextUnits.get(0).getContent());
        assertEquals("Test label", assetTextUnits.get(0).getComment());

    }

    @Test
    public void testAndroidStrings() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string name=\"Test\" description=\"Test label\">You must test your changes</string>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("You must test your changes", assetTextUnits.get(0).getContent());
        assertEquals("Test label", assetTextUnits.get(0).getComment());

    }

    @Test
    public void testAndroidStringsWithSpecialCharacters() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string name=\"test\">Make sure you\\\'d \\\"escaped\\\" <b>special</b> characters like quotes &amp; ampersands.\\n</string>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("test", assetTextUnits.get(0).getName());
        assertEquals("Make sure you'd \"escaped\" <b>special</b> characters like quotes & ampersands.\n", assetTextUnits.get(0).getContent());

    }

    @Test
    public void testAndroidStringsArray() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string-array name=\"$x_Collaborators\" description=\"Reference to number of collaborators in folders\">\n"
                + "    <item>No people</item>\n"
                + "    <item>1 person</item>\n"
                + "    <item>%1$d people</item>\n"
                + "  </string-array>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);

        assertEquals("Processing should have extracted 3 text units", 3, assetTextUnits.size());

        assertEquals("$x_Collaborators_0", assetTextUnits.get(0).getName());
        assertEquals("No people", assetTextUnits.get(0).getContent());
        assertEquals("Reference to number of collaborators in folders", assetTextUnits.get(0).getComment());

        assertEquals("$x_Collaborators_1", assetTextUnits.get(1).getName());
        assertEquals("1 person", assetTextUnits.get(1).getContent());
        assertEquals("Reference to number of collaborators in folders", assetTextUnits.get(1).getComment());

        assertEquals("$x_Collaborators_2", assetTextUnits.get(2).getName());
        assertEquals("%1$d people", assetTextUnits.get(2).getContent());
        assertEquals("Reference to number of collaborators in folders", assetTextUnits.get(2).getComment());
    }

    @Test
    public void testAndroidStringsArrayWithEmptyItem() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string-array name=\"N_items_failed_to_move\">\n"
                + "        <item />\n"
                + "        <item>1 item failed to move</item>\n"
                + "        <item>%1$d items failed to move</item>\n"
                + "    </string-array>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);

        assertEquals("Processing should have extracted 2 text units", 2, assetTextUnits.size());

        assertEquals("N_items_failed_to_move_1", assetTextUnits.get(0).getName());
        assertEquals("1 item failed to move", assetTextUnits.get(0).getContent());

        assertEquals("N_items_failed_to_move_2", assetTextUnits.get(1).getName());
        assertEquals("%1$d items failed to move", assetTextUnits.get(1).getContent());
    }

    @Test
    public void testAndroidStringsPlural() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <plurals name=\"numberOfCollaborators\" description=\"Example of plurals\">\n"
                + "    <item quantity=\"zero\">No people</item>\n"
                + "    <item quantity=\"one\">1 person</item>\n"
                + "    <item quantity=\"few\">few people</item>\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);

        assertEquals("Processing should have extracted 6 text units", 6, assetTextUnits.size());

        assertEquals("numberOfCollaborators_zero", assetTextUnits.get(0).getName());
        assertEquals("No people", assetTextUnits.get(0).getContent());
        assertEquals("Example of plurals", assetTextUnits.get(0).getComment());

        assertEquals("numberOfCollaborators_one", assetTextUnits.get(1).getName());
        assertEquals("1 person", assetTextUnits.get(1).getContent());
        assertEquals("Example of plurals", assetTextUnits.get(1).getComment());

        assertEquals("numberOfCollaborators_two", assetTextUnits.get(2).getName());
        assertEquals("%1$d people", assetTextUnits.get(2).getContent());
        assertEquals("Example of plurals", assetTextUnits.get(2).getComment());

        assertEquals("numberOfCollaborators_few", assetTextUnits.get(3).getName());
        assertEquals("few people", assetTextUnits.get(3).getContent());
        assertEquals("Example of plurals", assetTextUnits.get(3).getComment());

        assertEquals("numberOfCollaborators_many", assetTextUnits.get(4).getName());
        assertEquals("%1$d people", assetTextUnits.get(4).getContent());
        assertEquals("Example of plurals", assetTextUnits.get(4).getComment());

        assertEquals("numberOfCollaborators_other", assetTextUnits.get(5).getName());
        assertEquals("%1$d people", assetTextUnits.get(5).getContent());
        assertEquals("Example of plurals", assetTextUnits.get(5).getComment());
    }

    @Test
    public void testAndroidStringsPluralExtra() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <!-- Example of plurals -->\n"
                + "  <plurals name=\"numberOfCollaborators\">\n"
                + "    <item quantity=\"one\">1 person</item>\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);

        List<String> expectedTextUnits = new ArrayList<>();
        expectedTextUnits.add("numberOfCollaborators_zero");
        expectedTextUnits.add("numberOfCollaborators_one");
        expectedTextUnits.add("numberOfCollaborators_two");
        expectedTextUnits.add("numberOfCollaborators_few");
        expectedTextUnits.add("numberOfCollaborators_many");
        expectedTextUnits.add("numberOfCollaborators_other");


        assertEquals("Processing should have extracted 6 text units", 6, assetTextUnits.size());

        for (int i = 0; i < assetTextUnits.size(); i++) {
            assertEquals(expectedTextUnits.get(i), assetTextUnits.get(i).getName());

            if ( i != 1 )
                assertEquals("%1$d people", assetTextUnits.get(i).getContent());
            else
                assertEquals("1 person", assetTextUnits.get(i).getContent());
            assertEquals("Example of plurals", assetTextUnits.get(i).getComment());

        }
        
    }

    @Test
    public void testAndroidStringsPluralExtraOpeningAndClosing() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <!-- Example of plurals -->\n"
                + "  <plurals name=\"numberOfCollaborators\">\n"
                + "    <item quantity=\"one\">1 person</item>\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "  <!-- Example of plurals2 -->\n"
                + "  <plurals name=\"numberOfCollaborators2\">\n"
                + "    <item quantity=\"other\">%1$d people2</item>\n"
                + "  </plurals>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);
        int size = assetTextUnits.size();
        List<AssetTextUnit> headAssetTextUnits = assetTextUnits.subList(0, size / 2);
        List<AssetTextUnit> tailAssetTextUnits = assetTextUnits.subList(size / 2, size);

        List<String> expectedHeadTextUnits = new ArrayList<>();
        expectedHeadTextUnits.add("numberOfCollaborators_zero");
        expectedHeadTextUnits.add("numberOfCollaborators_one");
        expectedHeadTextUnits.add("numberOfCollaborators_two");
        expectedHeadTextUnits.add("numberOfCollaborators_few");
        expectedHeadTextUnits.add("numberOfCollaborators_many");
        expectedHeadTextUnits.add("numberOfCollaborators_other");

        List<String> expectedTailTextUnits = new ArrayList<>();
        expectedTailTextUnits.add("numberOfCollaborators2_zero");
        expectedTailTextUnits.add("numberOfCollaborators2_one");
        expectedTailTextUnits.add("numberOfCollaborators2_two");
        expectedTailTextUnits.add("numberOfCollaborators2_few");
        expectedTailTextUnits.add("numberOfCollaborators2_many");
        expectedTailTextUnits.add("numberOfCollaborators2_other");

        assertEquals("Processing should have extracted 12 text units", 12, assetTextUnits.size());

        for (int i = 0; i < headAssetTextUnits.size(); i++) {
            assertEquals(expectedHeadTextUnits.get(i), headAssetTextUnits.get(i).getName());

            if (i == 1)
                assertEquals("1 person", headAssetTextUnits.get(i).getContent());
            else
                assertEquals("%1$d people", headAssetTextUnits.get(i).getContent());
            assertEquals("Example of plurals", headAssetTextUnits.get(i).getComment());
        }

        for (int i = 0; i < tailAssetTextUnits.size(); i++) {
            assertEquals(expectedTailTextUnits.get(i), tailAssetTextUnits.get(i).getName());
            assertEquals("%1$d people2", tailAssetTextUnits.get(i).getContent());
            assertEquals("Example of plurals2", tailAssetTextUnits.get(i).getComment());
        }

    }

    @Test
    public void testMacStrings() throws Exception {

        String content = "/* Title: Title for the add content to folder menu header */\n"
                + "\"Add to Folder\" = \"Add to Folder\";";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/en.lproj/Localizable.strings", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Add to Folder", assetTextUnits.get(0).getName());
        assertEquals("Add to Folder", assetTextUnits.get(0).getContent());
        assertEquals(" Title: Title for the add content to folder menu header ", assetTextUnits.get(0).getComment());

    }

    @Test
    public void testMacStringsNamesWithoutDoubleQuotes() throws Exception {

        String content = "/* Comment 1 */\n"
                + "NSUsageDescription 1 = \"Add to Folder 1\";\n"
                + "NSUsageDescription 2 = \"Add to Folder 2\";\n\n"
                + "/* Comment 3 */\n"
                + "NSUsageDescription 3 = \"Add to Folder 3\";";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/en.lproj/Localizable.strings", null);

        assertEquals("Processing should have extracted 3 text units", 3, assetTextUnits.size());
        assertEquals("NSUsageDescription 1", assetTextUnits.get(0).getName());
        assertEquals("Add to Folder 1", assetTextUnits.get(0).getContent());
        assertEquals(" Comment 1 ", assetTextUnits.get(0).getComment());
        assertEquals("NSUsageDescription 2", assetTextUnits.get(1).getName());
        assertEquals("Add to Folder 2", assetTextUnits.get(1).getContent());
        assertNull(assetTextUnits.get(1).getComment());
        assertEquals("NSUsageDescription 3", assetTextUnits.get(2).getName());
        assertEquals("Add to Folder 3", assetTextUnits.get(2).getContent());
        assertEquals(" Comment 3 ", assetTextUnits.get(2).getComment());

    }

    @Test
    public void testMacStringsWithInvalidComments() throws Exception {

        String content = "/* Comment 1 */\n\n"
                + "// Comment 1-1\n"
                + "// Comment 1-2\n"
                + "NSUsageDescription 1 = \"Add to Folder 1\";\n\n"
                + "// Comment 2-1\n"
                + "// Comment 2-2\n"
                + "NSUsageDescription 2 = \"Add to Folder 2\";\n\n"
                + "// Comment 3\n"
                + "NSUsageDescription 3 = \"Add to Folder 3\";\n\n"
                + "// Comment 4\n"
                + "NSUsageDescription 4 = \"Add to Folder 4\";";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/en.lproj/Localizable.strings", null);

        assertEquals("Processing should have extracted 4 text units", 4, assetTextUnits.size());
        assertEquals("NSUsageDescription 1", assetTextUnits.get(0).getName());
        assertEquals("Add to Folder 1", assetTextUnits.get(0).getContent());
        assertEquals(" Comment 1-2", assetTextUnits.get(0).getComment());
        assertEquals("NSUsageDescription 2", assetTextUnits.get(1).getName());
        assertEquals("Add to Folder 2", assetTextUnits.get(1).getContent());
        assertEquals(" Comment 2-2", assetTextUnits.get(1).getComment());
        assertEquals("NSUsageDescription 3", assetTextUnits.get(2).getName());
        assertEquals("Add to Folder 3", assetTextUnits.get(2).getContent());
        assertEquals(" Comment 3", assetTextUnits.get(2).getComment());
        assertEquals("NSUsageDescription 4", assetTextUnits.get(3).getName());
        assertEquals("Add to Folder 4", assetTextUnits.get(3).getContent());
        assertEquals(" Comment 4", assetTextUnits.get(3).getComment());

    }

    @Test
    public void testMacStringsNoComment() throws Exception {

        String content = "/* No comment provided by engineer. */\n"
                + "\"Comment\" = \"Comment\";";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/en.lproj/Localizable.strings", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Comment", assetTextUnits.get(0).getName());
        assertEquals("Comment", assetTextUnits.get(0).getContent());
        assertNull(assetTextUnits.get(0).getComment());

    }

    @Test
    public void testMacStringsWithDoubleQuotesAndNewline() throws Exception {

        String content = "/* Test escaping double-quotes */\n"
                + "\"Add to \\\"%@\\\"\" = \"Add to \\\"%@\\\"\";\n"
                + "/* Test newline */\n"
                + "\"thisline \\n nextline\" = \"thisline \\n nextline\";\n";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/en.lproj/Localizable.strings", null);

        assertEquals("Processing should have extracted 2 text units", 2, assetTextUnits.size());
        assertEquals("Add to \\\"%@\\\"", assetTextUnits.get(0).getName());
        assertEquals("Add to \"%@\"", assetTextUnits.get(0).getContent());
        assertEquals(" Test escaping double-quotes ", assetTextUnits.get(0).getComment());
        assertEquals("thisline \\n nextline", assetTextUnits.get(1).getName());
        assertEquals("thisline \n nextline", assetTextUnits.get(1).getContent());
        assertEquals(" Test newline ", assetTextUnits.get(1).getComment());
    }

    @Test
    public void testAndroidStringsWithNotTranslatable() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string name=\"Test\">You must test your changes</string>\n"
                + "  <string name=\"Test_translatable\" translatable=\"true\">This is translatable</string>\n"
                + "  <string name=\"Test_not_translatable\" translatable=\"false\">This is not translatable</string>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);

        assertEquals("Processing should have extracted 2 text units", 2, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("You must test your changes", assetTextUnits.get(0).getContent());
        assertEquals("Test_translatable", assetTextUnits.get(1).getName());
        assertEquals("This is translatable", assetTextUnits.get(1).getContent());

    }

    @Test
    public void testAndroidStringsWithEscapedHTMLTags() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string name=\"Test\">Hello, %1$s! You have &lt;b>%2$d new messages&lt;/b>.</string>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("Hello, %1$s! You have <b>%2$d new messages</b>.", assetTextUnits.get(0).getContent());

    }

    @Test
    public void testAndroidStringsWithCDATA() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string name=\"Test\">Hello, %1$s! You have <![CDATA[<b>%2$d new messages</b>]]>.</string>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("Hello, %1$s! You have <b>%2$d new messages</b>.", assetTextUnits.get(0).getContent());

    }

    @Test
    public void testAndroidStringsWithDescriptionInXMLComments() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <!-- comment for hello -->\n"
                + "  <string name=\"hello\">Hello</string>\n"
                + "  <!-- this shouldn't override -->\n"
                + "  <string name=\"hello2\" description=\"comment for hello2\">Hello2</string>\n"
                + "  <!-- line 1 -->\n"
                + "  <!-- line 2 -->\n"
                + "  <!-- line 3 -->\n"
                + "  <string name=\"hello3\">Hello3</string>\n"
                + "</resources>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/strings.xml", null);

        assertEquals("Processing should have extracted 3 text units", 3, assetTextUnits.size());
        assertEquals("hello", assetTextUnits.get(0).getName());
        assertEquals("Hello", assetTextUnits.get(0).getContent());
        assertEquals("comment for hello", assetTextUnits.get(0).getComment());

        assertEquals("hello2", assetTextUnits.get(1).getName());
        assertEquals("Hello2", assetTextUnits.get(1).getContent());
        assertEquals("comment for hello2", assetTextUnits.get(1).getComment());

        assertEquals("hello3", assetTextUnits.get(2).getName());
        assertEquals("Hello3", assetTextUnits.get(2).getContent());
        assertEquals("line 1 line 2 line 3", assetTextUnits.get(2).getComment());

    }

    @Test
    public void testXliffNoResname() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"1.2\" xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 http://docs.oasis-open.org/xliff/v1.2/os/xliff-core-1.2-strict.xsd\">\n"
                + "  <file original=\"Localizable.strings\" source-language=\"en\" datatype=\"plaintext\">\n"
                + "    <header>\n"
                + "      <tool tool-id=\"com.apple.dt.xcode\" tool-name=\"Xcode\" tool-version=\"7.2\" build-num=\"7C68\"/>\n"
                + "    </header>\n"
                + "    <body>\n"
                + "      <trans-unit id=\"Test\">\n"
                + "        <source>This is a test</source>\n"
                + "        <note>This is note for Test</note>"
                + "      </trans-unit>\n"
                + "    </body>\n"
                + "  </file>"
                + "</xliff>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/en.xliff", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("This is a test", assetTextUnits.get(0).getContent());
        assertEquals("This is note for Test", assetTextUnits.get(0).getComment());

    }

    @Test
    public void testXliffDoNotTranslate() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"1.2\" xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 http://docs.oasis-open.org/xliff/v1.2/os/xliff-core-1.2-strict.xsd\">\n"
                + "  <file original=\"Localizable.strings\" source-language=\"en\" datatype=\"plaintext\">\n"
                + "    <header>\n"
                + "      <tool tool-id=\"com.apple.dt.xcode\" tool-name=\"Xcode\" tool-version=\"7.2\" build-num=\"7C68\"/>\n"
                + "    </header>\n"
                + "    <body>\n"
                + "      <trans-unit id=\"Test\">\n"
                + "        <source>This is a test</source>\n"
                + "        <note>This is note for Test</note>"
                + "      </trans-unit>\n"
                + "      <trans-unit id=\"Test not translatable\">\n"
                + "        <source>This is not translatable</source>\n"
                + "        <note>DO NOT TRANSLATE</note>"
                + "      </trans-unit>\n"
                + "    </body>\n"
                + "  </file>"
                + "</xliff>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/res/en.xliff", null);

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("This is a test", assetTextUnits.get(0).getContent());
        assertEquals("This is note for Test", assetTextUnits.get(0).getComment());

    }

    @Test
    public void testXtb() throws Exception {

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE translationbundle>\n"
                + "<translationbundle lang=\"en-GB\">\n"
                + "	<translation id=\"0\" key=\"MSG_DIALOG_OK_\" source=\"lib/closure-library/closure/goog/ui/dialog.js\" desc=\"Standard caption for the dialog 'OK' button.\">OK</translation>\n"
                + "     <translation id=\"1\" key=\"MSG_VIEWER_MENU\" source=\"src/js/box/dicom/viewer/toolbar.js\" desc=\"Tooltip text for the &quot;More&quot; menu.\">More</translation>\n"
                + "     <translation id=\"2\" key=\"MSG_GONSTEAD_STEP\" source=\"src/js/box/dicom/viewer/gonsteaddialog.js\" desc=\"Instructions for the Gonstead method.\">Select the &lt;strong&gt;left Iliac crest&lt;/strong&gt;</translation>\n"
                + "</translationbundle>";
        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/fake/xtb/messages-en-US.xtb", null);

        assertEquals("Processing should have extracted 3 text units", 3, assetTextUnits.size());
        assertEquals("MSG_DIALOG_OK_", assetTextUnits.get(0).getName());
        assertEquals("OK", assetTextUnits.get(0).getContent());
        assertEquals("Standard caption for the dialog 'OK' button.", assetTextUnits.get(0).getComment());
        assertEquals("MSG_VIEWER_MENU", assetTextUnits.get(1).getName());
        assertEquals("More", assetTextUnits.get(1).getContent());
        assertEquals("Tooltip text for the \"More\" menu.", assetTextUnits.get(1).getComment());
        assertEquals("MSG_GONSTEAD_STEP", assetTextUnits.get(2).getName());
        assertEquals("Select the <strong>left Iliac crest</strong>", assetTextUnits.get(2).getContent());
        assertEquals("Instructions for the Gonstead method.", assetTextUnits.get(2).getComment());
    }

    @Test
    public void testTextUnitsWithUsagesForPlural() throws Exception {
        String content = "#: path/to/file.js:25\n"
                + "msgid < \"person\"\n"
                + "msgid_plural < \"people\"\n"
                + "msgstr[0] < \"person\"\n"
                + "msgstr[1] < \"people\"\n";

        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/file.pot", null);

        Set<String> expectedUsages = new HashSet<>();
        expectedUsages.add("path/to/file.js:25");
        checkAssetTextUnits(assetTextUnits, expectedUsages);
    }

    @Test
    public void testTextUnitsWithMultipleUsagesForPlural() throws Exception {
        String content = "#: path/to/file.js:25\n"
                + "#: path/to/file.js:30\n"
                + "msgid < \"person\"\n"
                + "msgid_plural < \"people\"\n"
                + "msgstr[0] < \"person\"\n"
                + "msgstr[1] < \"people\"\n";

        List<AssetTextUnit> assetTextUnits = getAssetTextUnits(content, "path/to/file.pot", null);

        Set<String> expectedUsages = new HashSet<>();
        expectedUsages.add("path/to/file.js:25");
        expectedUsages.add("path/to/file.js:30");
        checkAssetTextUnits(assetTextUnits, expectedUsages);
    }

    @Test
    public void testBranches() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String assetPath = "path/to/file.properties";
        String masterContent = "# string1 description\n"
                + "string1=content1\n"
                + "string2=content2\n";

        Asset asset = assetService.createAsset(repository.getId(), assetPath, false);

        Branch master = branchService.createBranch(asset.getRepository(), "master", null);
        Branch branch1 = branchService.createBranch(asset.getRepository(), "branch1", null);
        Branch branch2 = branchService.createBranch(asset.getRepository(), "branch2", null);

        AssetContent assetContent = assetContentService.createAssetContent(asset, masterContent, master);
        assetExtractionService.processAssetAsync(null, assetContent.getId(), null, null).get();

        List<AssetTextUnit> masterAssetTextUnits = getAssetTextUnitsWithUsages(assetRepository.findOne(asset.getId()));

        logger.info("Number of text units: {}", masterAssetTextUnits.size());
        assertEquals(2L, masterAssetTextUnits.size());
        assertEquals("string1", masterAssetTextUnits.get(0).getName());
        assertEquals("string2", masterAssetTextUnits.get(1).getName());

        String branch1Content = "# string1 description\n"
                + "string1=content1\n";

        AssetContent branch1AssetContent = assetContentService.createAssetContent(asset, branch1Content, branch1);
        assetExtractionService.processAssetAsync(null, branch1AssetContent.getId(), null, null).get();

        List<AssetTextUnit> branch1AssetTextUnits = getAssetTextUnitsWithUsages(assetRepository.findOne(asset.getId()));

        logger.info("Number of text units: {}", branch1AssetTextUnits.size());
        assertEquals(2L, branch1AssetTextUnits.size());
        assertEquals("string1", branch1AssetTextUnits.get(0).getName());
        assertEquals("string2", branch1AssetTextUnits.get(1).getName());


        String branch2Content = "# string3 description\n"
                + "string3=content3\n";

        AssetContent branch2AssetContent = assetContentService.createAssetContent(asset, branch2Content, branch2);
        assetExtractionService.processAssetAsync(null, branch2AssetContent.getId(), null, null).get();

        List<AssetTextUnit> branch2AssetTextUnits = getAssetTextUnitsWithUsages(assetRepository.findOne(asset.getId()));

        logger.info("Number of text units: {}", branch2AssetTextUnits.size());
        assertEquals(3L, branch2AssetTextUnits.size());
        assertEquals("string1", branch2AssetTextUnits.get(0).getName());
        assertEquals("string2", branch2AssetTextUnits.get(1).getName());
        assertEquals("string3", branch2AssetTextUnits.get(2).getName());

        assetExtractionService.deleteAssetBranch(assetRepository.findOne(asset.getId()), branch2.getName());
        List<AssetTextUnit> deletebranch2AssetTextUnits = getAssetTextUnitsWithUsages(assetRepository.findOne(asset.getId()));
        logger.info("Number of text units: {}", deletebranch2AssetTextUnits.size());
        assertEquals(2L, deletebranch2AssetTextUnits.size());
        assertEquals("string1", deletebranch2AssetTextUnits.get(0).getName());
        assertEquals("string2", deletebranch2AssetTextUnits.get(1).getName());

    }

    /**
     * Checks the assetTextUnits have the correct usages
     * @param assetTextUnits    list of AssetTextUnits
     * @param expectedUsages    list of Strings containing expected usages
     */
    private void checkAssetTextUnits(List<AssetTextUnit> assetTextUnits, Set<String> expectedUsages) {
        for (AssetTextUnit assetTextUnit : assetTextUnits) {
            assertEquals(expectedUsages, assetTextUnit.getUsages());
        }
    }
}
