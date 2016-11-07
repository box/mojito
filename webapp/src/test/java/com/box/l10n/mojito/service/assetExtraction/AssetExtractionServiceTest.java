package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.extractor.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;

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
    AssetExtractionService assetExtractionService;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetRepository assetRepository;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testProcessAssetShouldProcessCsvFiles() throws Exception {

        String csvContent = "e7341f2db0cfe6a630057bff0eed1394,\"Application description:\",,,Application_description_with_colon,d41d8cd98f00b204e9800998ecf8427e,\"Description of application\"\n"
                + "ce306a623fba4c0873a3325d5cce3333,\"Box View API key successfully created!\",0,\"Clé Box View API créée !\",Box_view_api_key_successfully_created_,471e627327b00962376364c4af54c889,\n"
                + "d2fda97e68866012bafc01dbdbe7ddc7,\"Content API Access Only:\",0,\"Accès à Content API uniquement :\",Content_API_Access_Only,7df9b9d8a801c1365393a522e93cba18,\"this is saying the application can only be used with the content API\"";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset csvAsset = assetService.createAsset(repository.getId(), csvContent, "path/to/fake/file.csv");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(csvAsset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 3 text units", 3, assetTextUnits.size());

        AssetTextUnit assetTextUnit = assetTextUnits.get(2);
        assertEquals("Content_API_Access_Only", assetTextUnit.getName());
        assertEquals("Content API Access Only:", assetTextUnit.getContent());
        assertEquals("this is saying the application can only be used with the content API", assetTextUnit.getComment());
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

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset xliffAsset = assetService.createAsset(repository.getId(), xliffContent, "path/to/fake/file.xliff");
        PollableFuture<Asset> processResult = assetExtractionService.processAsset(xliffAsset.getId(), null, PollableTask.INJECT_CURRENT_TASK);

        Asset processedAsset = processResult.get();
        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

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

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset xliffAsset = assetService.createAsset(repository.getId(), xliffContent, "path/to/fake/file.xliff");
        PollableFuture<Asset> processResult = assetExtractionService.processAsset(xliffAsset.getId(), null, PollableTask.INJECT_CURRENT_TASK);

        Asset processedAsset = processResult.get();
        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

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

        Asset asset = assetService.createAsset(repository.getId(), "fake-content", "path/to/fake/file-with-unsupported.ext");
        PollableFuture<Asset> pollableTaskResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);

        // Wait for the processing to finish
        try {
            pollableTaskResult.get();
            fail("An exception should have been thrown");
        } catch (ExecutionException e) {
            Throwable originalException = e.getCause();
            assertTrue(originalException instanceof UnsupportedAssetFilterTypeException);

            logger.debug("\n===============\nThe exception thrown above is expected. Do not panic!\n===============\n");
        }
    }

    @Test
    public void testResx() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<root>\n"
                + "  <data name=\"Test\" xml:space=\"preserve\">\n"
                + "    <value>You must test your changes</value>\n"
                + "	<comment>Test label</comment>\n"
                + "  </data>\n"
                + "</root>";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/Test.resx");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("You must test your changes", assetTextUnits.get(0).getContent());
        assertEquals("Test label", assetTextUnits.get(0).getComment());

    }
    
    @Test
    public void testResw() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<root>\n"
                + "  <data name=\"Test\" xml:space=\"preserve\">\n"
                + "    <value>You must test your changes</value>\n"
                + "	<comment>Test label</comment>\n"
                + "  </data>\n"
                + "</root>";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/Test.resw");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("You must test your changes", assetTextUnits.get(0).getContent());
        assertEquals("Test label", assetTextUnits.get(0).getComment());

    }
    
    @Test
    public void testAndroidStrings() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string name=\"Test\" description=\"Test label\">You must test your changes</string>\n"
                + "</resources>";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/res/strings.xml");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("You must test your changes", assetTextUnits.get(0).getContent());
        assertEquals("Test label", assetTextUnits.get(0).getComment());

    }
    
    @Test
    public void testAndroidStringsWithSpecialCharacters() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string name=\"test\">Make sure you\\\'d \\\"escaped\\\" <b>special</b> characters like quotes &amp; ampersands.\\n</string>\n"
                + "</resources>";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/res/strings.xml");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("test", assetTextUnits.get(0).getName());
        assertEquals("Make sure you'd \"escaped\" <b>special</b> characters like quotes & ampersands.\n", assetTextUnits.get(0).getContent());

    }
    
    @Test
    public void testAndroidStringsArray() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string-array name=\"$x_Collaborators\" description=\"Reference to number of collaborators in folders\">\n"
                + "    <item>No people</item>\n"
                + "    <item>1 person</item>\n"
                + "    <item>%1$d people</item>\n"
                + "  </string-array>\n"
                + "</resources>";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/res/strings.xml");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

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

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string-array name=\"N_items_failed_to_move\">\n"
                + "        <item />\n"
                + "        <item>1 item failed to move</item>\n"
                + "        <item>%1$d items failed to move</item>\n"
                + "    </string-array>\n"
                + "</resources>";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/res/strings.xml");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 2 text units", 2, assetTextUnits.size());
        
        assertEquals("N_items_failed_to_move_1", assetTextUnits.get(0).getName());
        assertEquals("1 item failed to move", assetTextUnits.get(0).getContent());

        assertEquals("N_items_failed_to_move_2", assetTextUnits.get(1).getName());
        assertEquals("%1$d items failed to move", assetTextUnits.get(1).getContent());
    }
    
    @Test
    public void testAndroidStringsPlural() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <plurals name=\"numberOfCollaborators\" description=\"Example of plurals\">\n"
                + "    <item quantity=\"zero\">No people</item>\n"
                + "    <item quantity=\"one\">1 person</item>\n"
                + "    <item quantity=\"few\">few people</item>\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "</resources>";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/res/strings.xml");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 4 text units", 4, assetTextUnits.size());
        
        assertEquals("numberOfCollaborators_zero", assetTextUnits.get(0).getName());
        assertEquals("No people", assetTextUnits.get(0).getContent());
        assertEquals("Example of plurals", assetTextUnits.get(0).getComment());

        assertEquals("numberOfCollaborators_one", assetTextUnits.get(1).getName());
        assertEquals("1 person", assetTextUnits.get(1).getContent());
        assertEquals("Example of plurals", assetTextUnits.get(1).getComment());
        
        assertEquals("numberOfCollaborators_few", assetTextUnits.get(2).getName());
        assertEquals("few people", assetTextUnits.get(2).getContent());
        assertEquals("Example of plurals", assetTextUnits.get(2).getComment());
        
        assertEquals("numberOfCollaborators_other", assetTextUnits.get(3).getName());
        assertEquals("%1$d people", assetTextUnits.get(3).getContent());
        assertEquals("Example of plurals", assetTextUnits.get(3).getComment());
    }
    
    @Test
    public void testMacStrings() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "/* Title: Title for the add content to folder menu header */\n"
                + "\"Add to Folder\" = \"Add to Folder\";";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/en.lproj/Localizable.strings");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Add to Folder", assetTextUnits.get(0).getName());
        assertEquals("Add to Folder", assetTextUnits.get(0).getContent());
        assertEquals(" Title: Title for the add content to folder menu header ", assetTextUnits.get(0).getComment());

    }
    
    @Test
    public void testMacStringsNoComment() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "/* No comment provided by engineer. */\n"
                + "\"Comment\" = \"Comment\";";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/en.lproj/Localizable.strings");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Comment", assetTextUnits.get(0).getName());
        assertEquals("Comment", assetTextUnits.get(0).getContent());
        assertNull(assetTextUnits.get(0).getComment());

    }

    @Test
    public void testMacStringsWithDoubleQuotesAndNewline() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "/* Test escaping double-quotes */\n"
                + "\"Add to \\\"%@\\\"\" = \"Add to \\\"%@\\\"\";\n"
                + "/* Test newline */\n"
                + "\"thisline \\n nextline\" = \"thisline \\n nextline\";\n";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/en.lproj/Localizable.strings");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

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

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string name=\"Test\">You must test your changes</string>\n"
                + "  <string name=\"Test_translatable\" translatable=\"true\">This is translatable</string>\n"
                + "  <string name=\"Test_not_translatable\" translatable=\"false\">This is not translatable</string>\n"
                + "</resources>";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/res/strings.xml");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 2 text units", 2, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("You must test your changes", assetTextUnits.get(0).getContent());
        assertEquals("Test_translatable", assetTextUnits.get(1).getName());
        assertEquals("This is translatable", assetTextUnits.get(1).getContent());

    }

    @Test
    public void testAndroidStringsWithEscapedHTMLTags() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string name=\"Test\">Hello, %1$s! You have &lt;b>%2$d new messages&lt;/b>.</string>\n"
                + "</resources>";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/res/strings.xml");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("Hello, %1$s! You have <b>%2$d new messages</b>.", assetTextUnits.get(0).getContent());

    }

    @Test
    public void testAndroidStringsWithCDATA() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <string name=\"Test\">Hello, %1$s! You have <![CDATA[<b>%2$d new messages</b>]]>.</string>\n"
                + "</resources>";
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/res/strings.xml");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("Hello, %1$s! You have <b>%2$d new messages</b>.", assetTextUnits.get(0).getContent());

    }

    @Test
    public void testXliffNoResname() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

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
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/res/en.xliff");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("This is a test", assetTextUnits.get(0).getContent());
        assertEquals("This is note for Test", assetTextUnits.get(0).getComment());

    }

    @Test
    public void testXliffDoNotTranslate() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

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
        Asset asset = assetService.createAsset(repository.getId(), content, "path/to/fake/res/en.xliff");

        PollableFuture<Asset> processResult = assetExtractionService.processAsset(asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        Asset processedAsset = processResult.get();

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(processedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("Processing should have extracted 1 text units", 1, assetTextUnits.size());
        assertEquals("Test", assetTextUnits.get(0).getName());
        assertEquals("This is a test", assetTextUnits.get(0).getContent());
        assertEquals("This is note for Test", assetTextUnits.get(0).getComment());

    }
}
