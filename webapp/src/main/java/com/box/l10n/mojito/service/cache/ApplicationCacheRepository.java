package com.box.l10n.mojito.service.cache;

import com.box.l10n.mojito.entity.ApplicationCache;
import com.box.l10n.mojito.entity.ApplicationCacheType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface ApplicationCacheRepository extends JpaRepository<ApplicationCache, Long> {

  @Query(
      value =
          """
          select ac from ApplicationCache ac
          where ac.applicationCacheType = :applicationCacheType and ac.keyMD5 = :keyMD5
          and (ac.expiryDate is null or ac.expiryDate > CURRENT_TIMESTAMP)
          """)
  Optional<ApplicationCache> findByIdAndNotExpired(
      @Param("applicationCacheType") ApplicationCacheType applicationCacheType,
      @Param("keyMD5") String keyMD5);

  Optional<ApplicationCache> findByApplicationCacheTypeAndKeyMD5(
      @Param("applicationCacheType") ApplicationCacheType applicationCacheType,
      @Param("keyMD5") String keyMD5);

  void deleteByApplicationCacheTypeAndKeyMD5(
      @Param("applicationCacheType") ApplicationCacheType applicationCacheType,
      @Param("keyMD5") String keyMD5);

  @Modifying
  @Query(
      value =
          """
          delete from ApplicationCache ac
          where ac.applicationCacheType.id = :cacheId
          """)
  void clearCache(@Param("cacheId") short cacheId);

  @Modifying
  @Query("delete from ApplicationCache ac where ac.expiryDate <= CURRENT_TIMESTAMP")
  void clearAllExpired();
}
