package com.box.l10n.mojito.service.assetintegritychecker;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.okapi.XliffState;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.asset.AssetUpdateException;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author wyau
 */
public class AssetIntegrityCheckerServiceTest extends ServiceTestBase {

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetService assetService;

    @Autowired
    AssetIntegrityCheckerService assetIntegrityCheckerService;

    @Autowired
    TMService tmService;

    @Autowired
    LocaleService localeService;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Autowired
    private TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    PollableTaskService pollableTaskService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();
    protected static final String ASSET_PATH = "source-asset-path.xliff";

    @Test
    public void testIntegrityCheckerIsUsedInTmServiceUpdate() throws RepositoryLocaleCreationException, ExecutionException, InterruptedException, RepositoryNameAlreadyUsedException, AssetUpdateException, UnsupportedAssetFilterTypeException {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String frFR = "fr-FR";
        repositoryService.addRepositoryLocale(repository, "fr-FR");

        assetIntegrityCheckerService.addToRepository(repository, ASSET_PATH, IntegrityCheckerType.MESSAGE_FORMAT);

        String sourceTextUnit = "{numFiles, plural, one{# There is one file} other{There are # files}}";
        String sourceXliff = xliffDataFactory.generateSourceXliff(Arrays.asList(
            xliffDataFactory.createTextUnit(1L, "tu1", sourceTextUnit, null)
        ));

        PollableFuture<Asset> assetPollableFuture =
                assetService.addOrUpdateAssetAndProcessIfNeeded(repository.getId(), "source-asset-path.xliff",
                                                                sourceXliff, false, null, null, null, null, null);
        pollableTaskService.waitForPollableTask(assetPollableFuture.getPollableTask().getId());

        Long tmId = repository.getTm().getId();
        List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(tmId);
        assertEquals(1, tmTextUnits.size());
        Long tmTextUnitId = tmTextUnits.get(0).getId();

        String targetXliff = xliffDataFactory.generateTargetXliff(Arrays.asList(
                xliffDataFactory.createTextUnit(tmTextUnitId, "tu1", sourceTextUnit, null, "{numFiles, plural, one{Il y a un fichier} other{Il y a # fichiers}", frFR, XliffState.TRANSLATED)
        ), frFR);

        Locale frFRLocale = localeService.findByBcp47Tag(frFR);
        tmService.updateTMWithXLIFFById(targetXliff, null);

        List<TMTextUnitVariant> textUnitVariants = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(frFRLocale.getId(), tmId);

        assertEquals(1, textUnitVariants.size());
        //TODO(P2) check message from message table
        assertFalse(textUnitVariants.get(0).isIncludedInLocalizedFile());
        assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, textUnitVariants.get(0).getStatus());
    }
}
    
