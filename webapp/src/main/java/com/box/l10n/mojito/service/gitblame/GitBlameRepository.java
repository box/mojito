package com.box.l10n.mojito.service.gitblame;

import com.box.l10n.mojito.entity.GitBlame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Set;


@RepositoryRestResource(exported = false)
public interface GitBlameRepository extends JpaRepository<GitBlame, Long>, JpaSpecificationExecutor<GitBlame> {

    List<GitBlame> findByTmTextUnitIdIn(Set<Long> ids);
}
