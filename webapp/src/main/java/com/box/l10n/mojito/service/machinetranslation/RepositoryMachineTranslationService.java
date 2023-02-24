package com.box.l10n.mojito.service.machinetranslation;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RepositoryMachineTranslationService {

  static Logger logger = LoggerFactory.getLogger(RepositoryMachineTranslationService.class);

  @Autowired MachineTranslationService machineTranslationService;
  @Autowired RepositoryRepository repositoryRepository;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired TextUnitBatchImporterService textUnitBatchImporterService;

  /**
   * Machine translate untranslated strings in a repository.
   *
   * <p>one locale at the time, get untranslated strings, send them to the machine translation
   * engine and then import the translations in the repository
   *
   * @param repositoryName
   * @param targetBcp47Tags
   * @param sourceTextMaxCountPerLocale
   * @return
   */
  @Pollable(async = true, message = "Start machine translating repository")
  public PollableFuture<Void> translateRepository(
      String repositoryName, List<String> targetBcp47Tags, int sourceTextMaxCountPerLocale) {

    logger.info(
        "Start Machine Translating repository: {} and target locales: {}",
        repositoryName,
        targetBcp47Tags);
    Repository repository = repositoryRepository.findByName(repositoryName);

    if (repository == null) {
      throw new RuntimeException("Must have a valid repository name to machine translate");
    }

    for (String targetBcp47Tag : targetBcp47Tags) {
      final TextUnitSearcherParameters searchParameters = new TextUnitSearcherParameters();
      searchParameters.setRepositoryIds(repository.getId());
      searchParameters.setStatusFilter(StatusFilter.UNTRANSLATED);
      searchParameters.setUsedFilter(UsedFilter.USED);
      searchParameters.setLocaleTags(Arrays.asList(targetBcp47Tag));

      final List<TextUnitDTO> textUnitDTOS = textUnitSearcher.search(searchParameters);

      final List<String> sourceTexts =
          textUnitDTOS.stream().map(TextUnitDTO::getSource).distinct().collect(Collectors.toList());

      logger.info("Number of text unit to machine translate: {}", sourceTexts.size());

      if (sourceTexts.size() > sourceTextMaxCountPerLocale) {
        throw new RuntimeException("Too many source text to MT, do not proceed");
      }

      Iterables.partition(sourceTexts, 1000)
          .forEach(
              sourceTextBatch -> {
                final TranslationsResponseDTO translationsResponseDTO =
                    machineTranslationService.getTranslations(
                        sourceTexts,
                        repository.getSourceLocale().getBcp47Tag(),
                        Arrays.asList(targetBcp47Tag),
                        false,
                        true,
                        null,
                        null);

                final Map<String, List<TextUnitDTO>> sourceToTextUnitDTOs =
                    textUnitDTOS.stream().collect(Collectors.groupingBy(TextUnitDTO::getSource));

                final List<TextUnitDTO> machineTranslatedTextUnitDTOs =
                    translationsResponseDTO.getTextUnitTranslations().stream()
                        .flatMap(
                            textUnitTranslationGroupDTO -> {
                              final String sourceText = textUnitTranslationGroupDTO.getSourceText();

                              final Stream<TextUnitDTO> textUnitDTOStreamFor1Translation;

                              if (!textUnitTranslationGroupDTO.getTranslations().isEmpty()) {
                                // since we're doing 1 locale we just read the first entry
                                final TranslationDTO translationDTO =
                                    textUnitTranslationGroupDTO.getTranslations().get(0);

                                textUnitDTOStreamFor1Translation =
                                    sourceToTextUnitDTOs
                                        .getOrDefault(sourceText, Collections.emptyList()).stream()
                                        .map(
                                            textUnitDTO -> {
                                              textUnitDTO.setTarget(translationDTO.getText());
                                              return textUnitDTO;
                                            });
                              } else {
                                logger.error(
                                    "There must be a translation available but if not, just skip");
                                textUnitDTOStreamFor1Translation = Stream.empty();
                              }

                              return textUnitDTOStreamFor1Translation;
                            })
                        .collect(Collectors.toList());

                textUnitBatchImporterService.importTextUnits(
                    machineTranslatedTextUnitDTOs, false, true);
              });
    }

    return new PollableFutureTaskResult<>();
  }
}
