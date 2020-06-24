package com.box.l10n.mojito.service.blobstorage.database;

import com.box.l10n.mojito.entity.MBlob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author jaurambault
 */
@Repository
public interface MBlobRepository extends JpaRepository<MBlob, Long>, JpaSpecificationExecutor<MBlob> {

    MBlob findByName(@Param("name") String name);

    @Query(
            "select mb.id from #{#entityName} mb " +
                    "where (unix_timestamp(mb.createdDate) + mb.expireAfterSeconds) < unix_timestamp()"
    )
    List<Long> findExpiredBlobIds(Pageable pageable);

    @Transactional
    @Modifying
    @Query("delete from #{#entityName} mb where mb.id in ?1")
    int deleteByIds(List<Long> ids);
}
