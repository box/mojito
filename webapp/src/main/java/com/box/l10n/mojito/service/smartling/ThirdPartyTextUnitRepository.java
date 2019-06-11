package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.ThirdPartyTextUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface ThirdPartyTextUnitRepository extends JpaRepository<ThirdPartyTextUnit, Long>, JpaSpecificationExecutor<ThirdPartyTextUnit> {

    ThirdPartyTextUnit findByThirdPartyTextUnitId(String thirdPartyTextUnit);

    List<ThirdPartyTextUnit> findByThirdPartyTextUnitIdIsInAndMappingKeyIsIn(List<String> thirdPartyTextUnit, List<String> mappingKey);

}