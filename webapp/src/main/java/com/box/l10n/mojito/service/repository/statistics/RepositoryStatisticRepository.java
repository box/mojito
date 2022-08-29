package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author jaurambault */
@RepositoryRestResource(exported = false)
public interface RepositoryStatisticRepository
    extends JpaRepository<RepositoryStatistic, Long>, JpaSpecificationExecutor<Repository> {}
