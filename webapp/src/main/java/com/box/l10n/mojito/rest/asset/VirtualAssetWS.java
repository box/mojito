package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.service.asset.VirtualAsset;
import com.box.l10n.mojito.service.asset.VirtualAssetBadRequestException;
import com.box.l10n.mojito.service.asset.VirtualAssetRequiredException;
import com.box.l10n.mojito.service.asset.VirtualAssetService;
import com.box.l10n.mojito.service.asset.VirtualAssetTextUnit;
import com.box.l10n.mojito.service.asset.VirutalAssetMissingTextUnitException;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author jaurambault
 */
@RestController
public class VirtualAssetWS {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(VirtualAssetWS.class);

    @Autowired
    VirtualAssetService virtualAssetService;

    @RequestMapping(value = "/api/virtualAssets", method = RequestMethod.POST)
    public VirtualAsset createOrUpdateVirtualAsset(@RequestBody VirtualAsset virtualAsset) throws VirtualAssetBadRequestException {
        return virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/textUnits", method = RequestMethod.POST)
    public PollableTask addTextUnits(
            @PathVariable("assetId") long assetId,
            @RequestBody List<VirtualAssetTextUnit> virtualAssetTextUnits) throws VirtualAssetRequiredException {

        PollableFuture addTextUnits = virtualAssetService.addTextUnits(assetId, virtualAssetTextUnits);
        return addTextUnits.getPollableTask();
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/textUnits", method = RequestMethod.PUT)
    public PollableTask replaceTextUnits(
            @PathVariable("assetId") long assetId,
            @RequestBody List<VirtualAssetTextUnit> virtualAssetTextUnits) throws VirtualAssetRequiredException {

        PollableFuture replaceTextUnitsTask = virtualAssetService.replaceTextUnits(assetId, virtualAssetTextUnits);
        return replaceTextUnitsTask.getPollableTask();
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/textUnits", method = RequestMethod.DELETE)
    public void deleteTextUnit(
            @PathVariable("assetId") long assetId,
            @RequestBody VirtualAssetTextUnit virtualAssetTextUnit) {
        virtualAssetService.deleteTextUnit(assetId, virtualAssetTextUnit.getName());
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/locale/{localeId}/textUnits", method = RequestMethod.POST)
    public PollableTask importLocalizedTextUnits(
            @PathVariable("assetId") long assetId,
            @PathVariable("localeId") long localeId,
            @RequestBody List<VirtualAssetTextUnit> textUnitForVirtualAssets) throws VirtualAssetRequiredException, VirutalAssetMissingTextUnitException {

        PollableFuture importLocalizedTextUnits = virtualAssetService.importLocalizedTextUnits(assetId, localeId, textUnitForVirtualAssets);
        return importLocalizedTextUnits.getPollableTask();
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/textUnits", method = RequestMethod.GET)
    public List<VirtualAssetTextUnit> getTextUnits(
            @PathVariable("assetId") long assetId,
            @RequestParam(value = "doNotTranslateFilter", required = false) Boolean doNotTranslateFilter) throws VirtualAssetRequiredException {
        return virtualAssetService.getTextUnits(assetId, doNotTranslateFilter);
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/locale/{localeId}/textUnits", method = RequestMethod.GET)
    public List<VirtualAssetTextUnit> getLocalizedTextUnits(
            @PathVariable("assetId") long assetId,
            @PathVariable(value = "localeId") long localeId,
            @RequestParam(value = "inheritanceMode", defaultValue = "USE_PARENT") InheritanceMode inheritanceMode) throws VirtualAssetRequiredException {

        return virtualAssetService.getLocalizedTextUnits(assetId, localeId, inheritanceMode);
    }

}
