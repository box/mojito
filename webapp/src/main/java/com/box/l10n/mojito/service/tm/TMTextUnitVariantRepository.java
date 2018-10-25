package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitVariantRepository extends JpaRepository<TMTextUnitVariant, Long> {

    List<TMTextUnitVariant> findAllByLocale_IdAndTmTextUnit_Tm_id(Long localeId, Long tmId);

    TMTextUnitVariant findTopByTmTextUnitTmIdOrderByCreatedDateDesc(Long tmId);
    
    List<TMTextUnitVariant> findByTmTextUnitTmRepositoriesOrderByContent(Repository repository);

    List<TMTextUnitVariant> findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(Repository repository, String bcp47Tag);

}
