package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorage;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PollableTaskBlobStorageTest extends ServiceTestBase {

    @Autowired
    PollableTaskBlobStorage pollableTaskBlobStorage;

    @Autowired
    PollableTaskService pollableTaskService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired
    BlobStorage blobStorage;

    @Before
    public void before() {
        // to be sure ids in the db and in the storage are in sync (if using s3, data might be there from previous run)
        Assume.assumeTrue(blobStorage instanceof DatabaseBlobStorage);
    }

    @Test
    public void inputNull() {
        PollableTask createPollableTask = pollableTaskService.createPollableTask(null, testIdWatcher.getEntityName("inputNull"), null, 0);
        pollableTaskBlobStorage.saveInput(createPollableTask.getId(), null);
        String input = pollableTaskBlobStorage.getInput(createPollableTask.getId(), String.class);
        assertNull(input);
    }

    @Test
    public void inputNotNull() {
        PollableTask createPollableTask = pollableTaskService.createPollableTask(null, testIdWatcher.getEntityName("inputNotNull"), null, 0);
        TestData testData = new TestData();
        testData.setId(10L);
        testData.setName("somename");
        pollableTaskBlobStorage.saveInput(createPollableTask.getId(), testData);
        TestData input = pollableTaskBlobStorage.getInput(createPollableTask.getId(), TestData.class);
        assertEquals(testData, input);
    }

    @Test
    public void missingInput() {
        // that'd fail if the storage has data for that id ... like if db is not cleaned up
        long pollableId = 999999999999999999L;
        try {
            TestData input = pollableTaskBlobStorage.getInput(pollableId, TestData.class);
            fail();
        } catch (RuntimeException e) {
           assertEquals("Can't get the input json for: " + pollableId, e.getMessage());
        }
    }

    static class TestData {

        Long id;
        String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestData testData = (TestData) o;
            return Objects.equals(id, testData.id) &&
                    Objects.equals(name, testData.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

}