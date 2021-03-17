package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class EmptyTargetNotEmptySourceIntegrityCheckerTest {

    EmptyTargetNotEmptySourceIntegrityChecker emptyTargetNotEmptySourceIntegrityChecker = new EmptyTargetNotEmptySourceIntegrityChecker();

    @Test
    public void checkEmptyTargetNotEmptySource() {
        Assertions.assertThatThrownBy(() -> emptyTargetNotEmptySourceIntegrityChecker.check("source", ""))
                .isInstanceOf(EmptyTargetNotEmptySourceIntegrityCheckerException.class)
                .hasMessage("Empty target is rejected when the source is not empty");
    }

    @Test(expected = Test.None.class)
    public void checkEmptyTargetEmptySource() {
        emptyTargetNotEmptySourceIntegrityChecker.check("", "");
    }

    @Test(expected = Test.None.class)
    public void checkNotEmpty() {
        emptyTargetNotEmptySourceIntegrityChecker.check("source", "target");
    }
}