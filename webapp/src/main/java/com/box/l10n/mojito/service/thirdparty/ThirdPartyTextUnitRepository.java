package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyTextUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.HashSet;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface ThirdPartyTextUnitRepository extends JpaRepository<ThirdPartyTextUnit, Long>, JpaSpecificationExecutor<ThirdPartyTextUnit> {

    @Query("select tptu.thirdPartyId from #{#entityName} tptu inner join tptu.asset a where a.repository = ?1")
    HashSet<String> findThirdPartyIdsByRepository(Repository repository);

    ThirdPartyTextUnit findByTmTextUnit(TMTextUnit tmTextUnit);
}
