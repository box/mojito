package com.box.l10n.mojito.boxsdk;

import com.box.l10n.mojito.service.boxsdk.BoxSDKServiceConfigEntityRepository;
import com.box.l10n.mojito.service.boxsdk.BoxSDKServiceConfigEntityService;
import com.box.l10n.mojito.test.category.BoxSDKTest;
import com.box.sdk.BoxComment;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxSharedLink;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author jaurambault
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@ComponentScan(basePackageClasses = {BoxSDKServiceTest.class, BoxSDKServiceConfigEntityService.class})
@SpringBootTest(classes = {BoxSDKServiceTest.class, PropertyPlaceholderAutoConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableConfigurationProperties
public class BoxSDKServiceTest {

    /**
     * logger
     */
    static Logger logger = getLogger(BoxSDKServiceTest.class);

    @TestConfiguration
    static class TestConfig {

        @Bean
        public BoxSDKServiceConfigEntityRepository getBoxSDKServiceConfigEntityRepository() {
            return null;
        }
    }

    @Autowired
    BoxSDKServiceConfigFromProperties boxSDKServiceConfigFromProperties;

    @Autowired
    BoxSDKService boxSDKService;

    static BoxFolder testFolder = null;

    @Before
    public void prepareTestFolderOnBox() throws Exception {

        assumeTrue(StringUtils.isNotEmpty(boxSDKServiceConfigFromProperties.getRootFolderId()));

        // since @Before runs multiple times...
        if (testFolder != null) {
            return;
        }

        String currentClassName = this.getClass().getSimpleName();
        BoxFolder folder = boxSDKService.getFolderWithName(currentClassName);

        if (folder != null) {
            boxSDKService.deleteFolderAndItsContent(folder.getID());
        }

        testFolder = boxSDKService.createFolderUnderRoot(currentClassName);
    }

    @Test
    @Category(BoxSDKTest.class)
    public void testGetRootFolder() throws Exception {
        BoxFolder rootFolder = boxSDKService.getRootFolder();
        assertEquals(boxSDKServiceConfigFromProperties.getRootFolderId(), rootFolder.getID());
    }

    @Test
    @Category(BoxSDKTest.class)
    public void testListFiles() throws Exception {

        BoxFolder createdFolder = boxSDKService.createFolder("testListFiles-" + new Date().getTime(), testFolder.getID());
        boxSDKService.uploadFile(createdFolder.getID(), "file1.txt", "content of file1.txt");
        boxSDKService.uploadFile(createdFolder.getID(), "file2.txt", "content of file2.txt");

        List<BoxFile> listFiles = boxSDKService.listFiles(createdFolder.getID());

        assertEquals("file1.txt", listFiles.get(0).getInfo().getName());
        assertEquals("file2.txt", listFiles.get(1).getInfo().getName());

        boxSDKService.uploadFile(createdFolder.getID(), "file2.txt", "v2 content of file2.txt");

        listFiles = boxSDKService.listFiles(createdFolder.getID());

        assertEquals("file1.txt", listFiles.get(0).getInfo().getName());
        assertEquals("file2.txt", listFiles.get(1).getInfo().getName());
    }

    @Test
    @Category(BoxSDKTest.class)
    public void testUpload() throws Exception {
        BoxFolder createdFolder = boxSDKService.createFolder("testUpadteFile-" + new Date().getTime(), testFolder.getID());

        String fileContentStr = "content of file1.txt";
        BoxFile createdFile = boxSDKService.uploadFile(createdFolder.getID(), "file1.txt", fileContentStr);

        BoxFileWithContent fileContent = boxSDKService.getFileContent(createdFile);
        assertEquals(fileContentStr, fileContent.getContent());

        BoxFile updatedFile = boxSDKService.uploadFile(createdFolder.getID(), "file1.txt", fileContentStr + "updated");

        BoxFileWithContent fileContentUpdate = boxSDKService.getFileContent(updatedFile);
        assertEquals(fileContentStr + "updated", fileContentUpdate.getContent());
    }

    @Test
    @Category(BoxSDKTest.class)
    public void testGetFileContent() throws Exception {

        BoxFolder createdFolder = boxSDKService.createFolder("testGetFileContent-" + new Date().getTime(), testFolder.getID());

        String fileContentStr = "content of file1.txt";
        BoxFile createdFile = boxSDKService.uploadFile(createdFolder.getID(), "file1.txt", fileContentStr);

        BoxFileWithContent fileContent = boxSDKService.getFileContent(createdFile);

        assertEquals(fileContentStr, fileContent.getContent());
        assertEquals(createdFile, fileContent.getBoxFile());
    }

    @Test
    @Category(BoxSDKTest.class)
    public void testMultithreadedCreateFolder() throws ExecutionException, InterruptedException {

        int nbThreads = 10;

        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(nbThreads);
        List<Callable<Object>> callables = new ArrayList<>();

        for (int i = 0; i < nbThreads; i++) {
            callables.add(new Callable() {

                @Override
                public Object call() throws Exception {
                    boxSDKService.createFolder("testMultithreadedCreateFolder-" + UUID.randomUUID(), testFolder.getID());
                    return null;
                }
            });
        }

        List<Future<Object>> invokeAll = newFixedThreadPool.invokeAll(callables);

        for (Future<Object> future : invokeAll) {
            try {
                future.get();
            } catch (ExecutionException e) {
                fail("Multithreaded create folder must not fail: " + e.getMessage());
            }
        }
    }

    @Test
    @Category(BoxSDKTest.class)
    public void testGetFolderWithNameShouldFindSubFolder() throws BoxSDKServiceException {

        String expectedSubFolderName = "testGetFolderWithName-subFolder";
        String testFolderId = testFolder.getID();

        BoxFolder subFolder = boxSDKService.createFolder(expectedSubFolderName, testFolderId);

        BoxItem foundItem = boxSDKService.getFolderWithNameAndParentFolderId(expectedSubFolderName, testFolderId);

        assertEquals(subFolder.getID(), foundItem.getID());
    }

    @Test
    @Category(BoxSDKTest.class)
    public void testGetFolderWithNameShouldReturnNullIfItemNotFound() throws BoxSDKServiceException {
        String testFolderId = testFolder.getID();

        BoxFolder rootFolder = boxSDKService.createFolder("testGetFolderWithNameShouldReturnNullIfItemNotFound-" + new Date().getTime(), testFolderId);

        BoxItem foundItem = boxSDKService.getFolderWithNameAndParentFolderId("file-that-does-not-exist", testFolderId);

        assertNull(foundItem);
    }

    @Test
    @Category(BoxSDKTest.class)
    public void testAddCommentToFile() throws BoxSDKServiceException {
        String testFolderId = testFolder.getID();

        BoxFolder rootFolder = boxSDKService.createFolder("testAddCommentOnFile-" + new Date().getTime(), testFolderId);
        BoxFile uploadFile = boxSDKService.uploadFile(rootFolder.getID(), "fileWithComment.txt", "for test");

        boxSDKService.addCommentToFile(uploadFile.getID(), "test comment");
        boxSDKService.addCommentToFile(uploadFile.getID(), "test comment2");

        List<BoxComment.Info> comments = uploadFile.getComments();

        Assert.assertEquals(2, comments.size());
        Assert.assertEquals("test comment", comments.get(0).getMessage());
        Assert.assertEquals("test comment2", comments.get(1).getMessage());
    }

    /**
     * This is not a test per say but cleans up old data in the root folder so
     * that there won't be any performance issue after a while.
     *
     * @throws BoxSDKServiceException
     */
    @Category(BoxSDKTest.class)
    public void removeContentOlderThanADayInRootFolder() throws BoxSDKServiceException {

        BoxFolder rootFolder = boxSDKService.getRootFolder();

        DateTime dateTime = new DateTime();
        dateTime = dateTime.minusDays(1);

        boxSDKService.deleteFolderContentOlderThan(rootFolder.getID(), dateTime);
    }

    @Test
    @Category(BoxSDKTest.class)
    public void testSharedFolder() throws BoxSDKServiceException {
        String testFolderId = testFolder.getID();

        BoxFolder folder = boxSDKService.createSharedFolder("testSharedFolder-" + new Date().getTime(), testFolderId);

        BoxSharedLink sharedLink = folder.getInfo().getSharedLink();
        assertNotNull(sharedLink);
        logger.debug("SharedLink Url is: {}", sharedLink.getURL());
    }

}
