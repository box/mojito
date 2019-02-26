package com.box.l10n.mojito.service.assetExtraction.extractor;

import com.box.l10n.mojito.okapi.filters.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

//TODO(P1) Isn't that already supported by okapi?
/**
 * Helper class to map an asset type to a filter config.
 * This allows to create a generic pipeline that can process different types of asset.
 *
 * @author aloison
 */
@Component
public class AssetPathToFilterConfigMapper {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetPathToFilterConfigMapper.class);

    public static final String XLIFF_FILTER_CONFIG_ID = "okf_xliff";

    //TODO(P1) check if we want escaped or not, potentially configurable.
    //TODO(P1) Trim comment coming from this PropertiesFilter? # comment gives " comment"
    public static final String PROPERTIES_FILTER_CONFIG_ID = "okf_properties-outputNotEscaped";
    public static final String MACSTRINGS_FILTER_CONFIG_ID = MacStringsFilter.FILTER_CONFIG_ID + "-macStrings";
    public static final String RESX_FILTER_CONFIG_ID = XMLFilter.FILTER_CONFIG_ID + "-resx";
    public static final String XTB_FILTER_CONFIG_ID = XMLFilter.FILTER_CONFIG_ID + "-xtb";
    public static final String JS_FILTER_CONFIG_ID = JSFilter.FILTER_CONFIG_ID + "-js";
    
    private enum AssetFilterType {
        CSV(CSVFilter.FILTER_CONFIG_ID, "csv"),
        XLIFF(XLIFF_FILTER_CONFIG_ID, "xlf", "xliff", "sdlxliff", "mxliff"),
        PROPERTIES(PROPERTIES_FILTER_CONFIG_ID, "properties"),
        ANDROIDSTRINGS(AndroidFilter.FILTER_CONFIG_ID, "xml"),
        MACSTRINGS(MACSTRINGS_FILTER_CONFIG_ID, "strings"),
        MACSTRINGSDICT(MacStringsdictFilter.FILTER_CONFIG_ID, "stringsdict"),
        PO(POFilter.FILTER_CONFIG_ID, "pot"),
        RESX(RESX_FILTER_CONFIG_ID, "resx", "resw"),
        XTB(XTB_FILTER_CONFIG_ID, "xtb"),
        JS(JS_FILTER_CONFIG_ID, "ts", "js"),
        JSON(JSONFilter.FILTER_CONFIG_ID, "json");

        private String configId;
        private Set<String> supportedExtensions = new HashSet<>();

        AssetFilterType(String configId, String... supportedExtensions) {
            this.configId = configId;
            this.supportedExtensions.addAll(Arrays.asList(supportedExtensions));
        }

        public String getConfigId() {
            return configId;
        }

        public Set<String> getSupportedExtensions() {
            return supportedExtensions;
        }

        /**
         * @param extension The file extension of the asset
         * @return The asset filter type associated to the extension or null if none found
         */
        public static AssetFilterType findByExtension(String extension) {
            for (AssetFilterType assetFilterType : AssetFilterType.values()) {
                if (assetFilterType.getSupportedExtensions().contains(extension)) {
                    return assetFilterType;
                }
            }

            return null;
        }
    }

    /**
     * @param path Path of the asset that will be filtered
     * @return The config ID associated to the given type or null if none found
     * @throws UnsupportedAssetFilterTypeException If cannot find a suitable filter configuration for the asset
     */
    public String getFilterConfigIdFromPath(String path) throws UnsupportedAssetFilterTypeException {

        AssetFilterType assetFilterType = getAssetFilterTypeFromPath(path);
        return assetFilterType.getConfigId();
    }

    /**
     * Returns the type of the asset based on its path (more specifically on its extension).
     *
     * @param assetPath The asset path
     * @return Type of the asset
     * @throws UnsupportedAssetFilterTypeException If cannot find a suitable filter configuration for the asset
     */
    private AssetFilterType getAssetFilterTypeFromPath(String assetPath) throws UnsupportedAssetFilterTypeException {

        String extension = FilenameUtils.getExtension(assetPath).toLowerCase();
        AssetFilterType assetFilterType = AssetFilterType.findByExtension(extension);

        if (assetFilterType == null) {
            logger.error("Cannot find a suitable filter configuration for the asset: " + assetPath);
            throw new UnsupportedAssetFilterTypeException("Cannot find a suitable filter configuration for the asset: " + assetPath);
        }

        return assetFilterType;
    }
}
