package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetIntegrityChecker;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetintegritychecker.AssetIntegrityCheckerRepository;
import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class IntegrityCheckerFactory {

  @Autowired AssetIntegrityCheckerRepository assetIntegrityCheckerRepository;

  @Autowired ApplicationContext applicationContext;

  List<DocumentIntegrityChecker> documentIntegrityCheckers = new ArrayList<>();

  @PostConstruct
  private void initAvailableDocumentCheckers() {
    Iterable<DocumentIntegrityChecker> documentIntegrityCheckersIterable =
        applicationContext.getBeansOfType(DocumentIntegrityChecker.class).values();
    documentIntegrityCheckers = Lists.newArrayList(documentIntegrityCheckersIterable);
  }

  /**
   * @param documentExtension The file extension of the document to check
   * @return A list of {@link DocumentIntegrityChecker} supporting the given document extension
   */
  public List<DocumentIntegrityChecker> getDocumentCheckers(String documentExtension) {

    List<DocumentIntegrityChecker> supportedCheckers = new ArrayList<>();

    for (DocumentIntegrityChecker documentIntegrityChecker : documentIntegrityCheckers) {
      if (documentIntegrityChecker.supportsExtension(documentExtension)) {
        supportedCheckers.add(documentIntegrityChecker);
      }
    }

    return supportedCheckers;
  }

  /**
   * Builds the text-unit checkers that apply to {@code asset}.
   *
   * <p>Resolution is a set union of type-owned and repository-owned checker types whose {@code
   * assetExtension} equals {@link FilenameUtils#getExtension(String)} of the asset path. The same
   * {@link IntegrityCheckerType} configured on both the type and the repository is instantiated
   * once. An untyped repository contributes no type checkers. The type rows are loaded by
   * repository id so callers do not need to initialize the repository's lazy {@code repoType}
   * association.
   *
   * @param asset asset whose repository and path extension select the checkers
   * @return one {@link TextUnitIntegrityChecker} instance per distinct matching checker type; empty
   *     when neither the repository nor its type has a checker for the extension
   * @throws IntegrityCheckerInstantiationException if unable to create an instance of the integrity
   *     checker
   */
  public Set<TextUnitIntegrityChecker> getTextUnitCheckers(Asset asset) {

    Repository repository = asset.getRepository();
    String assetExtension = FilenameUtils.getExtension(asset.getPath());

    Set<AssetIntegrityChecker> assetIntegrityCheckers =
        assetIntegrityCheckerRepository.findByRepositoryAndAssetExtension(
            repository, assetExtension);
    Set<IntegrityCheckerType> integrityCheckerTypes =
        new HashSet<>(
            assetIntegrityCheckerRepository
                .findTypeIntegrityCheckerTypesByRepositoryIdAndAssetExtension(
                    repository.getId(), assetExtension));

    for (AssetIntegrityChecker assetIntegrityChecker : assetIntegrityCheckers) {
      integrityCheckerTypes.add(assetIntegrityChecker.getIntegrityCheckerType());
    }

    Set<TextUnitIntegrityChecker> textUnitIntegrityCheckers = new HashSet<>();

    for (IntegrityCheckerType integrityCheckerType : integrityCheckerTypes) {
      textUnitIntegrityCheckers.add(
          createInstanceForClassName(integrityCheckerType.getClassName()));
    }

    return textUnitIntegrityCheckers;
  }

  /**
   * @param className
   * @return An instance of {@link TextUnitIntegrityChecker} for the given class
   * @throws IntegrityCheckerInstantiationException
   */
  private TextUnitIntegrityChecker createInstanceForClassName(String className)
      throws IntegrityCheckerInstantiationException {
    try {
      Class<?> clazz = Class.forName(className);
      return (TextUnitIntegrityChecker) clazz.newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new IntegrityCheckerInstantiationException(
          "Cannot create an instance of TextUnitIntegrityChecker using reflection", e);
    }
  }
}
