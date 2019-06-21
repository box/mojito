package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyTextUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface ThirdPartyTextUnitRepository extends JpaRepository<ThirdPartyTextUnit, Long>, JpaSpecificationExecutor<ThirdPartyTextUnit> {

    ThirdPartyTextUnit findByThirdPartyTextUnitId(String thirdPartyTextUnitId);

    ThirdPartyTextUnit findByTmTextUnitId(Long tmTextUnitId);

    @Query("select new com.box.l10n.mojito.service.smartling.ThirdPartyTextUnitDTO(t.id, t.thirdPartyTextUnitId, t.mappingKey, t.tmTextUnit.id) from ThirdPartyTextUnit t where t.thirdPartyTextUnitId IN (?1) and t.mappingKey IN (?2)")
    List<ThirdPartyTextUnitDTO> getByThirdPartyTextUnitIdIsInAndMappingKeyIsIn(List<String> thirdPartyTextUnit, List<String> mappingKey);

}