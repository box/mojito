package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerFactory;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import com.google.common.collect.Sets;
import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author wyau
 */
@RunWith(MockitoJUnitRunner.class)
public class TMTextUnitIntegrityCheckServiceTest {

    @InjectMocks
    TMTextUnitIntegrityCheckService integrityCheckService;

    @Mock
    IntegrityCheckerFactory integrityCheckerFactory;

    @Mock
    TMTextUnitRepository tmTextUnitRepository;

    @Test
    public void testCheckTMTextUnitIntegrityWillRunThroughIfAssetHasNoIntegrityChecker() {
        Asset asset = Mockito.mock(Asset.class);
        TMTextUnit tmTextUnit = Mockito.mock(TMTextUnit.class);

        Mockito.when(tmTextUnit.getAsset()).thenReturn(asset);
        Mockito.when(tmTextUnitRepository.findOne(Mockito.anyLong())).thenReturn(tmTextUnit);
        Mockito.when(integrityCheckerFactory.getTextUnitCheckers(asset)).thenReturn(new HashSet<TextUnitIntegrityChecker>());

        integrityCheckService.checkTMTextUnitIntegrity(1L, "string to check");

        Mockito.verify(integrityCheckerFactory, Mockito.times(1)).getTextUnitCheckers(asset);
    }

    @Test
    public void testCheckTMTextUnitIntegrityWillRunThroughAndCheck() {
        Asset asset = Mockito.mock(Asset.class);
        TMTextUnit tmTextUnit = Mockito.mock(TMTextUnit.class);
        TextUnitIntegrityChecker checker = Mockito.mock(TextUnitIntegrityChecker.class);

        Mockito.when(tmTextUnit.getAsset()).thenReturn(asset);
        Mockito.when(tmTextUnitRepository.findOne(Mockito.anyLong())).thenReturn(tmTextUnit);
        Mockito.when(integrityCheckerFactory.getTextUnitCheckers(asset)).thenReturn(Sets.newHashSet(checker));

        integrityCheckService.checkTMTextUnitIntegrity(1L, "string to check");

        Mockito.verify(checker, Mockito.times(1)).check(Mockito.anyString(), Mockito.anyString());
    }

    @Test(expected = IntegrityCheckException.class)
    public void testCheckTMTextUnitIntegrityWillThrowWhenCheckFails() {
        Asset asset = Mockito.mock(Asset.class);
        TMTextUnit tmTextUnit = Mockito.mock(TMTextUnit.class);
        TextUnitIntegrityChecker checker = Mockito.mock(TextUnitIntegrityChecker.class);

        Mockito.when(tmTextUnit.getAsset()).thenReturn(asset);
        Mockito.when(tmTextUnitRepository.findOne(Mockito.anyLong())).thenReturn(tmTextUnit);
        Mockito.when(integrityCheckerFactory.getTextUnitCheckers(asset)).thenReturn(Sets.newHashSet(checker));
        Mockito.doThrow(new IntegrityCheckException("bad")).when(checker).check(Mockito.anyString(), Mockito.anyString());

        integrityCheckService.checkTMTextUnitIntegrity(1L, "string to check");

        Mockito.verify(checker, Mockito.times(1)).check(Mockito.anyString(), Mockito.anyString());
    }
}
