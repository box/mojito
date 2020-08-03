package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Repository;

import java.util.List;

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

    /**
     * Push source strings to the third party TMS
     *
     * @param repository                The Mojito repository
     * @param projectId                 The third party project Id
     * @param pluralSeparator           The plural separator to be used in file generation
     * @param skipTextUnitsWithPattern  The pattern to skip when searching text units
     * @param skipAssetsWithPathPattern The path pattern to skip when searching assets
     * @param options                   The options being passed to the third party API
     * @return The list of files generated in the operation
     */
    void push(Repository repository,
              String projectId,
              String pluralSeparator,
              String skipTextUnitsWithPattern,
              String skipAssetsWithPathPattern,
              List<String> options);
}
