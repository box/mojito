package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.client.exception.AssetNotFoundException;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.rest.entity.FilterConfigIdOverride;
import com.box.l10n.mojito.rest.entity.ImportLocalizedAssetBody;
import com.box.l10n.mojito.rest.entity.Locale;
import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.SourceAsset;
import com.box.l10n.mojito.rest.entity.XliffExportBody;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author wyau
 */
@Component
public class AssetClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetClient.class);

    public static final String OUTPUT_BCP47_TAG = "en-x-pseudo";

    @Override
    public String getEntityName() {
        return "assets";
    }

    /**
     * Send the source asset to the server and start the extraction process
     *
     * @param sourceAsset
     * @return
     */
    public SourceAsset sendSourceAsset(SourceAsset sourceAsset) {
        return authenticatedRestTemplate.postForObject(
                getBasePathForEntity(),
                sourceAsset,
                SourceAsset.class
        );
    }

    /**
     * Gets a localized version of provided content, the content is related to a
     * given Asset.
     * <p/>
     * <p/>
     * The content can be a new version of the asset stored in the TMS. This is
     * used to localize files during development with usually minor changes done
     * to the persisted asset.
     *
     * @param assetId {@link Asset#id}
     * @param localeId {@link Locale#id}
     * @param content the asset content to be localized
     * @param outputBcp47tag Optional, can be null. Allows to generate the file
     * for a bcp47 tag that is different from the repository locale (which is
     * still used to fetch the translations). This can be used to generate a
     * file with tag "fr" even if the translations are stored with fr-FR
     * repository locale.
     * @param filterConfigIdOverride Optional, can be null. Allows to specify a
     * specific Okapi filter to use to process the asset
     * @return the localized asset content
     */
    public LocalizedAssetBody getLocalizedAssetForContent(
            Long assetId,
            Long localeId,
            String content,
            String outputBcp47tag,
            FilterConfigIdOverride filterConfigIdOverride,
            LocalizedAssetBody.InheritanceMode inheritanceMode,
            LocalizedAssetBody.Status status) {
        logger.debug("Getting localized asset with asset id = {}, locale id = {}, outputBcp47tag: {}", assetId, localeId, outputBcp47tag);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath(getBasePathForResource(assetId, "localized", localeId));

        LocalizedAssetBody localizedAssetBody = new LocalizedAssetBody();
        localizedAssetBody.setContent(content);
        localizedAssetBody.setOutputBcp47tag(outputBcp47tag);
        localizedAssetBody.setFilterConfigIdOverride(filterConfigIdOverride);
        localizedAssetBody.setInheritanceMode(inheritanceMode);
        localizedAssetBody.setStatus(status);

        return authenticatedRestTemplate.postForObject(uriBuilder.toUriString(),
                localizedAssetBody,
                LocalizedAssetBody.class);
    }

    /**
     * Gets a pseudo localized version of provided content, the content is
     * related to a given Asset.
     *
     * The content can be a new version of the asset stored in the TMS. This is
     * used to pseudo localize files during development with usually minor
     * changes done to the persisted asset.
     *
     * @param assetId {@link Asset#id}
     * @param content the asset content to be pseudolocalized
     * @param filterConfigIdOverride Optional, can be null. Allows to specify a
     * specific Okapi filter to use to process the asset
     * @return the pseudoloocalized asset content
     */
    public LocalizedAssetBody getPseudoLocalizedAssetForContent(Long assetId, String content, FilterConfigIdOverride filterConfigIdOverride) {

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath(getBasePathForResource(assetId, "pseudo"));

        LocalizedAssetBody localizedAssetBody = new LocalizedAssetBody();
        localizedAssetBody.setContent(content);
        localizedAssetBody.setOutputBcp47tag(OUTPUT_BCP47_TAG);
        localizedAssetBody.setFilterConfigIdOverride(filterConfigIdOverride);

        return authenticatedRestTemplate.postForObject(uriBuilder.toUriString(),
                localizedAssetBody,
                LocalizedAssetBody.class);
    }

    /**
     * Imports a localized version of an asset.
     *
     * The target strings are checked against the source strings and if they are
     * equals the status of the imported translation is defined by
     * statusForEqualTarget. When SKIIED is specified the import is actually
     * skipped.
     *
     * For not fully translated locales, targets are imported only if they are
     * different from target of the parent locale.
     *
     * @param assetId {@link Asset#id}
     * @param localeId {@link Locale#id}
     * @param content the asset content to be localized
     * @param statusForEqualTarget the status of the text unit variant when
     * the target is the same as the parent
     * @param filterConfigIdOverride
     * @return 
     */
    public ImportLocalizedAssetBody importLocalizedAssetForContent(
            Long assetId,
            Long localeId,
            String content,
            ImportLocalizedAssetBody.StatusForEqualTarget statusForEqualTarget,
            FilterConfigIdOverride filterConfigIdOverride) {
        logger.debug("Import localized asset with asset id = {}, locale id = {}", assetId, localeId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath(getBasePathForResource(assetId, "localized", localeId, "import"));

        ImportLocalizedAssetBody importLocalizedAssetBody = new ImportLocalizedAssetBody();
        importLocalizedAssetBody.setContent(content);
        importLocalizedAssetBody.setStatusForEqualTarget(statusForEqualTarget);
        importLocalizedAssetBody.setFilterConfigIdOverride(filterConfigIdOverride);

        return authenticatedRestTemplate.postForObject(uriBuilder.toUriString(),
                importLocalizedAssetBody,
                ImportLocalizedAssetBody.class);
    }

    /**
     * Get the asset that maps to the path and repositoryId
     *
     * @param path {@link Asset#path}
     * @param repositoryId {@link Asset#repository} id
     * @return {@link Asset}
     * @throws AssetNotFoundException
     */
    public Asset getAssetByPathAndRepositoryId(String path, Long repositoryId) throws AssetNotFoundException {

        Assert.notNull(path);
        Assert.notNull(repositoryId);

        List<Asset> assets = getAssets(path, repositoryId);

        if (assets.size() > 0) {
            return assets.get(0);
        } else {
            throw new AssetNotFoundException("Could not find asset with path = [" + path + "] at repo id [" + repositoryId + "]");
        }
    }

    /**
     * Gets the list of {@link Asset}s in a {@link Repository}.
     *
     * @param repositoryId {@link Repository#id}
     * @return the list of {@link Asset}s in a {@link Repository}
     */
    public List<Asset> getAssetsByRepositoryId(Long repositoryId) {
        return getAssets(null, repositoryId);
    }

    /**
     * Get the list of {@link Asset} by the {@link Asset#repository#id}
     *
     * @param repositoryId {@link Asset#repository} id
     * @param deleted
     * @return list of {@link Asset}
     */
    public List<Asset> getAssetsByRepositoryId(Long repositoryId, Boolean deleted) {
        return getAssets(null, repositoryId, deleted);
    }

    protected List<Asset> getAssets(String path, Long repositoryId) {
        return getAssets(path, repositoryId, null);
    }

    /**
     * Get assets for a given path and repositoryId
     *
     * @param path {@link Asset#path}
     * @param repositoryId {@link Asset#repository} id
     * @param deleted
     * @return List of {@link Asset}s
     */
    protected List<Asset> getAssets(String path, Long repositoryId, Boolean deleted) {
        logger.debug("Get assets by path = {} repo id = {} deleted = {}", path, repositoryId, deleted);

        Map<String, String> filterParams = new HashMap<>();

        if (path != null) {
            filterParams.put("path", path);
        }

        if (repositoryId != null) {
            filterParams.put("repositoryId", repositoryId.toString());
        }

        if (deleted != null) {
            filterParams.put("deleted", deleted.toString());
        }

        return authenticatedRestTemplate.getForObjectAsListWithQueryStringParams(getBasePathForEntity(), Asset[].class, filterParams);
    }

    /**
     * Exports an XLIFF that contains all translation (regardless if they are
     * used or not) of an {@link Asset}.
     *
     * @param assetId {@link Asset#id} for which translation was exported
     * @param tmXliffId {@link TMXliff#id} where exported xliff is persisted
     * @return an XLIFF
     */
    public String getExportedXLIFF(Long assetId, Long tmXliffId) {
        logger.debug("Get exported xliff for asset id: {} for tm xliff id: {}", assetId, tmXliffId);

        String xliffExportBasePath = getBasePathForResource(assetId, "xliffExport", tmXliffId);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(xliffExportBasePath);

        return authenticatedRestTemplate.getForObject(uriBuilder.toUriString(), XliffExportBody.class).getContent();
    }

    /**
     * Exports an XLIFF that contains all translation (regardless if they are
     * used or not) of an {@link Asset} asynchronously.
     *
     * @param assetId {@link Asset#id} for which translation needs to be
     * exported
     * @param bcp47tag bcp47tag for which translation needs to be exported
     * @return {@link XliffExportBody}
     */
    public XliffExportBody exportAssetAsXLIFFAsync(Long assetId, String bcp47tag) {
        logger.debug("Export asset id: {} for locale: {}", assetId, bcp47tag);

        XliffExportBody xliffExportBody = new XliffExportBody();

        String xliffExportBasePath = getBasePathForResource(assetId, "xliffExport");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath(xliffExportBasePath)
                .queryParam("bcp47tag", bcp47tag);

        return authenticatedRestTemplate.postForObject(uriBuilder.toUriString(), xliffExportBody, XliffExportBody.class);
    }

    /**
     * Deletes an {@link Asset} by the {@link Asset#id}
     *
     * @param assetId
     */
    public void deleteAssetById(Long assetId) {
        logger.debug("Deleting asset by id = [{}]", assetId);
        authenticatedRestTemplate.delete(getBasePathForResource(assetId));
    }

    /**
     * Deletes multiple {@link Asset} by the list of {@link Asset#id}
     *
     * @param assetIds
     */
    public void deleteAssetsByIds(Set<Long> assetIds) {
        logger.debug("Deleting assets by asset ids = {}", assetIds.toString());

        HttpEntity<Set<Long>> httpEntity = new HttpEntity<>(assetIds);
        authenticatedRestTemplate.delete(getBasePathForEntity(), httpEntity);
    }

    /**
     * Returns list of {@link Asset#id} for a given {@link Repository}
     *
     * @param repositoryId
     * @param deleted optional
     * @param virtual optional
     * @return
     */
    public List<Long> getAssetIds(Long repositoryId, Boolean deleted, Boolean virtual) {
        Assert.notNull(repositoryId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath(getBasePathForEntity() + "/ids");

        Map<String, String> params = new HashMap<>();
        params.put("repositoryId", repositoryId.toString());

        if (deleted != null) {
            params.put("deleted", deleted.toString());
        }

        if (virtual != null) {
            params.put("virtual", virtual.toString());
        }

        return authenticatedRestTemplate.getForObjectAsListWithQueryStringParams(uriBuilder.toUriString(), Long[].class, params);
    }
}
