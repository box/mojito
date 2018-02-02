package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.service.asset.VirtualAssetTextUnit;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.service.asset.VirtualAsset;
import com.box.l10n.mojito.service.asset.VirtualAssetRequiredException;
import com.box.l10n.mojito.service.asset.VirtualAssetService;
import com.box.l10n.mojito.service.asset.VirutalAssetMissingTextUnitException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public VirtualAsset createOrUpdateVirtualAsset(@RequestBody VirtualAsset virtualAsset) throws VirtualAssetRequiredException {
        return virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/textUnits", method = RequestMethod.POST)
    public void addTextUnits(
            @PathVariable("assetId") long assetId,
            @RequestBody List<VirtualAssetTextUnit> virtualAssetTextUnits) throws VirtualAssetRequiredException {

        virtualAssetService.addTextUnits(virtualAssetTextUnits, assetId);
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/textUnits", method = RequestMethod.PUT)
    public void replaceTextUnits(
            @PathVariable("assetId") long assetId,
            @RequestBody List<VirtualAssetTextUnit> virtualAssetTextUnits) throws VirtualAssetRequiredException {

        virtualAssetService.replaceTextUnits(assetId, virtualAssetTextUnits);
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/textUnits", method = RequestMethod.DELETE)
    public void deleteTextUnit(
            @PathVariable("assetId") long assetId,
            @RequestBody VirtualAssetTextUnit virtualAssetTextUnit) {
        virtualAssetService.deleteTextUnit(assetId, virtualAssetTextUnit.getName());
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/locale/{localeId}/textUnits", method = RequestMethod.POST)
    public void importLocalizedTextUnits(
            @PathVariable("assetId") long assetId,
            @PathVariable("localeId") long localeId,
            @RequestBody List<VirtualAssetTextUnit> textUnitForVirtualAssets) throws VirtualAssetRequiredException, VirutalAssetMissingTextUnitException {

        virtualAssetService.importLocalizedTextUnits(assetId, localeId, textUnitForVirtualAssets);
    }

    @RequestMapping(value = "/api/virtualAssets/{assetId}/textUnits", method = RequestMethod.GET)
    public List<VirtualAssetTextUnit> getTextUnits(@PathVariable("assetId") long assetId) throws VirtualAssetRequiredException {
        return virtualAssetService.getTextUnits(assetId);
    }
    
    @RequestMapping(value = "/api/virtualAssets/{assetId}/locale/{localeId}/textUnits", method = RequestMethod.GET)
    public List<VirtualAssetTextUnit> getLocalizedTextUnits(
            @PathVariable("assetId") long assetId,
            @PathVariable(value = "localeId") long localeId,
            @RequestParam(value = "inheritanceMode", defaultValue = "USE_PARENT") InheritanceMode inheritanceMode) throws VirtualAssetRequiredException {

        return virtualAssetService.getLoalizedTextUnits(assetId, localeId, inheritanceMode);
    }

}
