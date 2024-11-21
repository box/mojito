package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface TmTextUnitPendingMTRepository extends JpaRepository<TmTextUnitPendingMT, Long> {

  TmTextUnitPendingMT findByTmTextUnitId(Long tmTextUnitId);

  @Query(
      value = "SELECT * FROM tm_text_unit_pending_mt ORDER BY id LIMIT :batchSize",
      nativeQuery = true)
  List<TmTextUnitPendingMT> findBatch(@Param("batchSize") int batchSize);
}
