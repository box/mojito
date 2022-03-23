package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.iterators.PageFetcherOffsetAndLimitSplitIterator;
import com.box.l10n.mojito.iterators.Spliterators;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingJsonConverter;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.SmartlingClientException;
import com.box.l10n.mojito.smartling.SmartlingJsonKeys;
import com.box.l10n.mojito.smartling.response.File;
import com.box.l10n.mojito.smartling.response.Items;
import com.box.l10n.mojito.smartling.response.StringInfo;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.isPluralFile;

/**
 * There are limitation with Android format for ICU message format, so we're going to reimplement a sync based on JSON.
 * Project that use JSON must not have plural text units which should be the case if they use message format.
 * <p>
 * {@link ThirdPartyTMSSmartling} will redirect request to this class based on an option
 */
@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSSmartling")
@Component
public class ThirdPartyTMSSmartlingWithJson {

    static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSSmartlingWithJson.class);

    SmartlingClient smartlingClient;
    SmartlingJsonConverter smartlingJsonConverter;
    TextUnitSearcher textUnitSearcher;
    TextUnitBatchImporterService textUnitBatchImporterService;
    SmartlingJsonKeys smartlingJsonKeys;

    int batchSize = 5000;

    public ThirdPartyTMSSmartlingWithJson(SmartlingClient smartlingClient,
                                          SmartlingJsonConverter smartlingJsonConverter,
                                          TextUnitSearcher textUnitSearcher,
                                          TextUnitBatchImporterService textUnitBatchImporterService,
                                          SmartlingJsonKeys smartlingJsonKeys) {
        this.smartlingClient = smartlingClient;
        this.smartlingJsonConverter = smartlingJsonConverter;
        this.textUnitSearcher = textUnitSearcher;
        this.textUnitBatchImporterService = textUnitBatchImporterService;
        this.smartlingJsonKeys = smartlingJsonKeys;
    }

    void push(Repository repository,
              String projectId,
              String pluralSeparator,
              String skipTextUnitsWithPattern,
              String skipAssetsWithPathPattern,
              SmartlingOptions smartlingOptions) {

        logger.info("Pushing Mojito text units for repository: {} to Smartling project: {} using JSON sync", repository.getName(), projectId);

        long partitionCount = Spliterators.partitionStreamWithIndex(getSourceTextUnitIterator(repository, skipTextUnitsWithPattern, skipAssetsWithPathPattern),
                batchSize,
                (textUnitDTOS, index) -> {
                    String fileName = getSourceFileName(repository.getName(), index);
                    String fileContent = smartlingJsonConverter.textUnitDTOsToJsonString(textUnitDTOS, TextUnitDTO::getSource);
                    Mono.fromCallable(() -> smartlingClient.uploadFile(projectId, fileName, "json", fileContent, smartlingOptions.getPlaceholderFormat(), smartlingOptions.getCustomPlaceholderFormat()))
                            .retryWhen(smartlingClient.getRetryConfiguration()
                                    .doBeforeRetry(e -> logger.info(String.format("Retrying failed upload file request for file %s in project %s", fileName, projectId), e.failure())))
                            .doOnError(e -> {
                                String msg = String.format("Upload file request failed for file %s in project %s", fileName, projectId);
                                logger.error(msg, e);
                                throw new SmartlingClientException(msg, e);
                            })
                            .block();
                    return index;
                }).count();

        removeFileForBatchNumberGreaterOrEquals(repository.getName(), projectId, partitionCount);
    }

    void removeFileForBatchNumberGreaterOrEquals(String repositoryName, String projectId, long numberOfBatches) {
        Pattern sourceFilePattern = getSourceFilePattern(repositoryName);
        Items<File> files = getFilesListForProject(projectId);

        files.getItems().stream()
                .filter(file -> {
                    Matcher matcher = sourceFilePattern.matcher(file.getFileUri());
                    return matcher.matches() && Integer.valueOf(matcher.group(1)) > numberOfBatches;
                })
                .peek(file -> logger.debug("removing file: {}", file.getFileUri()))
                .forEach(file -> Mono.fromRunnable(() -> smartlingClient.deleteFile(projectId, file.getFileUri()))
                        .retryWhen(smartlingClient.getRetryConfiguration()
                                .doBeforeRetry(e -> logger.info(String.format("Retrying delete file request for file %s in project %s after failure", file.getFileUri(), projectId), e.failure())))
                        .doOnError(e -> {
                            String msg = String.format("Delete file request failed for file %s in project %s", file.getFileUri(), projectId);
                            logger.error(msg, e);
                            throw new SmartlingClientException(msg, e);
                        })
                        .block());
    }

    void pull(Repository repository,
              String projectId,
              Map<String, String> localeMapping) {

        List<File> repositoryFilesFromProject = getRepositoryFilesFromProject(repository, projectId);

        logger.info("Pull from project: {} into repository: {}", projectId, repository.getName());
        repositoryFilesFromProject.forEach(file -> {
            getRepositoryLocaleWithoutRootStream(repository)
                    .forEach(repositoryLocale -> {
                        String smartlingLocale = getSmartlingLocale(localeMapping, repositoryLocale);
                        String localizedFileContent = getLocalizedFileContent(projectId, file, smartlingLocale, false);

                        ImmutableList<TextUnitDTO> textUnitDTOS = smartlingJsonConverter.jsonStringToTextUnitDTOs(localizedFileContent, TextUnitDTO::setTarget);

                        // When returning translations from Smartling in JSON format with includeOriginalStrings set to
                        // false, untranslated strings get returned as an empty string. To be able to disambiguate
                        // between empty strings and untranslated strings, whenever we encounter that we'll make another
                        // call to Smartling with includeOriginalStrings set to true and compare the results.
                        if (hasEmptyTranslations(textUnitDTOS)) {
                            localizedFileContent = getLocalizedFileContent(projectId, file, smartlingLocale, true);
                            ImmutableList<TextUnitDTO> textUnitDTOSWithOriginalStrings = smartlingJsonConverter.jsonStringToTextUnitDTOs(localizedFileContent, TextUnitDTO::setTarget);
                            textUnitDTOS = getTranslatedUnits(textUnitDTOS, textUnitDTOSWithOriginalStrings);
                        }

                        textUnitDTOS.stream().forEach(t -> {
                            t.setRepositoryName(repository.getName());
                            t.setTargetLocale(repositoryLocale.getLocale().getBcp47Tag());
                        });
                        textUnitBatchImporterService.importTextUnits(textUnitDTOS, false, true);
                    });
        });
    }

    public void pushTranslations(Repository repository,
                                 String projectId,
                                 String pluralSeparator,
                                 Map<String, String> localeMapping,
                                 String skipTextUnitsWithPattern,
                                 String skipAssetsWithPathPattern,
                                 String includeTextUnitsWithPattern,
                                 SmartlingOptions smartlingOptions) {

        logger.info("Push translation from repository: {} into project: {}", repository.getName(), projectId);

        getRepositoryLocaleWithoutRootStream(repository)
                .forEach(repositoryLocale -> {
                    long partitionCount = Spliterators.partitionStreamWithIndex(
                            getTargetTextUnitIterator(repository, repositoryLocale.getLocale().getId(), skipTextUnitsWithPattern, skipAssetsWithPathPattern, includeTextUnitsWithPattern),
                            batchSize,
                            (textUnitDTOS, index) -> {
                                String fileName = getSourceFileName(repository.getName(), index);
                                String fileContent = smartlingJsonConverter.textUnitDTOsToJsonString(textUnitDTOS, TextUnitDTO::getTarget);
                                String smartlingLocale = getSmartlingLocale(localeMapping, repositoryLocale);
                                Mono.fromCallable(() -> smartlingClient.uploadLocalizedFile(projectId, fileName, "json", smartlingLocale, fileContent, null, null))
                                        .retryWhen(smartlingClient.getRetryConfiguration()
                                                .doBeforeRetry(e -> logger.info(String.format("Retrying after failure to upload localized file %s in project %s", fileName, projectId), e.failure())))
                                        .doOnError(e -> {
                                            String msg = String.format("Error uploading localized file %s in project %s", fileName, projectId);
                                            logger.error(msg, e);
                                            throw new SmartlingClientException(msg, e);
                                        })
                                        .block();
                                return index;
                            }
                    ).count();
                    logger.debug("Processed {} partitions", partitionCount);
                });
    }

    PageFetcherOffsetAndLimitSplitIterator<TextUnitDTO> getSourceTextUnitIterator(
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
                    parameters.setLimit(limit);
                    parameters.setPluralFormsFiltered(true);
                    List<TextUnitDTO> search = textUnitSearcher.search(parameters);
                    return search;
                }, batchSize);

        return textUnitDTOPageFetcherOffsetAndLimitSplitIterator;
    }

    PageFetcherOffsetAndLimitSplitIterator<TextUnitDTO> getTargetTextUnitIterator(
            Repository repository,
            Long localeId,
            String skipTextUnitsWithPattern,
            String skipAssetsWithPathPattern,
            String includeTextUnitsWithPattern) {

        PageFetcherOffsetAndLimitSplitIterator<TextUnitDTO> textUnitDTOPageFetcherOffsetAndLimitSplitIterator = new PageFetcherOffsetAndLimitSplitIterator<>(
                (offset, limit) -> {
                    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();
                    parameters.setRepositoryIds(repository.getId());
                    parameters.setLocaleId(localeId);
                    parameters.setDoNotTranslateFilter(false);
                    parameters.setStatusFilter(StatusFilter.TRANSLATED);
                    parameters.setUsedFilter(UsedFilter.USED);
                    parameters.setSkipTextUnitWithPattern(skipTextUnitsWithPattern);
                    parameters.setSkipAssetPathWithPattern(skipAssetsWithPathPattern);
                    parameters.setIncludeTextUnitsWithPattern(includeTextUnitsWithPattern);
                    parameters.setOffset(offset);
                    parameters.setLimit(limit);
                    parameters.setPluralFormsFiltered(true);
                    List<TextUnitDTO> search = textUnitSearcher.search(parameters);
                    return search;
                }, batchSize);

        return textUnitDTOPageFetcherOffsetAndLimitSplitIterator;
    }

    String getSourceFileName(String repositoryName, long batchNumber) {
        return String.format("%s/%05d_source.json", repositoryName, batchNumber);
    }

    Pattern getSourceFilePattern(String repositoryName) {
        return Pattern.compile(repositoryName + "/(\\d+)_source.json");
    }

    String getSmartlingLocale(Map<String, String> localeMapping, RepositoryLocale repositoryLocale) {
        String localeTag = repositoryLocale.getLocale().getBcp47Tag();
        return localeMapping.getOrDefault(localeTag, localeTag);
    }

    Stream<RepositoryLocale> getRepositoryLocaleWithoutRootStream(Repository repository) {
        return repository.getRepositoryLocales().stream()
                .filter(repositoryLocale -> repositoryLocale.getParentLocale() != null);
    }

    List<File> getRepositoryFilesFromProject(Repository repository, String projectId) {
        Pattern filePattern = getSourceFilePattern(repository.getName());
        Items<File> filesList = getFilesListForProject(projectId);

        List<File> files = filesList.getItems().stream()
                .filter(file -> filePattern.matcher(file.getFileUri()).matches())
                .collect(ImmutableList.toImmutableList());
        return files;
    }

    public List<ThirdPartyTextUnit> getThirdPartyTextUnits(Repository repository, String projectId, List<String> optionList) {

        logger.debug("Get third party text units for repository: {} and project id: {}", repository.getId(), projectId);

        SmartlingOptions smartlingOptions = SmartlingOptions.parseList(optionList);

        Pattern filePattern = getSourceFilePattern(repository.getName());
        Items<File> filesList = getFilesListForProject(projectId);

        List<File> files = filesList.getItems().stream()
                .filter(file -> filePattern.matcher(file.getFileUri()).matches())
                .collect(Collectors.toList());

        List<ThirdPartyTextUnit> thirdPartyTextUnits = files.stream().flatMap(file -> {
            Stream<StringInfo> stringInfos = Mono.fromCallable(() -> smartlingClient.getStringInfos(projectId, file.getFileUri()))
                    .retryWhen(smartlingClient.getRetryConfiguration()
                            .doBeforeRetry(e -> logger.info(String.format("Retrying after failure to get string information from Smartling for project %s, file %s", projectId, file.getFileUri()))))
                    .doOnError(e -> {
                        String msg = String.format("Error getting string information for file %s in project %s", file.getFileUri(), projectId);
                        logger.error(msg, e);
                        throw new SmartlingClientException(msg, e);
                    })
                    .blockOptional()
                    .orElseThrow(() -> new SmartlingClientException(String.format("Error getting string information for file %s in project %s", file.getFileUri(), projectId)));

            return stringInfos.map(stringInfo -> {
                logger.debug("hashcode: {}\nvariant: {}\nparsed string: {}",
                        stringInfo.getHashcode(),
                        stringInfo.getStringVariant(),
                        stringInfo.getParsedStringText());

                Preconditions.checkNotNull(stringInfo.getStringVariant(), "Variant must be enabled in Smartling for properly synchronizing");
                SmartlingJsonKeys.Key key = smartlingJsonKeys.parse(stringInfo.getStringVariant());
                ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
                thirdPartyTextUnit.setId(stringInfo.getHashcode());
                thirdPartyTextUnit.setTmTextUnitId(key.getTmTextUnitd());
                thirdPartyTextUnit.setAssetPath(key.getAssetPath());
                thirdPartyTextUnit.setName(key.getTextUnitName());
                thirdPartyTextUnit.setNamePluralPrefix(isPluralFile(file.getFileUri()));

                return thirdPartyTextUnit;
            });
        }).collect(Collectors.toList());

        return thirdPartyTextUnits;
    }

    boolean hasEmptyTranslations(ImmutableList<TextUnitDTO> textUnitDTOS) {
        return textUnitDTOS.stream().anyMatch(TextUnitDTO::hasEmptyTranslation);
    }

    String getLocalizedFileContent(String projectId, File file, String smartlingLocale, boolean includeOriginalStrings) {
        return Mono.fromCallable(() -> smartlingClient.downloadPublishedFile(projectId, smartlingLocale, file.getFileUri(), includeOriginalStrings))
                .retryWhen(smartlingClient.getRetryConfiguration()
                        .doBeforeRetry(e -> logger.info(String.format("Retrying download published file request for locale %s, file %s in project %s", smartlingLocale, file.getFileUri(), projectId), e.failure())))
                .doOnError(e -> {
                    String msg = String.format("Error downloading published file for locale %s, file %s in project %s", smartlingLocale, file.getFileUri(), projectId);
                    logger.error(msg, e);
                    throw new SmartlingClientException(msg, e);
                })
                .blockOptional()
                .orElseThrow(() -> new SmartlingClientException("Error downloading published file from Smartling, file content is not present"));
    }

    /**
     * Returns the list of translated units ready for import based on two lists of TextUnitDTOs that represent
     * untranslated strings in different formats for the same text units.
     *
     * @param textUnitDTOS has untranslated strings as empty strings
     * @param textUnitDTOSWithOriginalStrings has untranslated strings as the original source strings
     */
    ImmutableList<TextUnitDTO> getTranslatedUnits(ImmutableList<TextUnitDTO> textUnitDTOS, ImmutableList<TextUnitDTO> textUnitDTOSWithOriginalStrings) {
        Map<Long, TextUnitDTO> textUnitDTOWithOriginalStringsMap = textUnitDTOSWithOriginalStrings
                .stream()
                .collect(Collectors.toMap(TextUnitDTO::getTmTextUnitId, Function.identity()));

        return textUnitDTOS.stream().filter(textUnitDTO -> {
            TextUnitDTO textUnitDTOWithOriginalString = textUnitDTOWithOriginalStringsMap.get(textUnitDTO.getTmTextUnitId());

            // Skip untranslated strings
            return !(textUnitDTOWithOriginalString == null
                    || textUnitDTO.hasEmptyTranslation() && !textUnitDTOWithOriginalString.hasEmptyTranslation());
        } ).collect(ImmutableList.toImmutableList());
    }

    private Items<File> getFilesListForProject(String projectId) {
        return Mono.fromCallable(() -> smartlingClient.getFiles(projectId))
                .retryWhen(smartlingClient.getRetryConfiguration()
                        .doBeforeRetry(e -> logger.info(String.format("Retrying get files request after failure for project %s", projectId), e.failure())))
                .doOnError(e -> {
                    String msg = String.format("Error getting files for project %s", projectId);
                    logger.error(msg, e);
                    throw new SmartlingClientException(msg, e);
                })
                .blockOptional()
                .orElseThrow(() -> new SmartlingClientException("Error getting project files, file items is not present"));

    }
}
