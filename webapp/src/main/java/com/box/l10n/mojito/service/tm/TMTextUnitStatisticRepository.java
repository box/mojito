package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitStatisticRepository extends JpaRepository<TMTextUnitStatistic, Long> {}
