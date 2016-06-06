package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetIntegrityChecker;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.assetintegritychecker.AssetIntegrityCheckerRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wyau
 */
@Component
public class IntegrityCheckerFactory {

    @Autowired
    AssetIntegrityCheckerRepository assetIntegrityCheckerRepository;

    @Autowired
    ApplicationContext applicationContext;

    List<DocumentIntegrityChecker> documentIntegrityCheckers = new ArrayList<>();

    @PostConstruct
    private void initAvailableDocumentCheckers() {
        Iterable<DocumentIntegrityChecker> documentIntegrityCheckersIterable = applicationContext.getBeansOfType(DocumentIntegrityChecker.class).values();
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
     * @param asset
     * @return Instance of {@link TextUnitIntegrityChecker}, depending on the given asset
     * @throws IntegrityCheckerInstantiationException if unable to create an instance of the integrity checker
     */
    public TextUnitIntegrityChecker getTextUnitChecker(Asset asset) {

        Repository repository = asset.getRepository();
        String assetExtension = FilenameUtils.getExtension(asset.getPath());

        AssetIntegrityChecker assetIntegrityChecker = assetIntegrityCheckerRepository.findByRepositoryAndAssetExtension(repository, assetExtension);
        TextUnitIntegrityChecker textUnitIntegrityChecker = null;

        if (assetIntegrityChecker != null) {
            String className = assetIntegrityChecker.getIntegrityCheckerType().getClassName();
            textUnitIntegrityChecker = createInstanceForClassName(className);
        }

        return textUnitIntegrityChecker;
    }

    /**
     * @param className
     * @return An instance of {@link TextUnitIntegrityChecker} for the given class
     * @throws IntegrityCheckerInstantiationException
     */
    private TextUnitIntegrityChecker createInstanceForClassName(String className) throws IntegrityCheckerInstantiationException {
        try {
            Class<?> clazz = Class.forName(className);
            return (TextUnitIntegrityChecker) clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new IntegrityCheckerInstantiationException("Cannot create an instance of TextUnitIntegrityChecker using reflection", e);
        }
    }
}
