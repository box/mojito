package com.box.l10n.mojito.service.pluralform;
 
import com.box.l10n.mojito.entity.PluralForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface PluralFormRepository extends JpaRepository<PluralForm, Long>, JpaSpecificationExecutor<PluralForm> {
}
