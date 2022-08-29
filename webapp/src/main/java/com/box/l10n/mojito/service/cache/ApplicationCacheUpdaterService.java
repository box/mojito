package com.box.l10n.mojito.service.cache;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.ApplicationCache;
import com.box.l10n.mojito.entity.ApplicationCacheType;
import com.box.l10n.mojito.service.DBUtils;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

/**
 * Class that implements MySQL optimized versions of the upsert methods for the database-backed
 * cache.
 */
@Component
public class ApplicationCacheUpdaterService {
  /** logger */
  static Logger logger = getLogger(ApplicationCacheUpdaterService.class);

  @Autowired EntityManager entityManager;

  @Autowired ApplicationCacheRepository applicationCacheRepository;

  @Autowired DBUtils dbUtils;

  /** Upsert a cache entry with a TTL, with a MySql optimized version. */
  void upsertWithTTL(
      @Param("cacheTypeId") short cacheTypeId,
      @Param("keyMD5") String keyMD5,
      @Param("value") byte[] value,
      @Param("ttlInSeconds") long ttlInSeconds) {
    if (dbUtils.isMysql()) {
      String mySqlQuery =
          "INSERT INTO application_cache "
              + "value (NULL, :cacheTypeId, :keyMD5, :value, CURRENT_TIMESTAMP, ADDDATE(CURRENT_TIMESTAMP, INTERVAL :ttlInSeconds SECOND))"
              + "ON DUPLICATE KEY UPDATE value = :value, created_date = CURRENT_TIMESTAMP, expiry_date = ADDDATE(CURRENT_TIMESTAMP, INTERVAL :ttlInSeconds SECOND)";

      Query query = entityManager.createNativeQuery(mySqlQuery);
      query.setParameter("cacheTypeId", cacheTypeId);
      query.setParameter("keyMD5", keyMD5);
      query.setParameter("value", value);
      query.setParameter("ttlInSeconds", ttlInSeconds);
      query.executeUpdate();

      entityManager.flush();
      entityManager.clear();
    } else {
      ApplicationCacheType applicationCacheType = new ApplicationCacheType(cacheTypeId);

      Optional<ApplicationCache> existingEntry =
          applicationCacheRepository.findByApplicationCacheTypeAndKeyMD5(
              applicationCacheType, keyMD5);
      DateTime currentSqlTimestamp = getCurrentSqlTimestamp();

      ApplicationCache applicationCache;

      if (existingEntry.isPresent()) {
        applicationCache = existingEntry.get();
        applicationCache.setValue(value);
        applicationCache.setCreatedDate(currentSqlTimestamp.plusSeconds((int) ttlInSeconds));
      } else {
        applicationCache =
            new ApplicationCache(
                applicationCacheType,
                keyMD5,
                value,
                currentSqlTimestamp.plusSeconds((int) ttlInSeconds));
        applicationCache.setCreatedDate(currentSqlTimestamp);
        applicationCacheRepository.save(applicationCache);
      }
    }
  }

  /** Upsert a cache entry without a TTL being specified, with a MySql optimized version. */
  void upsertNoExpiryDate(
      @Param("cacheTypeId") short cacheTypeId,
      @Param("keyMD5") String keyMD5,
      @Param("value") byte[] value) {
    if (dbUtils.isMysql()) {
      String mySqlQuery =
          "insert into application_cache "
              + "value (NULL, :cacheTypeId, :keyMD5, :value, CURRENT_TIMESTAMP, NULL)"
              + "ON DUPLICATE KEY UPDATE value = :value, created_date = CURRENT_TIMESTAMP";

      Query query = entityManager.createNativeQuery(mySqlQuery);
      query.setParameter("cacheTypeId", cacheTypeId);
      query.setParameter("keyMD5", keyMD5);
      query.setParameter("value", value);

      query.executeUpdate();

      entityManager.flush();
      entityManager.clear();
    } else {
      ApplicationCacheType applicationCacheType = new ApplicationCacheType(cacheTypeId);

      Optional<ApplicationCache> existingEntry =
          applicationCacheRepository.findByApplicationCacheTypeAndKeyMD5(
              applicationCacheType, keyMD5);
      ApplicationCache applicationCache;
      DateTime currentSqlTimestamp = getCurrentSqlTimestamp();

      if (existingEntry.isPresent()) {
        applicationCache = existingEntry.get();
        applicationCache.setValue(value);
        applicationCache.setCreatedDate(currentSqlTimestamp);
      } else {
        applicationCache = new ApplicationCache(applicationCacheType, keyMD5, value, null);
        applicationCache.setCreatedDate(currentSqlTimestamp);
        applicationCacheRepository.save(applicationCache);
      }
    }
  }

  private DateTime getCurrentSqlTimestamp() {
    DateTime currentTimestamp;

    try {
      currentTimestamp =
          new DateTime(
              entityManager
                  .createNativeQuery(
                      "SELECT TOP 1 CURRENT_TIMESTAMP FROM INFORMATION_SCHEMA.TABLES")
                  .getSingleResult());
    } catch (Exception ex) {
      logger.error("Could not retrieve current timestamp from the SQL DB", ex);
      currentTimestamp = DateTime.now();
    }
    return currentTimestamp;
  }
}
