package com.box.l10n.mojito.service.repository;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author aloison */
@RepositoryRestResource(exported = false)
public interface RepositoryLocaleRepository extends JpaRepository<RepositoryLocale, Long> {

  RepositoryLocale findByRepositoryAndLocale(Repository repository, Locale locale);

  RepositoryLocale findByRepositoryAndLocale_Bcp47Tag(Repository repostiory, String bcp47tag);

  RepositoryLocale findByRepositoryAndParentLocaleIsNull(Repository repository);

  Long deleteByRepositoryAndParentLocaleIsNotNull(Repository repository);

  RepositoryLocale findByRepositoryIdAndLocaleId(Long repositoryId, long localeId);
}
