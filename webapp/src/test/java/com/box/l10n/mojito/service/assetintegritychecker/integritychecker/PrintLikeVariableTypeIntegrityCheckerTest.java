package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class PrintLikeVariableTypeIntegrityCheckerTest {

    @Test
    public void testCheckPassesIfTypeVariablesMatch() throws PrintfLikeIntegrityCheckerException {
        PrintfLikeVariableTypeIntegrityChecker checker = new PrintfLikeVariableTypeIntegrityChecker();
        String source = "%(count)s view";
        String target = "%(count)s view";

        checker.check(source, target);
    }

    @Test
    public void testMissingVariableTypeCausesIntegrityViolation() throws PrintfLikeVariableTypeIntegrityCheckerException {
        PrintfLikeVariableTypeIntegrityChecker checker = new PrintfLikeVariableTypeIntegrityChecker();
        String source = "%(count)s view";
        String target = "%(count) view";

        try {
            checker.check(source, target);
            fail("PrintfLikeVariableTypeIntegrityCheckerException should have been thrown.");
        } catch (PrintfLikeVariableTypeIntegrityCheckerException e) {
            assertEquals("Variable types do not match.", e.getMessage());
        }
    }

    @Test
    public void testModifiedVariableTypeCausesIntegrityViolation() throws PrintfLikeIntegrityCheckerException {
        PrintfLikeVariableTypeIntegrityChecker checker = new PrintfLikeVariableTypeIntegrityChecker();
        String source = "%(count)s view";
        String target = "%(count)d view";

        try {
            checker.check(source, target);
            fail("PrintfLikeVariableTypeIntegrityCheckerException should have been thrown.");
        } catch (PrintfLikeVariableTypeIntegrityCheckerException e) {
            assertEquals("Variable types do not match.", e.getMessage());
        }
    }

    @Test
    public void testVariableWithFormattingFlagChecked() {
        PrintfLikeVariableTypeIntegrityChecker checker = new PrintfLikeVariableTypeIntegrityChecker();
        String source = "%(count)#s view";
        String target = "%(count)#d view";

        try {
            checker.check(source, target);
            fail("PrintfLikeVariableTypeIntegrityCheckerException should have been thrown.");
        } catch (PrintfLikeVariableTypeIntegrityCheckerException e) {
            assertEquals("Variable types do not match.", e.getMessage());
        }

        source = "%(count) s view";
        target = "%(count) d view";

        try {
            checker.check(source, target);
            fail("PrintfLikeVariableTypeIntegrityCheckerException should have been thrown.");
        } catch (PrintfLikeVariableTypeIntegrityCheckerException e) {
            assertEquals("Variable types do not match.", e.getMessage());
        }

        source = "%(count).1f view";
        target = "%(count).2f view";

        try {
            checker.check(source, target);
            fail("PrintfLikeVariableTypeIntegrityCheckerException should have been thrown.");
        } catch (PrintfLikeVariableTypeIntegrityCheckerException e) {
            assertEquals("Variable types do not match.", e.getMessage());
        }
    }

    @Test
    public void testCurlyBracketsAreChecked() {
        PrintfLikeVariableTypeIntegrityChecker checker = new PrintfLikeVariableTypeIntegrityChecker();
        String source = "%{count} s view";
        String target = "%{count} d view";

        try {
            checker.check(source, target);
            fail("PrintfLikeVariableTypeIntegrityCheckerException should have been thrown.");
        } catch (PrintfLikeVariableTypeIntegrityCheckerException e) {
            assertEquals("Variable types do not match.", e.getMessage());
        }
    }

    @Test
    public void testCheckPassesIfNoBracketsPresent() {
        PrintfLikeVariableTypeIntegrityChecker checker = new PrintfLikeVariableTypeIntegrityChecker();
        String source = "%s test";
        String target = "%s test";

        checker.check(source, target);
    }
}
