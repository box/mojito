package com.box.l10n.mojito.service.security.user;

import com.box.l10n.mojito.entity.security.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author wyau
 */
@RepositoryRestResource(exported = false)
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

  @EntityGraph(value = "User.legacy", type = EntityGraphType.FETCH)
  User findByUsername(String username);

  @Override
  @EntityGraph(value = "User.legacy", type = EntityGraphType.FETCH)
  List<User> findAll(Specification<User> spec, Sort sort);

  // Note it is not possible to use an entity graph here as it will give the following issue:
  // HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
  // @EntityGraph(value = "User.legacy", type = EntityGraphType.FETCH)
  //
  // See {@link com.box.l10n.mojito.service.security.user.UserService#findAll()}
  @Override
  Page<User> findAll(Specification<User> spec, Pageable pageable);

  @Override
  @EntityGraph(value = "User.legacy", type = EntityGraphType.FETCH)
  Optional<User> findById(Long aLong);

  @Query(
      """
      select u
      from User u
      where (:username is null or u.username = :username)
        and ((:search is null or :search = '')
             or (lower(u.username) like lower(concat('%', :search, '%'))
                 or (u.commonName is not null
                     and u.commonName <> ''
                     and lower(u.commonName) like lower(concat('%', :search, '%')))
                 or ((u.commonName is null or u.commonName = '')
                      and lower(concat(u.givenName, ' ', u.surname)) like lower(concat('%', :search, '%')))))
        and u.enabled = true
      """)
  Page<User> findByUsernameOrName(
      @Param("username") String username, @Param("search") String search, Pageable pageable);

  @Query(
      value =
          """
    SELECT u FROM  User u
      WHERE COALESCE(u.username, '') <> ''
        AND LOWER(u.username) LIKE CONCAT(LOWER(:servicePrefix), '%')
        AND LOWER(:serviceName) LIKE CONCAT(LOWER(u.username), '%')
        AND u.enabled IS TRUE
    """)
  @EntityGraph(value = "User.legacy", type = EntityGraphType.FETCH)
  List<User> findServicesByServiceNameAndPrefix(
      @Param("serviceName") String serviceName, @Param("servicePrefix") String servicePrefix);
}
