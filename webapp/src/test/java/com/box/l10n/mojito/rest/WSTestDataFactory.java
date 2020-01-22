package com.box.l10n.mojito.rest;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.factory.XliffDataFactory;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.asset.AssetUpdateException;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskException;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import net.sf.okapi.common.resource.TextUnit;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author aloison
 */
@Component
public class WSTestDataFactory {

    /**
     * logger
     */
    static Logger logger = getLogger(WSTestDataFactory.class);

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    LocaleService localeService;

    @Autowired
    AssetService assetService;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    TMService tmService;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    XliffDataFactory xliffDataFactory;

    /**
     * Creates a repo supporting few languages and a TM
     *
     * @param testIdWatcher
     * @return The {@link Repository}
     */
    @Transactional
    public Repository createRepository(TestIdWatcher testIdWatcher) throws RepositoryNameAlreadyUsedException {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        try {
            repositoryService.addRepositoryLocale(repository, "fr-FR");
            repositoryService.addRepositoryLocale(repository, "ja-JP");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        return repository;
    }

    /**
     * Creates a repo supporting few languages, a TM
     * and processes an asset to add text units
     *
     * @param testIdWatcher
     * @return The {@link Repository}
     */
    public Repository createRepoAndAssetAndTextUnits(TestIdWatcher testIdWatcher) throws RepositoryNameAlreadyUsedException, AssetUpdateException, UnsupportedAssetFilterTypeException {
        Repository repository = createRepository(testIdWatcher);

        try {
            PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repository.getId(), getTestSourceAssetContent(), "path/to/asset.xliff", null, null, null, null);
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException | ExecutionException e) {
            throw new RuntimeException("Could not update asset and process it", e);
        }

        return repository;
    }

    /**
     * @return The content of the test asset
     */
    public String getTestSourceAssetContent() {
        List<TextUnit> textUnits = new ArrayList<>();
        textUnits.add(xliffDataFactory.createTextUnit(1L, "2_factor_challenge_buttom", "Submit", null));
        textUnits.add(xliffDataFactory.createTextUnit(2L, "2fa_confirmation_code", "Confirmation code", null));
        textUnits.add(xliffDataFactory.createTextUnit(3L, "Account_security_and_password_settings", "Account security and password settings", null));

        return xliffDataFactory.generateSourceXliff(textUnits);
    }

    /**
     * Creates a repo supporting few languages, a TM,
     * processes an asset to add text units
     * and add some variants.
     *
     * @param testIdWatcher
     * @return The {@link Repository}
     */
    public Repository createRepoAndAssetAndTextUnitsAndVariants(TestIdWatcher testIdWatcher) throws Exception {
        Repository repository = createRepoAndAssetAndTextUnits(testIdWatcher);

        List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(repository.getTm().getId());

        // add variants only to fr-FR
        Locale frFrLocale = localeService.findByBcp47Tag("fr-FR");
        for (TMTextUnit tmTextUnit : tmTextUnits) {
            Long tmTextUnitId = tmTextUnit.getId();
            tmService.addCurrentTMTextUnitVariant(tmTextUnitId, frFrLocale.getId(), "Variant for tmTextUnit " + tmTextUnitId + " - " + frFrLocale.getBcp47Tag());
        }

        return repository;
    }
}
