package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.rest.asset.LocalizedAssetBody;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class GenerateLocalizedAssetJob extends QuartzPollableJob<LocalizedAssetBody, LocalizedAssetBody> {

    static Logger logger = LoggerFactory.getLogger(GenerateLocalizedAssetJob.class);

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    RepositoryLocaleRepository repositoryLocaleRepository;

    @Autowired
    TMService tmService;

    @Autowired
    AssetService assetService;

    @Autowired
    TMXliffRepository tmXliffRepository;

    @Autowired
    LocaleService localeService;

    @Override
    public LocalizedAssetBody call(LocalizedAssetBody localizedAssetBody) throws Exception {
        Long assetId = localizedAssetBody.getAssetId();
        logger.debug("Localizing content payload with asset id = {}, and locale = {}", assetId);

        Asset asset = assetRepository.findById(assetId).orElse(null);
        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryIdAndLocaleId(asset.getRepository().getId(), localizedAssetBody.getLocaleId());

        String normalizedContent = NormalizationUtils.normalize(localizedAssetBody.getContent());

        String generateLocalized = tmService.generateLocalized(
                asset,
                normalizedContent,
                repositoryLocale,
                localizedAssetBody.getOutputBcp47tag(),
                localizedAssetBody.getFilterConfigIdOverride(),
                localizedAssetBody.getFilterOptions(),
                localizedAssetBody.getStatus(),
                localizedAssetBody.getInheritanceMode(),
                localizedAssetBody.getPullRunName());

        localizedAssetBody.setContent(generateLocalized);

        if (localizedAssetBody.getOutputBcp47tag() != null) {
            localizedAssetBody.setBcp47Tag(localizedAssetBody.getOutputBcp47tag());
        } else {
            localizedAssetBody.setBcp47Tag(repositoryLocale.getLocale().getBcp47Tag());
        }

        return localizedAssetBody;
    }
}
