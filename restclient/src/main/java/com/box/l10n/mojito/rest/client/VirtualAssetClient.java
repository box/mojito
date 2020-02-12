package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.VirtualAssetTextUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author wyau
 */
@Component
public class VirtualAssetClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(VirtualAssetClient.class);

    @Override
    public String getEntityName() {
        return "virtualAssets";
    }

    public VirtualAsset createOrUpdate(VirtualAsset virtualAsset) {
        return authenticatedRestTemplate.postForObject(
                getBasePathForEntity(),
                virtualAsset,
                VirtualAsset.class
        );
    }

    public PollableTask addTextUnits(long assetId, List<VirtualAssetTextUnit> virtualAssetTextUnits) {
        String uriString = getVirtualTextUnitsUri(assetId);
        return authenticatedRestTemplate.postForObject(uriString, virtualAssetTextUnits, PollableTask.class);
    }

    public PollableTask repalceTextUnits(long assetId, List<VirtualAssetTextUnit> virtualAssetTextUnits) {
        String uriString = getVirtualTextUnitsUri(assetId);
        return authenticatedRestTemplate.putForObject(uriString, virtualAssetTextUnits, PollableTask.class);
    }

    public PollableTask importTextUnits(long assetId, long localeId, List<VirtualAssetTextUnit> virtualAssetTextUnits) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath(getBasePathForEntity())
                .pathSegment("{assetId}", "locale", Long.toString(localeId), "textUnits");
        String uriString = uriComponentsBuilder.buildAndExpand(assetId, localeId).toUriString();
        return authenticatedRestTemplate.postForObject(uriString, virtualAssetTextUnits, PollableTask.class);
    }

    public List<VirtualAssetTextUnit> getLocalizedTextUnits(long assetId, long localeId, String inheritanceMode) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath(getBasePathForEntity())
                .pathSegment("{assetId}", "locale", Long.toString(localeId), "textUnits");
        uriComponentsBuilder.queryParam("inheritanceMode", inheritanceMode);
        String uriString = uriComponentsBuilder.buildAndExpand(assetId, localeId).toUriString();
        VirtualAssetTextUnit[] virtualAssetTextUnits = authenticatedRestTemplate.getForObject(uriString, VirtualAssetTextUnit[].class);
        return Arrays.asList(virtualAssetTextUnits);
    }

    String getVirtualTextUnitsUri(long assetId) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath(getBasePathForEntity())
                .pathSegment("{assetId}", "textUnits");
        return uriComponentsBuilder.buildAndExpand(assetId).toUriString();
    }
}