package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.iterators.PageFetcherOffsetAndLimitSplitIterator;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingJsonConverter;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.box.l10n.mojito.smartling.AssetPathAndTextUnitNameKeys;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.response.File;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * There are limitation with Android format for ICU message format, so we're going to reimplement a sync based on JSON.
 * Project that use JSON must not have plural text units which should be the case if they use message format.
 * <p>
 * {@link ThirdPartyTMSSmartling} will redirect request to this class based on an option
 */
@Component
public class ThirdPartyTMSSmartlingWithJson {

    static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSSmartlingWithJson.class);

    SmartlingClient smartlingClient;
    SmartlingJsonConverter smartlingJsonConverter;
    TextUnitSearcher textUnitSearcher;
    TextUnitBatchImporterService textUnitBatchImporterService;

    int batchSize = 5000;

    public ThirdPartyTMSSmartlingWithJson(SmartlingClient smartlingClient,
                                          SmartlingJsonConverter smartlingJsonConverter,
                                          TextUnitSearcher textUnitSearcher,
                                          TextUnitBatchImporterService textUnitBatchImporterService) {
        this.smartlingClient = smartlingClient;
        this.smartlingJsonConverter = smartlingJsonConverter;
        this.textUnitSearcher = textUnitSearcher;
        this.textUnitBatchImporterService = textUnitBatchImporterService;
    }

    void push(Repository repository,
              String projectId,
              String pluralSeparator,
              String skipTextUnitsWithPattern,
              String skipAssetsWithPathPattern,
              SmartlingOptions smartlingOptions) {

        logger.info("Pushing Mojito text units for repository: {} to Smartling project: {} using JSON sync", repository.getName(), projectId);

        ImmutableList<TextUnitDTO> collect = StreamSupport.stream(getSourceTextUnitIterator(repository, skipTextUnitsWithPattern, skipAssetsWithPathPattern), false).collect(ImmutableList.toImmutableList());
        Iterable<List<TextUnitDTO>> partition = Iterables.partition(collect, batchSize);

        AtomicInteger numberOfBatches = new AtomicInteger();
        partition.forEach(textUnitDTOS -> {
            String fileName = getSourceFileName(repository.getName(), numberOfBatches.getAndIncrement());
            String fileContent = smartlingJsonConverter.textUnitDTOsToJsonString(textUnitDTOS, TextUnitDTO::getSource);
            smartlingClient.uploadFile(projectId, fileName, "json", fileContent, smartlingOptions.getPlaceholderFormat(), smartlingOptions.getCustomPlaceholderFormat());
        });

        removeFileForBatchNumberGreaterOrEquals(repository.getName(), projectId, numberOfBatches.get());
    }

    void removeFileForBatchNumberGreaterOrEquals(String repositoryName, String projectId, long numberOfBatches) {
        Pattern sourceFilePattern = getSourceFilePattern(repositoryName);
        smartlingClient.getFiles(projectId).getItems().stream()
                .filter(file -> {
                    Matcher matcher = sourceFilePattern.matcher(file.getFileUri());
                    return matcher.matches() && Integer.valueOf(matcher.group(1)) > numberOfBatches;
                })
                .peek(file -> logger.debug("removing file: {}", file.getFileUri()))
                .forEach(file -> smartlingClient.deleteFile(projectId, file.getFileUri()));
    }

    void pull(Repository repository,
              String projectId,
              String pluralSeparator,
              Map<String, String> localeMapping,
              String skipTextUnitsWithPattern,
              String skipAssetsWithPathPattern,
              SmartlingOptions smartlingOptions) {

        List<File> repositoryFilesFromProject = getRepositoryFilesFromProject(repository, projectId);

        logger.info("Pull from project: {} into repository: {}", projectId, repository.getName());
        repositoryFilesFromProject.forEach(file -> {
            getRepositoryLocaleWithoutRootStream(repository)
                    .forEach(repositoryLocale -> {
                        String smartlingLocale = getSmartlingLocale(localeMapping, repositoryLocale);
                        String localizedFileContent = smartlingClient.downloadPublishedFile(projectId, smartlingLocale, file.getFileUri(), false);
                        ImmutableList<TextUnitDTO> textUnitDTOS = smartlingJsonConverter.jsonStringToTextUnitDTOs(localizedFileContent, TextUnitDTO::setTarget);
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
                                 SmartlingOptions smartlingOptions) {

        logger.info("Push translation from repository: {} into project: {}", repository.getName(), projectId);
        getRepositoryLocaleWithoutRootStream(repository)
                .forEach(repositoryLocale -> {
                    ImmutableList<TextUnitDTO> collect = StreamSupport.stream(getTargetTextUnitIterator(repository, repositoryLocale.getLocale().getId(), skipTextUnitsWithPattern, skipAssetsWithPathPattern), false).collect(ImmutableList.toImmutableList());
                    ;
                    Iterable<List<TextUnitDTO>> partition = Iterables.partition(collect, batchSize);

                    AtomicInteger numberOfBatches = new AtomicInteger();
                    partition.forEach(textUnitDTOS -> {
                        String fileName = getSourceFileName(repository.getName(), numberOfBatches.getAndIncrement());
                        String fileContent = smartlingJsonConverter.textUnitDTOsToJsonString(textUnitDTOS, TextUnitDTO::getTarget);
                        String smartlingLocale = getSmartlingLocale(localeMapping, repositoryLocale);
                        smartlingClient.uploadLocalizedFile(projectId, fileName, "json", smartlingLocale, fileContent, smartlingOptions.getPlaceholderFormat(), smartlingOptions.getCustomPlaceholderFormat());
                    });
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
            String skipAssetsWithPathPattern) {

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
        List<File> files = smartlingClient.getFiles(projectId).getItems().stream()
                .filter(file -> filePattern.matcher(file.getFileUri()).matches())
                .collect(ImmutableList.toImmutableList());
        return files;
    }
}
