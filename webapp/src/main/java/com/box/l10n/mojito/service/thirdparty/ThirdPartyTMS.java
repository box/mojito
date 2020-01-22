package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;

import java.util.List;
import java.util.Map;

/**
 * Interface to be implemented for a third party TMS in order to be able to map its textunit with Mojito's, upload
 * screenshots to the TMS and link them with the third party text units.
 */
interface ThirdPartyTMS {

    /**
     * Uploads an image into the third party TMS
     *
     * @param projectId the third party project id
     * @param name      the image name
     * @param content   the image content
     * @return the image that was created in the third party system
     */
    ThirdPartyTMSImage uploadImage(String projectId, String name, byte[] content);

    /**
     * Gets the text units from the third party TMS
     *
     * @param repository the Mojito repository (info can be used to compute file uri, look sub-project etc)
     * @param projectId  the third party project id
     * @return the list of text units from the third party TMS
     */
    List<ThirdPartyTextUnit> getThirdPartyTextUnits(Repository repository, String projectId);

    /**
     * Create mappings (images to text units) in the third party TMS
     *
     * @param projectId                  the third party project id
     * @param thirdPartyImageToTextUnits the list of mappings
     */
    void createImageToTextUnitMappings(String projectId, List<ThirdPartyImageToTextUnit> thirdPartyImageToTextUnits);

    void syncSources(Repository repository, String projectId, List<TextUnitDTO> textUnitDTOList, String pluralSeparator, List<String> options, int batchNumber, boolean isSingular);

    void syncTranslations(Repository repository, String projectId, String pluralSeparator, List<String> options, Map<String, String> localeMappings);

    void uploadLocalizedFiles(Repository repository, String projectId, String locale, List<TextUnitDTO> textUnitDTOList, String pluralSeparator, List<String> options, Map<String, String> localeMappings, int batchNumber, boolean isSingular);
}
