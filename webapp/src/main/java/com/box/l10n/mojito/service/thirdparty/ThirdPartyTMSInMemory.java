package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.smartling.request.Binding;
import com.box.l10n.mojito.smartling.request.Bindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSInMemory", matchIfMissing = true)
@Component
public class ThirdPartyTMSInMemory implements ThirdPartyTMS {

    static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSInMemory.class);

    HashMap<String, byte[]> images = new HashMap<>();
    HashMap<String, HashMap<String, String>> imageToTextUnitMappings = new HashMap<>();

    /**
     * Storing the uploaded images, that could be useful for testing but at the moment is useless...
     * TODO remove if we don't need for testing
     */
    @Override
    public ThirdPartyTMSImage uploadImage(String projectId, String name, byte[] content) {
        logger.debug("Upload image to ThirdPartyTMSInMemory, project id: {}, name: {}", projectId, name);

        String imageId = UUID.randomUUID().toString();
        images.put(imageId, content);

        ThirdPartyTMSImage thirdPartyTMSImage = new ThirdPartyTMSImage();
        thirdPartyTMSImage.setId(imageId);
        return thirdPartyTMSImage;
    }

    @Override
    public List<ThirdPartyTextUnit> getThirdPartyTextUnits(Repository repository, String projectId) {
        logger.debug("Get third party text units for repository: {} and project id: {}", repository.getId(), projectId);

        // TODO return empty for now but later the interface will support adding strings and so this can retrun them
        return Collections.emptyList();
    }

    /**
     * Save the mappings, that could be useful for testing but at the moment is useless...
     * TODO remove if we don't need for testing
     */
    @Override
    public void createImageToTextUnitMappings(String projectId, List<ThirdPartyImageToTextUnit> thirdPartyImageToTextUnits) {
        logger.debug("Upload image to text units mapping for project id: {}", projectId);

        HashMap<String, String> forProject = imageToTextUnitMappings.computeIfAbsent(projectId, s -> new HashMap<>());

        for (ThirdPartyImageToTextUnit thirdPartyImageToTextUnit : thirdPartyImageToTextUnits) {
            forProject.put(thirdPartyImageToTextUnit.getImageId(), thirdPartyImageToTextUnit.getTextUnitId());
        }
    }
}
