package com.box.l10n.mojito.service.security.user;

import com.box.l10n.mojito.entity.security.user.UserLocale;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface UserLocaleRepository
    extends JpaRepository<UserLocale, Long>, JpaSpecificationExecutor<UserLocale> {

  @Query(
      """
          select ul.user.id as userId, locale.bcp47Tag as bcp47Tag
          from UserLocale ul
          join ul.locale locale
          where ul.user.id in :userIds
          """)
  List<UserLocaleTagProjection> findLocaleTagsForUsers(@Param("userIds") List<Long> userIds);
}
