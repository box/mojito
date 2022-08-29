package com.box.l10n.mojito.service.blobstorage.database;

import com.box.l10n.mojito.entity.MBlob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** @author jaurambault */
@Repository
public interface MBlobRepository
    extends JpaRepository<MBlob, Long>, JpaSpecificationExecutor<MBlob> {

  Optional<MBlob> findByName(@Param("name") String name);

  @Query(
      "select mb.id from #{#entityName} mb "
          + "where (unix_timestamp(mb.createdDate) + mb.expireAfterSeconds) < unix_timestamp()")
  List<Long> findExpiredBlobIds(Pageable pageable);

  @Transactional
  @Modifying
  @Query("delete from #{#entityName} mb where mb.id in ?1")
  int deleteByIds(List<Long> ids);

  @Query("select mb.id from  #{#entityName} mb where mb.name = ?1")
  Optional<Long> findIdByName(String name);
}
