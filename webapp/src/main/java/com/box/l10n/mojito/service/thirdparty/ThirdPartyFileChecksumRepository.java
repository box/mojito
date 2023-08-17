package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.ThirdPartyFileChecksum;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ThirdPartyFileChecksumRepository
    extends JpaRepository<ThirdPartyFileChecksum, Long> {

  Optional<ThirdPartyFileChecksum> findById(Long thirdPartyFileChecksumId);

  Optional<ThirdPartyFileChecksum> findByRepositoryAndFileNameAndLocale(
      Repository repository, String fileName, Locale locale);
}
