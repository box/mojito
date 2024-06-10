package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.android.strings.AndroidStringDocument;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentMapper;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentReader;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.thirdparty.phrase.PhraseClient;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.google.common.collect.ImmutableList;
import com.phrase.client.model.Tag;
import com.phrase.client.model.TranslationKey;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSPhrase")
@Component
public class ThirdPartyTMSPhrase implements ThirdPartyTMS {

  static final String TAG_PREFIX = "push_";

  static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSPhrase.class);

  @Autowired TextUnitSearcher textUnitSearcher = new TextUnitSearcher();

  @Autowired TextUnitBatchImporterService textUnitBatchImporterService;

  @Autowired(required = false)
  PhraseClient phraseClient;

  @Autowired RepositoryService repositoryService;

  @Override
  public void removeImage(String projectId, String imageId) {
    throw new UnsupportedOperationException("Remove image is not supported");
  }

  @Override
  public ThirdPartyTMSImage uploadImage(String projectId, String name, byte[] content) {
    throw new UnsupportedOperationException("Upload image is not supported");
  }

  @Override
  public List<ThirdPartyTextUnit> getThirdPartyTextUnits(
      Repository repository, String projectId, List<String> optionList) {

    List<ThirdPartyTextUnit> thirdPartyTextUnits = new ArrayList<>();

    List<TranslationKey> phraseTranslationKeys = phraseClient.getKeys(projectId);

    for (TranslationKey translationKey : phraseTranslationKeys) {

      String[] nameParts = translationKey.getName().split("#@#", 3);

      if (nameParts.length != 3) {
        logger.error(
            "Skipping entry. Name: {} should have 3 parts. Missing part could happen in old project",
            translationKey.getName());
        continue;
      }

      String idSection = nameParts[0];
      if (!idSection.contains(",")) {
        ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
        thirdPartyTextUnit.setAssetPath(nameParts[1]);
        thirdPartyTextUnit.setName(nameParts[2]);
        thirdPartyTextUnit.setId(translationKey.getId());
        thirdPartyTextUnit.setTmTextUnitId(Long.valueOf(idSection));
        thirdPartyTextUnits.add(thirdPartyTextUnit);
      } else {
        List<Long> ids = Arrays.stream(idSection.split(",")).map(Long::valueOf).toList();
        for (Long id : ids) {
          ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
          thirdPartyTextUnit.setAssetPath(nameParts[1]);
          thirdPartyTextUnit.setName(nameParts[2]);
          thirdPartyTextUnit.setId(translationKey.getId());
          thirdPartyTextUnit.setTmTextUnitId(id);
          thirdPartyTextUnits.add(thirdPartyTextUnit);
        }
      }
    }

    return thirdPartyTextUnits;
  }

  @Override
  public void createImageToTextUnitMappings(
      String projectId, List<ThirdPartyImageToTextUnit> thirdPartyImageToTextUnits) {
    throw new UnsupportedOperationException("Create image to text units is not supported");
  }

  @Override
  public void push(
      Repository repository,
      String projectId,
      String pluralSeparator,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      List<String> options) {

    List<TextUnitDTO> search =
        getSourceTextUnitDTOs(repository, skipTextUnitsWithPattern, skipAssetsWithPathPattern);

    String text = getFileContent(pluralSeparator, search, true);

    String tagForUpload = getTagForUpload();
    phraseClient.uploadAndWait(
        projectId,
        repository.getSourceLocale().getBcp47Tag(),
        "xml",
        repository.getName() + "-strings.xml",
        text,
        ImmutableList.of(tagForUpload));

    phraseClient.removeKeysNotTaggedWith(projectId, tagForUpload);

    List<Tag> tagsToDelete =
        phraseClient.listTags(projectId).stream()
            .filter(
                tag ->
                    tag.getName() != null
                        && !tag.getName().equals(tagForUpload)
                        && tag.getName().startsWith(TAG_PREFIX))
            .toList();
    phraseClient.deleteTags(projectId, tagsToDelete);
  }

  private List<TextUnitDTO> getSourceTextUnitDTOs(
      Repository repository, String skipTextUnitsWithPattern, String skipAssetsWithPathPattern) {
    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();

    parameters.setRepositoryIds(repository.getId());
    parameters.setForRootLocale(true);
    parameters.setDoNotTranslateFilter(false);
    parameters.setUsedFilter(UsedFilter.USED);
    parameters.setSkipTextUnitWithPattern(skipTextUnitsWithPattern);
    parameters.setSkipAssetPathWithPattern(skipAssetsWithPathPattern);
    parameters.setPluralFormsFiltered(false);
    parameters.setOrderByTextUnitID(true);

    return textUnitSearcher.search(parameters);
  }

  public static String getTagForUpload() {
    ZonedDateTime zonedDateTime = JSR310Migration.dateTimeNowInUTC();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSS");
    return ("%s%s_%s")
        .formatted(
            TAG_PREFIX,
            formatter.format(zonedDateTime),
            Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1000));
  }

  @Override
  public PollableFuture<Void> pull(
      Repository repository,
      String projectId,
      String pluralSeparator,
      Map<String, String> localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      List<String> optionList,
      String schedulerName,
      PollableTask currentTask) {

    Set<RepositoryLocale> repositoryLocalesWithoutRootLocale =
        repositoryService.getRepositoryLocalesWithoutRootLocale(repository);

    for (RepositoryLocale repositoryLocale : repositoryLocalesWithoutRootLocale) {
      String localeTag = repositoryLocale.getLocale().getBcp47Tag();
      logger.info("Downloading locale: {} from Phrase", localeTag);
      String fileContent = phraseClient.localeDownload(projectId, localeTag, "xml");

      AndroidStringDocumentMapper mapper =
          new AndroidStringDocumentMapper(
              pluralSeparator, null, localeTag, repository.getName(), true);

      List<TextUnitDTO> textUnitDTOS =
          mapper.mapToTextUnits(AndroidStringDocumentReader.fromText(fileContent));

      textUnitBatchImporterService.importTextUnits(textUnitDTOS, false, true);
    }

    return null;
  }

  @Override
  public void pushTranslations(
      Repository repository,
      String projectId,
      String pluralSeparator,
      Map<String, String> localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitsWithPattern,
      List<String> optionList) {

    Set<RepositoryLocale> repositoryLocalesWithoutRootLocale =
        repositoryService.getRepositoryLocalesWithoutRootLocale(repository);

    for (RepositoryLocale repositoryLocale : repositoryLocalesWithoutRootLocale) {
      List<TextUnitDTO> textUnitDTOS =
          getTextUnitDTOSForLocale(
              repository,
              skipTextUnitsWithPattern,
              skipAssetsWithPathPattern,
              includeTextUnitsWithPattern,
              repositoryLocale);

      String fileContent = getFileContent(pluralSeparator, textUnitDTOS, false);

      phraseClient.uploadCreateFile(
          projectId,
          repositoryLocale.getLocale().getBcp47Tag(),
          "xml",
          repository.getName() + "-strings.xml",
          fileContent,
          null);
    }
  }

  private List<TextUnitDTO> getTextUnitDTOSForLocale(
      Repository repository,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitsWithPattern,
      RepositoryLocale repositoryLocale) {
    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();
    parameters.setRepositoryIds(repository.getId());
    parameters.setLocaleId(repositoryLocale.getLocale().getId());
    parameters.setDoNotTranslateFilter(false);
    parameters.setStatusFilter(StatusFilter.TRANSLATED);
    parameters.setUsedFilter(UsedFilter.USED);
    parameters.setSkipTextUnitWithPattern(skipTextUnitsWithPattern);
    parameters.setSkipAssetPathWithPattern(skipAssetsWithPathPattern);
    parameters.setIncludeTextUnitsWithPattern(includeTextUnitsWithPattern);
    parameters.setPluralFormsFiltered(true);
    return textUnitSearcher.search(parameters);
  }

  private static String getFileContent(
      String pluralSeparator, List<TextUnitDTO> textUnitDTOS, boolean useSource) {

    AndroidStringDocumentMapper androidStringDocumentMapper =
        new AndroidStringDocumentMapper(pluralSeparator, null, null, null, true);

    AndroidStringDocument androidStringDocument =
        androidStringDocumentMapper.readFromTextUnits(textUnitDTOS, useSource);

    return new AndroidStringDocumentWriter(androidStringDocument).toText();
  }

  @Override
  public void pullSource(
      Repository repository,
      String projectId,
      List<String> optionList,
      Map<String, String> localeMapping) {
    throw new UnsupportedOperationException("Pull source is not supported");
  }
}
