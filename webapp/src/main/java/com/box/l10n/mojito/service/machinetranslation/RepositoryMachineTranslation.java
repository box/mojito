package com.box.l10n.mojito.service.machinetranslation;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
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
public class RepositoryMachineTranslation {

  static Logger logger = LoggerFactory.getLogger(RepositoryMachineTranslation.class);

  @Autowired MachineTranslationService machineTranslationService;
  @Autowired RepositoryRepository repositoryRepository;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired TextUnitBatchImporterService textUnitBatchImporterService;

  public void translateRepository(String repositoryName, String targetLocale) {

    logger.info(
        "Start Machine Translating repository: {} and target locale: {}",
        repositoryName,
        targetLocale);
    Repository repository = repositoryRepository.findByName(repositoryName);

    if (repository == null) {
      throw new RuntimeException("Must have a valid repository name to machine translate");
    }

    final TextUnitSearcherParameters searchParameters = new TextUnitSearcherParameters();
    searchParameters.setRepositoryIds(repository.getId());
    searchParameters.setStatusFilter(StatusFilter.UNTRANSLATED);
    searchParameters.setUsedFilter(UsedFilter.USED);
    searchParameters.setLocaleTags(Arrays.asList(targetLocale));

    final List<TextUnitDTO> textUnitDTOS = textUnitSearcher.search(searchParameters);

    final List<String> sourceTexts =
        textUnitDTOS.stream().map(TextUnitDTO::getSource).collect(Collectors.toList());

    final TranslationsResponseDTO translationsResponseDTO =
        machineTranslationService.getTranslations(
            sourceTexts,
            repository.getSourceLocale().getBcp47Tag(),
            Arrays.asList(targetLocale),
            false,
            true,
            null,
            null);

    final Map<String, List<TextUnitDTO>> sourceToTextUnitDTOs =
        textUnitDTOS.stream().collect(Collectors.groupingBy(TextUnitDTO::getSource));

    logger.info("Number of text unit to machine translate: {}", sourceToTextUnitDTOs.size());

    if (sourceToTextUnitDTOs.size() > 300) {
      throw new RuntimeException("Too many text unit, fail safe for now");
    }

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
                        sourceToTextUnitDTOs.getOrDefault(sourceText, Collections.emptyList())
                            .stream()
                            .map(
                                textUnitDTO -> {
                                  textUnitDTO.setTarget(translationDTO.getText());
                                  return textUnitDTO;
                                });
                  } else {
                    logger.error("There must be a translation available but if not, just skip");
                    textUnitDTOStreamFor1Translation = Stream.empty();
                  }

                  return textUnitDTOStreamFor1Translation;
                })
            .collect(Collectors.toList());

    textUnitBatchImporterService.importTextUnits(machineTranslatedTextUnitDTOs, false, true);
  }
}
