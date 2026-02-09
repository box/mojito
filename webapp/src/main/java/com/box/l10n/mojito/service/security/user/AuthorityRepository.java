package com.box.l10n.mojito.service.security.user;

import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.entity.security.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author wyau
 */
@RepositoryRestResource(exported = false)
public interface AuthorityRepository
    extends JpaRepository<Authority, Long>, JpaSpecificationExecutor<Authority> {

  Authority findByUser(User user);

  @Query(
      """
          select a.user.id as userId, a.authority as authority
          from Authority a
          where a.user.id in :userIds
          """)
  List<UserAuthorityProjection> findAuthoritiesForUsers(@Param("userIds") List<Long> userIds);
}
