package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import static com.box.l10n.mojito.rest.asset.AssetSpecification.deletedEquals;
import static com.box.l10n.mojito.rest.asset.AssetSpecification.pathEquals;
import static com.box.l10n.mojito.rest.asset.AssetSpecification.repositoryIdEquals;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.data.jpa.domain.Specifications.where;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import static com.box.l10n.mojito.rest.asset.AssetSpecification.virtualEquals;

/**
 * @author aloison
 */
@RestController
public class AssetWS {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetWS.class);

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    RepositoryLocaleRepository repositoryLocaleRepository;

    @Autowired
    TMService tmService;

    @Autowired
    AssetService assetService;

    /**
     * Gets the list of {@link Asset} for a given {@link Repository} and other
     * optional filters
     *
     * @param repositoryId {@link Repository#id}
     * @param path {@link Asset#path}
     * @param deleted
     * @param virtualContent
     * @return the list of {@link Asset} for a given {@link Repository}
     */
    @RequestMapping(value = "/api/assets", method = RequestMethod.GET)
    public List<Asset> getAssets(@RequestParam(value = "repositoryId") Long repositoryId,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "deleted", required = false) Boolean deleted,
            @RequestParam(value = "virtual", required = false) Boolean virtualContent) {

        return assetRepository.findAll(where(ifParamNotNull(repositoryIdEquals(repositoryId)))
                 .and(ifParamNotNull(pathEquals(path)))
                 .and(ifParamNotNull(deletedEquals(deleted)))
                 .and(ifParamNotNull(virtualEquals(virtualContent))
                )
        );
    }

    /**
     * Creates the source asset and kicks off extraction process
     *
     * @param sourceAsset The source asset to be imported
     * @return The created asset
     * @throws java.util.concurrent.ExecutionException
     * @throws java.lang.InterruptedException
     */
    @RequestMapping(value = "/api/assets", method = RequestMethod.POST)
    public SourceAsset importSourceAsset(@RequestBody SourceAsset sourceAsset) throws Throwable {
        logger.debug("Importing source asset");

        // ********************************************
        // TODO(P1) check permission to update the repo
        // ********************************************
        String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
        PollableFuture<Asset> assetFuture = assetService.addOrUpdateAssetAndProcessIfNeeded(
                sourceAsset.getRepositoryId(),
                normalizedContent,
                sourceAsset.getPath(),
                sourceAsset.getFilterConfigIdOverride()
        );

        try {
            sourceAsset.setAddedAssetId(assetFuture.get().getId());
        } catch (ExecutionException ee) {
            throw ee.getCause();
        }
        
        sourceAsset.setPollableTask(assetFuture.getPollableTask());

        return sourceAsset;
    }

    /**
     * Localizes the payload content with translations of a given {@link Asset}.
     *
     * This is usually to translate an asset that was slightly modified (for
     * example during development or when using different branches) compared to
     * the asset version stored in the database (usually synchronized with a CI
     * tool).
     *
     * @param assetId {@link Asset#id}
     * @param localeId {@link Locale#id}
     * @param localizedAssetBody the payload to be localized with optional
     * parameters
     * @return the localized payload as a {@link LocalizedAssetBody}
     */
    @RequestMapping(value = "/api/assets/{assetId}/localized/{localeId}", method = RequestMethod.POST)
    public LocalizedAssetBody getLocalizedAssetForContent(
            @PathVariable("assetId") long assetId,
            @PathVariable("localeId") long localeId,
            @RequestBody LocalizedAssetBody localizedAssetBody) {

        logger.debug("Localizing content payload with asset id = {}, and locale id = {}", assetId, localeId);

        Asset asset = assetRepository.getOne(assetId);
        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryIdAndLocaleId(asset.getRepository().getId(), localeId);

        String normalizedContent = NormalizationUtils.normalize(localizedAssetBody.getContent());

        String generateLocalized = tmService.generateLocalized(
                asset,
                normalizedContent,
                repositoryLocale,
                localizedAssetBody.getOutputBcp47tag(),
                localizedAssetBody.getFilterConfigIdOverride());

        localizedAssetBody.setContent(generateLocalized);

        if (localizedAssetBody.getOutputBcp47tag() != null) {
            localizedAssetBody.setBcp47Tag(localizedAssetBody.getOutputBcp47tag());
        } else {
            localizedAssetBody.setBcp47Tag(repositoryLocale.getLocale().getBcp47Tag());
        }

        return localizedAssetBody;
    }

    /**
     * Pseudo localizes the payload content with translations of a given
     * {@link Asset}.
     *
     * @param assetId {@link Asset#id}
     * @param localizedAssetBody the payload to be localized with optional
     * parameters
     * @return the pseudo localized payload as a {@link LocalizedAssetBody}
     */
    @RequestMapping(value = "/api/assets/{assetId}/pseudo", method = RequestMethod.POST)
    public LocalizedAssetBody getPseudoLocalizedAssetForContent(
            @PathVariable("assetId") long assetId,
            @RequestBody LocalizedAssetBody localizedAssetBody) {

        logger.debug("Pseudo localizing content payload with asset id = {}", assetId);

        Asset asset = assetRepository.getOne(assetId);
        String normalizedContent = NormalizationUtils.normalize(localizedAssetBody.getContent());

        String generateLocalized = tmService.generatePseudoLocalized(
                asset,
                normalizedContent,
                localizedAssetBody.getFilterConfigIdOverride());

        localizedAssetBody.setContent(generateLocalized);

        return localizedAssetBody;
    }

    //TODO(P1) It would be nice to put this to be POST on .../localized/{localeId}
    // but it won't backward compatible so... The URL is taken by another POST 
    // that is used as a GET because we needed to send the paylaod. Here would
    // be the logic URL usage
    @RequestMapping(value = "/api/assets/{assetId}/localized/{localeId}/import", method = RequestMethod.POST)
    public ImportLocalizedAssetBody importLocalizedAsset(
            @PathVariable("assetId") long assetId,
            @PathVariable("localeId") long localeId,
            @RequestBody ImportLocalizedAssetBody importLocalizedAssetBody) {

        
        logger.debug("Import localized asset with id = {}, and locale id = {}", assetId, localeId);

        Asset asset = assetRepository.getOne(assetId);
        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryIdAndLocaleId(asset.getRepository().getId(), localeId);

        String normalizedContent = NormalizationUtils.normalize(importLocalizedAssetBody.getContent());

        PollableFuture pollableFuture = tmService.importLocalizedAsset(
                asset,
                normalizedContent,
                repositoryLocale,
                importLocalizedAssetBody.getSourceEqualTargetProcessing(),
                importLocalizedAssetBody.getFilterConfigIdOverride());
        
        importLocalizedAssetBody.setPollableTask(pollableFuture.getPollableTask());
        
        return importLocalizedAssetBody;
    }

    /**
     * Exports all the translations (used and unused) of an {@link Asset} into
     * XLIFF.
     *
     * @param assetId {@link Asset#id}
     * @param bcp47tag bcp47 tag of translations to be exported
     * @return an XLIFF that contains all the translations of the {@link Asset}
     */
    @RequestMapping(method = RequestMethod.GET, value = "/api/assets/{assetId}/xliffExport")
    @ResponseStatus(HttpStatus.OK)
    public XliffExportBody xliffExport(@PathVariable("assetId") long assetId, @RequestParam("bcp47tag") String bcp47tag) {
        String content = tmService.exportAssetAsXLIFF(assetId, bcp47tag);
        String normalizedContent = NormalizationUtils.normalize(content);
        return new XliffExportBody(normalizedContent);
    }

    /**
     * Deletes one {@link Asset} by the {@link Asset#id}
     *
     * @param assetId
     * @return
     */
    @RequestMapping(value = "/api/assets/{assetId}", method = RequestMethod.DELETE)
    public ResponseEntity deleteAssetById(@PathVariable Long assetId) {
        logger.info("Deleting asset [{}]", assetId);

        ResponseEntity result;
        Asset asset = assetRepository.findOne(assetId);

        if (asset != null) {
            assetService.deleteAsset(asset);
            result = new ResponseEntity(HttpStatus.OK);
        } else {
            result = new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return result;
    }

    /**
     * Deletes multiple {@link Asset} by the list of {@link Asset#id}
     *
     * @param ids
     * @return
     */
    @RequestMapping(value = "/api/assets", method = RequestMethod.DELETE)
    public ResponseEntity deleteAssets(@RequestBody Set<Long> ids) {
        logger.info("Deleting assets: {}", ids.toString());
        assetService.deleteAssets(ids);
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Returns list of {@link Asset#id} for a given {@link Repository}
     *
     * @param repositoryId
     * @param deleted
     * @return
     */
    @RequestMapping(value = "/api/assets/ids", method = RequestMethod.GET)
    public Set<Long> getAssetIds(@RequestParam(value = "repositoryId", required = true) Long repositoryId,
            @RequestParam(value = "deleted", required = false) Boolean deleted) {
        if (deleted == null) {
            return assetRepository.findIdByRepositoryId(repositoryId);
        } else {
            return assetRepository.findIdByRepositoryIdAndDeleted(repositoryId, deleted);
        }
    }
}
