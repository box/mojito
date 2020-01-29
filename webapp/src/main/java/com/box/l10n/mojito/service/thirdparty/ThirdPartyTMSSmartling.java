package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.smartling.AssetPathAndTextUnitNameKeys;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.request.Binding;
import com.box.l10n.mojito.smartling.request.Bindings;
import com.box.l10n.mojito.smartling.response.ContextUpload;
import com.box.l10n.mojito.smartling.response.File;
import com.box.l10n.mojito.smartling.response.StringInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ConditionalOnProperty(value = "l10n.third-party-tms.impl", havingValue = "ThirdPartyTMSSmartling")
@Component
public class ThirdPartyTMSSmartling implements ThirdPartyTMS {

    static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSSmartling.class);

    @Autowired
    SmartlingClient smartlingClient;

    @Autowired
    AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys;

    @Override
    public ThirdPartyTMSImage uploadImage(String projectId, String name, byte[] content) {
        logger.debug("Upload image to Smartling, project id: {}, name: {}", projectId, name);
        ContextUpload contextUpload = smartlingClient.uploadContext(projectId, name, content);
        ThirdPartyTMSImage thirdPartyTMSImage = new ThirdPartyTMSImage();
        thirdPartyTMSImage.setId(contextUpload.getContextUid());
        return thirdPartyTMSImage;
    }

    @Override
    public List<ThirdPartyTextUnit> getThirdPartyTextUnits(Repository repository, String projectId) {

        logger.debug("Get third party text units for repository: {} and project id: {}", repository.getId(), projectId);

        Pattern filePattern = getFilePattern(repository.getName());

        List<File> files = smartlingClient.getFiles(projectId).getItems().stream()
                .filter(file -> filePattern.matcher(file.getFileUri()).matches())
                .collect(Collectors.toList());

        List<ThirdPartyTextUnit> thirdPartyTextUnits = files.stream().flatMap(file -> {
            Stream<StringInfo> stringInfos = smartlingClient.getStringInfos(projectId, file.getFileUri());
            return stringInfos.map(stringInfo -> {
                logger.debug("hashcode: {}\nvariant: {}\nparsed string: {}",
                        stringInfo.getHashcode(),
                        stringInfo.getStringVariant(),
                        stringInfo.getParsedStringText());

                AssetPathAndTextUnitNameKeys.Key key = assetPathAndTextUnitNameKeys.parse(stringInfo.getStringVariant());

                ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
                thirdPartyTextUnit.setId(stringInfo.getHashcode());
                thirdPartyTextUnit.setAssetPath(key.getAssetPath());
                thirdPartyTextUnit.setName(key.getTextUnitName());
                thirdPartyTextUnit.setContent(stringInfo.getStringVariant());
                thirdPartyTextUnit.setNamePluralPrefix(isPluralFile(file.getFileUri()));

                return thirdPartyTextUnit;
            });
        }).collect(Collectors.toList());

        return thirdPartyTextUnits;
    }

    @Override
    public void createImageToTextUnitMappings(String projectId, List<ThirdPartyImageToTextUnit> thirdPartyImageToTextUnits) {
        logger.debug("Upload image to text units mapping for project id: {}", projectId);
        Bindings bindings = new Bindings();

        List<Binding> bindingList = thirdPartyImageToTextUnits.stream().map(thirdPartyImageToTextUnit -> {
            Binding binding = new Binding();
            binding.setStringHashcode(thirdPartyImageToTextUnit.getTextUnitId());
            binding.setContextUid(thirdPartyImageToTextUnit.getImageId());
            return binding;
        }).collect(Collectors.toList());

        bindings.setBindings(bindingList);
        smartlingClient.createBindings(bindings, projectId);
    }

    Pattern getFilePattern(String repositoryName) {
        return Pattern.compile(repositoryName + "/(\\d+)_(singular|plural)_source.xml");
    }

    Boolean isPluralFile(String fileUri) {
        return fileUri.endsWith("plural_source.xml");
    }
}
