package com.box.l10n.mojito.test;

import java.util.UUID;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class TestIdWatcher extends TestWatcher {

    /**
     * Test id used as prefix to process independent entities/directories in
     * each test
     */
    protected String testId;
    protected UUID uuid;

    public String getTestId() {
        return this.testId;
    }

    @Override
    protected void starting(Description description) {
        String clazz = description.getClassName().replaceAll("\\.", "/");
        testId = clazz + "/" + description.getMethodName();
        uuid = UUID.randomUUID();
    }

    /**
     * Gets test entity name.
     *
     * @param baseName
     * @return
     */
    public String getEntityName(String baseName) {
        return getTestId() + "/" + baseName + '/' + uuid;
    }
}
