package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.android.strings.AndroidStringDocumentMapper;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentReader;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.iterators.PageFetcherOffsetAndLimitSplitIterator;
import com.box.l10n.mojito.iterators.Spliterators;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFile;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingPluralFix;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.PluralFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.box.l10n.mojito.smartling.AssetPathAndTextUnitNameKeys;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.request.Binding;
import com.box.l10n.mojito.smartling.request.Bindings;
import com.box.l10n.mojito.smartling.response.ContextUpload;
import com.box.l10n.mojito.smartling.response.File;
import com.box.l10n.mojito.smartling.response.StringInfo;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.getOutputSourceFile;
import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.isPluralFile;
import static com.google.common.collect.Streams.mapWithIndex;

/**
 * tmTextUnitId are not preserved in Smartling plural localized files so we have to import based on name which
 * causes some challenges when there are ambiguities (eg. same name different comment).
 * <p>
 * In singular file, the id is preserved hence used during import.
 * <p>
 * Smartling accept android files with entries where the name is dupplicated. It maps to a single text unit in Smartling.
 * Both entries get the same translation in the localized files.
 * <p>
 * There is no constrain in mojito database that insure the plural text units are valid. This can cause issue with
 * the current implementation. we group by plural form other and the comment.
 */
@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSSmartling")
@Component
public class ThirdPartyTMSSmartling implements ThirdPartyTMS {

    public static final String ANDROID_FILE_TYPE = "android";
    /*
    Devisible by 6 so that the plurals won't be broken up between files
    We force pull all 6 forms regardless of language so that the files
    contain the same keys and can be properly connected to their source
    file in smartling.
    */
    static final int DEFAULT_BATCH_SIZE = 5004;

    static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSSmartling.class);
    private final SmartlingClient smartlingClient;
    private final AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys;
    private final TextUnitSearcher textUnitSearcher;
    private final TextUnitBatchImporterService textUnitBatchImporterService;
    private final Integer batchSize;

    @Autowired
    public ThirdPartyTMSSmartling(SmartlingClient smartlingClient,
                                  TextUnitSearcher textUnitSearcher,
                                  AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys,
                                  TextUnitBatchImporterService textUnitBatchImporterService) {
        this(smartlingClient, textUnitSearcher, assetPathAndTextUnitNameKeys,
                textUnitBatchImporterService, DEFAULT_BATCH_SIZE);
    }

    public ThirdPartyTMSSmartling(SmartlingClient smartlingClient,
                                  TextUnitSearcher textUnitSearcher,
                                  AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys,
                                  TextUnitBatchImporterService textUnitBatchImporterService,
                                  int batchSize) {
        this.smartlingClient = smartlingClient;
        this.assetPathAndTextUnitNameKeys = assetPathAndTextUnitNameKeys;
        this.textUnitBatchImporterService = textUnitBatchImporterService;
        this.textUnitSearcher = textUnitSearcher;
        this.batchSize = batchSize < 1 ? DEFAULT_BATCH_SIZE : batchSize;
    }

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

        List<File> files = getRepositoryFilesFromProject(repository, projectId);

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

    List<File> getRepositoryFilesFromProject(Repository repository, String projectId) {
        Pattern filePattern = SmartlingFileUtils.getFilePattern(repository.getName());

        List<File> files = smartlingClient.getFiles(projectId).getItems().stream()
                .filter(file -> filePattern.matcher(file.getFileUri()).matches())
                .collect(ImmutableList.toImmutableList());
        return files;
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

    @Override
    public void push(Repository repository,
                     String projectId,
                     String pluralSeparator,
                     String skipTextUnitsWithPattern,
                     String skipAssetsWithPathPattern,
                     List<String> optionList) {

        SmartlingOptions options = SmartlingOptions.parseList(optionList);
        // TODO(jean) looks like asset delimiter is not used, not the locale, nor the repository name, so why
        AndroidStringDocumentMapper mapper = new AndroidStringDocumentMapper(pluralSeparator, null, null, assetPathAndTextUnitNameKeys);

        long numberOfSingularPartitions = Spliterators.partitionWithIndex(
                getSingularTextUnitIterator(repository, skipTextUnitsWithPattern, skipAssetsWithPathPattern),
                DEFAULT_BATCH_SIZE,
                (textUnitDTOS, index) -> {
                    convertToAndroidAndUpload(textUnitDTOS, index, mapper, repository, projectId, options, Prefix.SINGULAR);
                    return index;
                })
                .count();

        removeFileForBatchNumbetGreatOrEquals(numberOfSingularPartitions, Prefix.SINGULAR);

        long numberOfPluralPartitions = Spliterators.partitionWithIndex(
                getPluralTextUnitIterator(repository, skipTextUnitsWithPattern, skipAssetsWithPathPattern),
                DEFAULT_BATCH_SIZE,
                (textUnitDTOS, index) -> {
                    convertToAndroidAndUpload(textUnitDTOS, index, mapper, repository, projectId, options, Prefix.PLURAL);
                    return index;
                })
                .count();

        removeFileForBatchNumbetGreatOrEquals(numberOfPluralPartitions, Prefix.PLURAL);
    }

    PageFetcherOffsetAndLimitSplitIterator<TextUnitDTO> getSingularTextUnitIterator(
            Repository repository,
            String skipTextUnitsWithPattern,
            String skipAssetsWithPathPattern) {

        PageFetcherOffsetAndLimitSplitIterator<TextUnitDTO> textUnitDTOPageFetcherOffsetAndLimitSplitIterator = new PageFetcherOffsetAndLimitSplitIterator<>(
                (offset, limit) -> {
                    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();
                    parameters.setRepositoryIds(repository.getId());
                    parameters.setForRootLocale(true);
                    parameters.setDoNotTranslateFilter(false);
                    parameters.setUsedFilter(UsedFilter.USED);
                    parameters.setSkipTextUnitWithPattern(skipTextUnitsWithPattern);
                    parameters.setSkipAssetPathWithPattern(skipAssetsWithPathPattern);
                    parameters.setOffset(offset);
                    parameters.setLimit(batchSize);
                    parameters.setPluralFilter(PluralFilter.SINGULAR);
                    List<TextUnitDTO> search = textUnitSearcher.search(parameters);
                    return search;
                }, DEFAULT_BATCH_SIZE, true);

        return textUnitDTOPageFetcherOffsetAndLimitSplitIterator;
    }

    PageFetcherOffsetAndLimitSplitIterator<TextUnitDTO> getPluralTextUnitIterator(
            Repository repository,
            String skipTextUnitsWithPattern,
            String skipAssetsWithPathPattern) {

        PageFetcherOffsetAndLimitSplitIterator<TextUnitDTO> textUnitDTOPageFetcherOffsetAndLimitSplitIterator = new PageFetcherOffsetAndLimitSplitIterator<>(
                (offset, limit) -> {
                    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();
                    parameters.setRepositoryIds(repository.getId());
                    parameters.setForRootLocale(true);
                    parameters.setDoNotTranslateFilter(false);
                    parameters.setUsedFilter(UsedFilter.USED);
                    parameters.setSkipTextUnitWithPattern(skipTextUnitsWithPattern);
                    parameters.setSkipAssetPathWithPattern(skipAssetsWithPathPattern);
                    parameters.setOffset(offset);
                    parameters.setLimit(batchSize);
                    parameters.setPluralFilter(PluralFilter.PLURAL);
                    parameters.setPluralFormsFiltered(false);
                    List<TextUnitDTO> search = textUnitSearcher.search(parameters);
                    return search;
                }, DEFAULT_BATCH_SIZE, true);

        return textUnitDTOPageFetcherOffsetAndLimitSplitIterator;
    }

    void removeFileForBatchNumbetGreatOrEquals(long intValue, Prefix singular) {
        throw new UnsupportedOperationException();
    }

    SmartlingFile convertToAndroidAndUpload(List<TextUnitDTO> textUnitDTOS,
                                            long batchNumber,
                                            AndroidStringDocumentMapper mapper,
                                            Repository repository,
                                            String projectId,
                                            SmartlingOptions options,
                                            Prefix filePrefix) {

        logger.debug("Convert text units to Android string file for asset number: {}", batchNumber);
        String fileContent;
        try {
            AndroidStringDocumentWriter writer = new AndroidStringDocumentWriter(mapper.readFromSourceTextUnits(textUnitDTOS));
            fileContent = writer.toText();
        } catch (ParserConfigurationException | TransformerException e) {
            logger.error("An error ocurred while converting text units into an Android file content", e);
            throw new RuntimeException(e);
        }

        SmartlingFile file = new SmartlingFile();
        file.setFileName(getOutputSourceFile(batchNumber, repository.getName(), filePrefix.getType()));
        file.setFileContent(fileContent);

        logger.debug("Uplading source file: {} to Smartling", file.getFileName());
        smartlingClient.uploadFile(projectId, file.getFileName(), ANDROID_FILE_TYPE,
                file.getFileContent(), options.getPlaceholderFormat(), options.getCustomPlaceholderFormat());

        return file;
    }

    @Override
    public void pull(Repository repository,
                     String projectId,
                     String pluralSeparator,
                     Map<String, String> localeMapping,
                     String skipTextUnitsWithPattern,
                     String skipAssetsWithPathPattern,
                     List<String> optionList) {

        SmartlingOptions options = SmartlingOptions.parseList(optionList);
        getRepositoryFilesFromProject(repository, projectId).stream()
                .forEach(file -> downloadAndImportLocalizedFiles(file.getFileUri(), repository, projectId, pluralSeparator, localeMapping, options));
    }

    void downloadAndImportLocalizedFiles(String fileUri, Repository repository, String projectId, String pluralSeparator, Map<String, String> localeMapping, SmartlingOptions options) {
        repository.getRepositoryLocales().stream()
                .filter(repositoryLocale -> repositoryLocale.getParentLocale() != null)
                .forEach(repositoryLocale -> {
                    String localeTag = repositoryLocale.getLocale().getBcp47Tag();
                    String smartlingLocale = getSmartlingLocale(localeMapping, localeTag);
                    AndroidStringDocumentMapper mapper = new AndroidStringDocumentMapper(pluralSeparator, localeTag, repository.getName(), assetPathAndTextUnitNameKeys);

                    logger.debug("Download localized file from Smartling for file: {}, Mojito locale: {} and Smartling locale: {}",
                            fileUri, localeTag, smartlingLocale);

                    String fileContent = smartlingClient.downloadPublishedFile(projectId, smartlingLocale, fileUri, false);
                    List<TextUnitDTO> textUnitDTOs;

                    try {
                        textUnitDTOs = mapper.mapToTextUnits(AndroidStringDocumentReader.fromText(fileContent));
                    } catch (ParserConfigurationException | IOException | SAXException e) {
                        String msg = "An error ocurred when processing a pull batch";
                        logger.error(msg, e);
                        throw new RuntimeException(msg, e);
                    }

                    if (!textUnitDTOs.isEmpty() && SmartlingFileUtils.isPluralFile(fileUri) && options.getPluralFixForLocales().contains(localeTag)) {
                        textUnitDTOs = SmartlingPluralFix.fixTextUnits(textUnitDTOs);
                    }

                    logger.debug("Importing text units for locale: {}", smartlingLocale);
                    textUnitBatchImporterService.importTextUnits(textUnitDTOs, false, true);
                });
    }

    @Override
    public void pushTranslations(Repository repository,
                                 String projectId,
                                 String pluralSeparator,
                                 Map<String, String> localeMapping,
                                 String skipTextUnitsWithPattern,
                                 String skipAssetsWithPathPattern,
                                 List<String> optionList) {
    }

    String getSmartlingLocale(Map<String, String> localeMapping, String localeTag) {
        return localeMapping.getOrDefault(localeTag, localeTag);
    }

    // that's not a prefix ...
    private enum Prefix {
        SINGULAR, PLURAL;

        public String getType() {
            return name().toLowerCase();
        }

        public boolean isPlural() {
            return this.equals(PLURAL);
        }
    }

}
