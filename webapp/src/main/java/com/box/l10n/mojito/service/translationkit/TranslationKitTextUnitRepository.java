package com.box.l10n.mojito.service.translationkit;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.entity.TranslationKitTextUnit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TranslationKitTextUnitRepository extends JpaRepository<TranslationKitTextUnit, Long> {

    TranslationKitTextUnit findByTranslationKitAndTmTextUnitAndTranslationKit_Locale(TranslationKit translationKit, TMTextUnit tmTextUnit, Locale locale);

    int countByTranslationKitAndImportedTmTextUnitVariantIsNotNull(TranslationKit translationKit);

    int countByTranslationKitAndSourceEqualsTargetTrue(TranslationKit translationKit);

    @Query("select count(*) from #{#entityName} tktu where tktu.translationKit = ?1 and tktu.detectedLanguage != tktu.detectedLanguageExpected")
    int countByTranslationKitAndDetectedLanguageNotEqualsDetectedLanguageExpected(TranslationKit translationKit);
    
    List<TranslationKitTextUnit> findByTranslationKit(TranslationKit translationKit);
}
