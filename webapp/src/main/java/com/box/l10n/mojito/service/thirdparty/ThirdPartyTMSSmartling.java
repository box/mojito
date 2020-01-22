package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.android.strings.AndroidStringsTextUnit;
import com.box.l10n.mojito.android.strings.AndroidStringsXmlHelper;
import com.box.l10n.mojito.android.strings.AndroidStringsXmlReader;
import com.box.l10n.mojito.android.strings.AndroidStringsXmlWriter;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.box.l10n.mojito.android.strings.AndroidStringsXmlHelper.PluralItem;

@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSSmartling")
@Component
public class ThirdPartyTMSSmartling implements ThirdPartyTMS {

    static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSSmartling.class);

    @Autowired
    SmartlingClient smartlingClient;

    @Autowired
    AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys;

    @Autowired
    TextUnitBatchImporterService textUnitBatchImporterService;

    @Autowired
    PollableTaskService pollableTaskService;

    @Override
    public ThirdPartyTMSImage uploadImage(String projectId, String name, byte[] content) {
        logger.debug("Upload image to Smartling, project id: {}, name: {}", projectId, name);
        ContextUpload contextUpload = smartlingClient.uploadContext(projectId, name, content);
        ThirdPartyTMSImage thirdPartyTMSImage = new ThirdPartyTMSImage();
        thirdPartyTMSImage.setId(contextUpload.getContextUid());
        return thirdPartyTMSImage;
    }

    List<File> getFiles(Repository repository, String projectId) {
        Pattern filePattern = getFilePattern(repository.getName());

        return smartlingClient.getFiles(projectId).getItems().stream()
                .filter(file -> filePattern.matcher(file.getFileUri()).matches())
                .collect(Collectors.toList());
    }

    @Override
    public List<ThirdPartyTextUnit> getThirdPartyTextUnits(Repository repository, String projectId) {

        logger.debug("Get third party text units for repository: {} and project id: {}", repository.getId(), projectId);

        List<File> files = getFiles(repository, projectId);

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

    static final String SKIP_NAME_PATTERN_PARAM = "skip-name-pattern";
    static final String ASSET_PATH_PATTERN_PARAM = "asset-path-pattern";

    static final String PLACEHOLDER_FORMAT_PARAM = "smartling-placeholder-format";
    static final String PLACEHOLDER_FORMAT_CUSTOM_PARAM = "smartling-placeholder-format-custom";

    static final String SKIP_PLURAL_FIX_PARAM = "skip-plural-fix";

    static final String ASSET_DELIMITER = "#@#";

    @Override
    public void syncSources(Repository repository, String projectId, List<TextUnitDTO> textUnitDTOList, String pluralSeparator, List<String> options, int batchNumber, boolean isSingular) {

        Map<String, String> optionMap = convertOptionsToMap(options);

        logger.info("Process batch: {}", batchNumber);
        List<AndroidStringsTextUnit> textUnitList = textUnitDTOList.stream()
                .filter(TextUnitDTO::isTranslated)
                .filter(getTextUnitFilterBy(optionMap.get(SKIP_NAME_PATTERN_PARAM), optionMap.get(ASSET_PATH_PATTERN_PARAM)))
                .map(getConverterDataUnitToAndroidUnit(TextUnitDTO::getSource))
                .collect(Collectors.toList());
        try {
            logger.info("Convert text units to xml for batch number: {}", batchNumber);
            String fileContent = AndroidStringsXmlWriter.toText(textUnitList, pluralSeparator);
            String fileName = getFileName(repository.getName(), batchNumber, getTextUnitType(isSingular));

            logger.info("Push Android file to Smartling project: {}", projectId);
            smartlingClient.uploadFile(projectId, fileName, "android", fileContent,
                    optionMap.get(PLACEHOLDER_FORMAT_PARAM), optionMap.get(PLACEHOLDER_FORMAT_CUSTOM_PARAM));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void syncTranslations(Repository repository, String projectId, String pluralSeparator, List<String> options, Map<String, String> localeMappings) {

        for (File file : getFiles(repository, projectId)) {
            Map<String, PollableTask> pollableTaskIdsByLocale = new HashMap<>();
            for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
                String locale = repositoryLocale.getLocale().getBcp47Tag();
                String smartlingLocale = localeMappings.getOrDefault(locale, locale);

                logger.info("Download localized file from Smartling for file: {}, Mojito locale: {} and Smartling locale: {}", file.getFileUri(), locale, smartlingLocale);
                String fileContent = smartlingClient.downloadFile(projectId, file.getFileUri(), smartlingLocale,
                        false, SmartlingClient.RetrievalType.PUBLISHED);

                List<AndroidStringsTextUnit> textUnitList = Collections.emptyList();
                try {
                    logger.info("Upload localized text units to Mojito: {}", locale);
                    textUnitList = AndroidStringsXmlReader.fromText(fileContent, pluralSeparator);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                List<TextUnitDTO> textUnitDTOList = new ArrayList<>(textUnitList.size());
                for (AndroidStringsTextUnit textUnit : textUnitList) {
                    String[] name = textUnit.getName().split(ASSET_DELIMITER);

                    TextUnitDTO textUnitDTO = new TextUnitDTO();
                    textUnitDTO.setAssetPath(name[0]);
                    textUnitDTO.setName(name[1]);
                    textUnitDTO.setTarget(textUnit.getContent());
                    textUnitDTO.setRepositoryName(repository.getName());
                    textUnitDTO.setTargetLocale(locale);
                    textUnitDTO.setIncludedInLocalizedFile(true);
                    textUnitDTO.setTmTextUnitId(textUnit.getId());
                    textUnitDTO.setPluralForm(textUnit.getPluralForm());
                    textUnitDTO.setPluralFormOther(textUnit.getPluralFormOther());

                    textUnitDTOList.add(textUnitDTO);
                }

                //TODO(ja) remove this when smarlting is fixed
                if (hasPluralFix(locale, options)) {
                    for (int i = textUnitDTOList.size(); --i >= 0; ) {
                        TextUnitDTO textUnitDTO = textUnitDTOList.get(i);

                        //add missing plural for "many", copy other for now...
                        if (PluralItem.other.name().equals(textUnitDTO.getPluralForm())) {
                            TextUnitDTO copyOfTextUnitDTO = new TextUnitDTO();
                            copyOfTextUnitDTO.setAssetPath(textUnitDTO.getAssetPath());
                            copyOfTextUnitDTO.setName(textUnitDTO.getName().replace(PluralItem.other.name(), PluralItem.many.name()));
                            copyOfTextUnitDTO.setTarget(textUnitDTO.getTarget());
                            copyOfTextUnitDTO.setRepositoryName(textUnitDTO.getRepositoryName());
                            copyOfTextUnitDTO.setTargetLocale(textUnitDTO.getTargetLocale());
                            copyOfTextUnitDTO.setIncludedInLocalizedFile(textUnitDTO.isIncludedInLocalizedFile());
                            copyOfTextUnitDTO.setTmTextUnitId(textUnitDTO.getTmTextUnitId());
                            copyOfTextUnitDTO.setPluralForm(PluralItem.many.name());
                            copyOfTextUnitDTO.setPluralFormOther(textUnitDTO.getPluralFormOther());
                            textUnitDTOList.add(copyOfTextUnitDTO);
                        }
                    }
                }

                PollableFuture pollableFuture = textUnitBatchImporterService.asyncImportTextUnits(textUnitDTOList, false, true);
                pollableTaskIdsByLocale.put(locale, pollableFuture.getPollableTask());
            }

            boolean importTranslationFailed = false;
            for (Map.Entry<String, PollableTask> entry : pollableTaskIdsByLocale.entrySet()) {
                logger.info("Locale: {}, waiting for pollable task id: {}", entry.getKey(), entry.getValue());
                try {
                    PollableTask pollableTask = pollableTaskService.waitForPollableTask(entry.getValue().getId());
                    if (pollableTask.getErrorMessage() == null) {
                        logger.error(pollableTask.getErrorMessage());
                        importTranslationFailed = true;
                    }
                } catch (InterruptedException e) {
                    importTranslationFailed = true;
                }
            }

            if (importTranslationFailed) {
                logger.error("Translation import failed, see logs for details");
            }
        }
    }

    @Override
    public void uploadLocalizedFiles(Repository repository, String projectId, String locale, List<TextUnitDTO> textUnitDTOList, String pluralSeparator, List<String> options, Map<String, String> localeMappings, int batchNumber, boolean isSingular) {

        Map<String, String> optionMap = convertOptionsToMap(options);
        boolean pluralFix = !isSingular && hasPluralFix(locale, options);
        String smartlingLocale = localeMappings.getOrDefault(locale, locale);

        logger.info("Convert text units to AndroidString for asset number: {}", batchNumber);
        List<AndroidStringsTextUnit> textUnitList = textUnitDTOList.stream()
                .filter(TextUnitDTO::isTranslated)
                .filter(item -> !pluralFix || !PluralItem.many.name().equals(item.getPluralForm()))
                .filter(getTextUnitFilterBy(optionMap.get(SKIP_NAME_PATTERN_PARAM), optionMap.get(ASSET_PATH_PATTERN_PARAM)))
                .map(getConverterDataUnitToAndroidUnit(TextUnitDTO::getTarget))
                .collect(Collectors.toList());

        try {
            String fileName = getFileName(repository.getName(), batchNumber, getTextUnitType(isSingular));
            String fileContent = AndroidStringsXmlWriter.toText(textUnitList, pluralSeparator);

            logger.info("Push Android file to Smartling project: {}", projectId);
            smartlingClient.uploadLocalizedFile(projectId, fileName, "android", fileContent, smartlingLocale,
                    optionMap.get(PLACEHOLDER_FORMAT_PARAM), optionMap.get(PLACEHOLDER_FORMAT_CUSTOM_PARAM));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    boolean hasPluralFix(String locale, List<String> options) {
        return ("cs-CZ".equals(locale) || "sk-SK".equals(locale)) && !options.contains(SKIP_PLURAL_FIX_PARAM);
    }

    String getTextUnitType(boolean isSingular) {
        return isSingular ? "singular" : "plural";
    }

    Function<TextUnitDTO, AndroidStringsTextUnit> getConverterDataUnitToAndroidUnit(Function<TextUnitDTO, String> getContent) {
        return item -> AndroidStringsXmlHelper.createTextUnit(
                item.getAssetPath() + ASSET_DELIMITER + item.getName(), removeBadCharacters(getContent.apply(item)),
                item.getComment(), item.getTmTextUnitId(), item.getPluralForm(), item.getPluralFormOther());
    }

    Predicate<TextUnitDTO> getTextUnitFilterBy(String skipNamePattern, String assetPathPattern) {

        if (skipNamePattern != null) {
            logger.info("Filter text units with name matching pattern: {}", skipNamePattern);
        }

        if (assetPathPattern != null) {
            logger.info("Get text units with asset path matching pattern: {}", assetPathPattern);
        }

        return item -> (skipNamePattern == null || Boolean.FALSE.equals(Pattern.matches(skipNamePattern, item.getName())))
                && (assetPathPattern == null || Pattern.matches(assetPathPattern, item.getAssetPath()));
    }

    Map<String, String> convertOptionsToMap(List<String> options) {
        return Optional.ofNullable(options).orElse(Collections.emptyList()).stream().collect(
                Collectors.toMap(str -> str.split("=")[0], str -> str.split("=")[1], (a, b) -> a, HashMap::new));
    }

    String getFileName(String repositoryName, int batchNumber, String prefix) {
        return String.format("%s/%s_%s_source.xml", repositoryName,
                String.format("%5d", batchNumber).replace(' ', '0'), prefix);
    }

    String removeBadCharacters(String str) {
        return str.replace("\u001d", "").replace("\u001c", "").replace("\u0000", "");
    }

    Pattern getFilePattern(String repositoryName) {
        return Pattern.compile(repositoryName + "/(\\d+)_(singular|plural)_source.xml");
    }

    Boolean isPluralFile(String fileUri) {
        return fileUri.endsWith("plural_source.xml");
    }
}
