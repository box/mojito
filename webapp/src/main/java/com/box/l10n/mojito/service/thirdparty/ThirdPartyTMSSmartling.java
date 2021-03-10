package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.android.strings.AndroidStringDocumentMapper;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentReader;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFile;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingPluralFix;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingResultProcessor;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.SearchType;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.MANY;
import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.getOutputSourceFile;
import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.getOutputTargetFile;
import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.isPluralFile;
import static com.google.common.collect.Streams.mapWithIndex;

/**
 * tmTextUnitId are not preserved in Smartling plural localized files so we have to import based on name which
 * causes some challenges when there are ambiguities (eg. same name different comment).
 *
 * In singular file, the id is preserved hence used during import.
 *
 * Smartling accept android files with entries where the name is dupplicated. It maps to a single text unit in Smartling.
 * Both entries get the same translation in the localized files.
 *
 * There is no constrain in mojito database that insure the plural text units are valid. This can cause issue with
 * the current implementation. we group by plural form other and the comment.
 */
@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSSmartling")
@Component
public class ThirdPartyTMSSmartling implements ThirdPartyTMS {

    static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSSmartling.class);

    /*
    Devisible by 6 so that the plurals won't be broken up between files
    We force pull all 6 forms regardless of language so that the files
    contain the same keys and can be properly connected to their source
    file in smartling.
    */
    private static final int DEFAULT_BATCH_SIZE = 5004;
    private static final String LOCALE_EN = "en";

    private final SmartlingClient smartlingClient;
    private final AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys;
    private final TextUnitSearcher textUnitSearcher;
    private final TextUnitBatchImporterService textUnitBatchImporterService;
    private final SmartlingResultProcessor resultProcessor;
    private final Integer batchSize;
    private final ThirdPartyTMSSmartlingWithJson thirdPartyTMSSmartlingWithJson;

    @Autowired
    public ThirdPartyTMSSmartling(SmartlingClient smartlingClient,
                                  TextUnitSearcher textUnitSearcher,
                                  AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys,
                                  TextUnitBatchImporterService textUnitBatchImporterService,
                                  SmartlingResultProcessor resultProcessor,
                                  ThirdPartyTMSSmartlingWithJson thirdPartyTMSSmartlingWithJson) {
        this(smartlingClient, textUnitSearcher, assetPathAndTextUnitNameKeys,
                textUnitBatchImporterService, resultProcessor, thirdPartyTMSSmartlingWithJson, DEFAULT_BATCH_SIZE);
    }

    public ThirdPartyTMSSmartling(SmartlingClient smartlingClient,
                                  TextUnitSearcher textUnitSearcher,
                                  AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys,
                                  TextUnitBatchImporterService textUnitBatchImporterService,
                                  SmartlingResultProcessor resultProcessor,
                                  ThirdPartyTMSSmartlingWithJson thirdPartyTMSSmartlingWithJson,
                                  int batchSize) {
        this.smartlingClient = smartlingClient;
        this.assetPathAndTextUnitNameKeys = assetPathAndTextUnitNameKeys;
        this.textUnitBatchImporterService = textUnitBatchImporterService;
        this.textUnitSearcher = textUnitSearcher;
        this.resultProcessor = resultProcessor;
        this.batchSize = batchSize < 1 ? DEFAULT_BATCH_SIZE : batchSize;
        this.thirdPartyTMSSmartlingWithJson = thirdPartyTMSSmartlingWithJson;
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

        Pattern filePattern = SmartlingFileUtils.getFilePattern(repository.getName());

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

    @Override
    public void push(Repository repository,
                     String projectId,
                     String pluralSeparator,
                     String skipTextUnitsWithPattern,
                     String skipAssetsWithPathPattern,
                     List<String> optionList) {

        SmartlingOptions options = SmartlingOptions.parseList(optionList);

        if (options.isJsonSync()) {
            thirdPartyTMSSmartlingWithJson.push(repository, projectId, pluralSeparator, skipTextUnitsWithPattern, skipAssetsWithPathPattern, options);
        }

        AndroidStringDocumentMapper mapper = new AndroidStringDocumentMapper(pluralSeparator, null);

        Stream<SmartlingFile> singularFiles = mapWithIndex(
                partitionSingulars(repository.getId(), LOCALE_EN, skipTextUnitsWithPattern, skipAssetsWithPathPattern),
                (batch, index) -> processPushBatch(batch, index, mapper, repository, projectId, options, Prefix.SINGULAR));

        Stream<SmartlingFile> pluralFiles = mapWithIndex(
                partitionPlurals(repository.getId(), LOCALE_EN, skipTextUnitsWithPattern, skipAssetsWithPathPattern),
                (batch, index) -> processPushBatch(batch, index, mapper, repository, projectId, options, Prefix.PLURAL));

        List<SmartlingFile> result = Stream.concat(singularFiles, pluralFiles).collect(Collectors.toList());
        resultProcessor.processPush(result, options);
    }

    private SmartlingFile processPushBatch(List<TextUnitDTO> result,
                                           long batchNumber,
                                           AndroidStringDocumentMapper mapper,
                                           Repository repository,
                                           String projectId,
                                           SmartlingOptions options,
                                           Prefix filePrefix) {

        logger.debug("Convert text units to AndroidString for asset number: {}", batchNumber);
        SmartlingFile file = new SmartlingFile();
        file.setFileName(getOutputSourceFile(batchNumber, repository.getName(), filePrefix.getType()));

        try {

            logger.debug("Save source file to: {}", file.getFileName());
            AndroidStringDocumentWriter writer = new AndroidStringDocumentWriter(mapper.readFromSourceTextUnits(result));
            file.setFileContent(writer.toText());

        } catch (ParserConfigurationException | TransformerException e) {
            logger.error("An error ocurred when processing a push batch", e);
            throw new RuntimeException(e);
        }

        if (!options.isDryRun()) {
            smartlingClient.uploadFile(projectId, file.getFileName(), "android",
                    file.getFileContent(), options.getPlaceholderFormat(), options.getCustomPlaceholderFormat());
        }

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

        if (options.isJsonSync()) {
            thirdPartyTMSSmartlingWithJson.pull(repository, projectId, pluralSeparator, localeMapping, skipTextUnitsWithPattern, skipAssetsWithPathPattern, options);
        }

        Long singulars = singularCount(repository.getId(), LOCALE_EN, skipTextUnitsWithPattern, skipAssetsWithPathPattern);
        LongStream.range(0, batchesFor(singulars))
                  .forEach(num -> processPullBatch(num, pluralSeparator, repository, projectId, options, localeMapping, Prefix.SINGULAR));

        Long plurals = pluralCount(repository.getId(), LOCALE_EN, skipTextUnitsWithPattern, skipAssetsWithPathPattern);
        LongStream.range(0, batchesFor(plurals))
                  .forEach(num -> processPullBatch(num, pluralSeparator, repository, projectId, options, localeMapping, Prefix.PLURAL));
    }

    private void processPullBatch(Long batchNumber,
                                  String pluralSeparator,
                                  Repository repository,
                                  String projectId,
                                  SmartlingOptions options,
                                  Map<String, String> localeMapping,
                                  Prefix filePrefix) {

        AndroidStringDocumentMapper mapper;

        for (RepositoryLocale locale : repository.getRepositoryLocales()) {

            if (locale.getParentLocale() == null){
                continue;
            }

            String localeTag = locale.getLocale().getBcp47Tag();
            String smartlingLocale = getSmartlingLocale(localeMapping, localeTag);
            String fileName = getOutputSourceFile(batchNumber, repository.getName(), filePrefix.getType());
            mapper = new AndroidStringDocumentMapper(pluralSeparator, null, localeTag, repository.getName());

            logger.debug("Download localized file from Smartling for file: {}, Mojito locale: {} and Smartling locale: {}",
                    fileName, localeTag, smartlingLocale);
            String fileContent = smartlingClient.downloadPublishedFile(projectId, smartlingLocale, fileName, false);
            List<TextUnitDTO> textUnits;

            try {
                textUnits = mapper.mapToTextUnits(AndroidStringDocumentReader.fromText(fileContent));
            } catch (ParserConfigurationException | IOException | SAXException e) {
                String msg = "An error ocurred when processing a pull batch";
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }

            if (!textUnits.isEmpty() && filePrefix.isPlural() && options.getPluralFixForLocales().contains(localeTag)){
                textUnits = SmartlingPluralFix.fixTextUnits(textUnits);
            }

            if (!options.isDryRun()) {
                logger.debug("Importing text units for locale: {}", smartlingLocale);
                textUnitBatchImporterService.importTextUnits(textUnits, false, true);
            }
        }
    }

    @Override
    public void pushTranslations(Repository repository,
                                 String projectId,
                                 String pluralSeparator,
                                 Map<String, String> localeMapping,
                                 String skipTextUnitsWithPattern,
                                 String skipAssetsWithPathPattern,
                                 List<String> optionList) {

        SmartlingOptions options = SmartlingOptions.parseList(optionList);
        if (options.isJsonSync()) {
            thirdPartyTMSSmartlingWithJson.pushTranslations(repository, projectId, pluralSeparator, localeMapping, skipTextUnitsWithPattern, skipAssetsWithPathPattern, options);
        }

        List<SmartlingFile> result;

        AndroidStringDocumentMapper mapper = new AndroidStringDocumentMapper(pluralSeparator, null);

        result = repository.getRepositoryLocales()
                .stream()
                .map(l -> l.getLocale().getBcp47Tag())
                .filter(localeTag -> !localeTag.equalsIgnoreCase(repository.getSourceLocale().getBcp47Tag()))
                .flatMap(localeTag -> Stream.concat(
                        mapWithIndex(partitionSingulars(repository.getId(), localeTag,
                                skipTextUnitsWithPattern, skipAssetsWithPathPattern),
                                (list, batch) -> processTranslationBatch(list, batch, localeTag,
                                        mapper, repository, projectId, options, localeMapping, Prefix.SINGULAR)),
                        mapWithIndex(partitionPlurals(repository.getId(), localeTag,
                                skipTextUnitsWithPattern, skipAssetsWithPathPattern, options.getPluralFixForLocales()),
                                (list, batch) -> processTranslationBatch(list, batch, localeTag,
                                        mapper, repository, projectId, options, localeMapping, Prefix.PLURAL))
                ))
                .collect(Collectors.toList());

        resultProcessor.processPushTranslations(result, options);
    }

    private SmartlingFile processTranslationBatch(List<TextUnitDTO> batch,
                                                  Long batchNumber,
                                                  String localeTag,
                                                  AndroidStringDocumentMapper mapper,
                                                  Repository repository,
                                                  String projectId,
                                                  SmartlingOptions options,
                                                  Map<String, String> localeMapping,
                                                  Prefix filePrefix) {

        logger.debug("Process {} batch: {}", localeTag, batchNumber);
        logger.debug("Convert text units to AndroidString for asset number: {}", batchNumber);
        String sourceFilename = getOutputSourceFile(batchNumber, repository.getName(), filePrefix.getType());
        String targetFilename = getOutputTargetFile(batchNumber, repository.getName(), filePrefix.getType(), localeTag);
        SmartlingFile file = new SmartlingFile();
        file.setFileName(targetFilename);

        try {

            logger.debug("Save target file to: {}", file.getFileName());
            AndroidStringDocumentWriter writer = new AndroidStringDocumentWriter(mapper.readFromTargetTextUnits(batch));
            file.setFileContent(writer.toText());

        } catch (ParserConfigurationException | TransformerException e) {
            logger.error("An error ocurred when processing a push_translations batch", e);
            throw new RuntimeException(e);
        }

        if (!options.isDryRun()) {
            logger.debug("Push Android file to Smartling project: {} and locale: {}", projectId, localeTag);
            smartlingClient.uploadLocalizedFile(projectId, sourceFilename, "android",
                    getSmartlingLocale(localeMapping, localeTag), file.getFileContent(),
                    options.getPlaceholderFormat(), options.getCustomPlaceholderFormat());
        }

        return file;
    }

    private Stream<List<TextUnitDTO>> partitionSingulars(Long repositoryId,
                                                         String localeTag,
                                                         String skipTextUnitsWithPattern,
                                                         String skipAssetsWithPathPattern) {
        return partitionedStream(baseParams(repositoryId, localeTag, skipTextUnitsWithPattern,
                skipAssetsWithPathPattern, true, true, null), textUnitSearcher::search);
    }

    private Stream<List<TextUnitDTO>> partitionPlurals(Long repositoryId,
                                                       String localeTag,
                                                       String skipTextUnitsWithPattern,
                                                       String skipAssetsWithPathPattern) {
        return partitionedStream(baseParams(repositoryId, localeTag, skipTextUnitsWithPattern,
                skipAssetsWithPathPattern, false, false, "%"), textUnitSearcher::search);
    }

    private Stream<List<TextUnitDTO>> partitionPlurals(Long repositoryId,
                                                       String localeTag,
                                                       String skipTextUnitsWithPattern,
                                                       String skipAssetsWithPathPattern,
                                                       Set<String> pluralFixForLocales) {

        Function<TextUnitSearcherParameters, List<TextUnitDTO>> searchFunction = textUnitSearcher::search;

        if (pluralFixForLocales.contains(localeTag)){
            searchFunction = searchFunction.andThen(textUnits -> textUnits.stream()
                    .filter(tu -> !MANY.toString().equalsIgnoreCase(tu.getPluralForm()))
                    .collect(Collectors.toList()));
        }

        return partitionedStream(baseParams(repositoryId, localeTag, skipTextUnitsWithPattern,
                skipAssetsWithPathPattern, false, false, "%"), searchFunction);
    }

    private Stream<List<TextUnitDTO>> partitionedStream(TextUnitSearcherParameters params,
                                                        Function<TextUnitSearcherParameters, List<TextUnitDTO>> function){
        return StreamSupport.stream(
                Iterables.partition(function.apply(params), batchSize).spliterator(),
                false);
    }

    private Long singularCount(Long repositoryId,
                               String localeTag,
                               String skipTextUnitsWithPattern,
                               String skipAssetsWithPathPattern) {
        return textUnitSearcher.countTextUnitAndWordCount(baseParams(repositoryId, localeTag,
                skipTextUnitsWithPattern, skipAssetsWithPathPattern, true, true, null)).getTextUnitCount();
    }

    private Long pluralCount(Long repositoryId,
                             String localeTag,
                             String skipTextUnitsWithPattern,
                             String skipAssetsWithPathPattern) {
        return textUnitSearcher.countTextUnitAndWordCount(baseParams(repositoryId, localeTag,
                skipTextUnitsWithPattern, skipAssetsWithPathPattern, false, false, "%")).getTextUnitCount();
    }

    private TextUnitSearcherParameters baseParams(Long repositoryId,
                                                  String localeTag,
                                                  String skipTextUnitsWithPattern,
                                                  String skipAssetsWithPathPattern,
                                                  boolean pluralFormsFiltered,
                                                  boolean pluralFormsExcluded,
                                                  String pluralFormOther) {
        TextUnitSearcherParameters result = new TextUnitSearcherParameters();
        result.setRepositoryIds(repositoryId);
        result.setLocaleTags(ImmutableList.of(localeTag));
        result.setRootLocaleExcluded(false);
        result.setDoNotTranslateFilter(false);
        result.setSearchType(SearchType.ILIKE);
        result.setStatusFilter(StatusFilter.TRANSLATED);
        result.setUsedFilter(UsedFilter.USED);
        result.setPluralFormsFiltered(pluralFormsFiltered);
        result.setPluralFormsExcluded(pluralFormsExcluded);
        result.setSkipTextUnitWithPattern(skipTextUnitsWithPattern);
        result.setSkipAssetPathWithPattern(skipAssetsWithPathPattern);

        if (!Strings.isNullOrEmpty(pluralFormOther)) {
            result.setPluralFormOther(pluralFormOther);
        }

        return result;
    }

    /**
     * Calculates the number of batches required to process totalUnits,
     * considering the batchSize configured at the instance level. E.g: If
     * our batch size is 10, and we have 123 units, this function returns 13,
     * as we need 13 batches of 10 to process 123 units.
     *
     * @param totalUnits Total units to process
     * @return The amount of batches required to process totalUnits
     */
    long batchesFor(long totalUnits) {
        return totalUnits / batchSize + ((totalUnits % batchSize == 0) ? 0 : 1);
    }

    private String getSmartlingLocale(Map<String, String> localeMapping, String localeTag) {
        return localeMapping.getOrDefault(localeTag, localeTag);
    }

    private enum Prefix {
        SINGULAR, PLURAL;

        public String getType(){
            return name().toLowerCase();
        }

        public boolean isPlural(){
            return this.equals(PLURAL);
        }
    }

}
