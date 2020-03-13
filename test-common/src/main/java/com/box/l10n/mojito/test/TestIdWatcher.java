package com.box.l10n.mojito.test;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.UUID;

public class TestIdWatcher extends TestWatcher {

    /**
     * Test id used as prefix to process independent entities/directories in
     * each test
     */
    protected String testId;
    protected UUID uuid;
    protected String slashClassName;
    protected String methodName;

    public String getTestId() {
        return this.testId;
    }

    @Override
    protected void starting(Description description) {
        this.slashClassName = description.getClassName().replaceAll("\\.", "/");
        this.methodName = description.getMethodName();
        testId = slashClassName + "/" + description.getMethodName();
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

    public String getSlashClassName() {
        return slashClassName;
    }

    public String getMethodName() {
        return methodName;
    }
}
