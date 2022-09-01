package com.box.l10n.mojito.service.screenshot;

import com.box.l10n.mojito.entity.ScreenshotTextUnit;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author jaurambault */
@RepositoryRestResource(exported = false)
public interface ScreenshotTextUnitRepository
    extends JpaRepository<ScreenshotTextUnit, Long>, JpaSpecificationExecutor<ScreenshotTextUnit> {

  List<ScreenshotTextUnit> findByTmTextUnitIdIn(Set<Long> tmTextUnitId);

  @Modifying
  @Query("delete from ScreenshotTextUnit where screenshot.id = ?1")
  void deleteAllByScreenshotId(Long screenshotId);
}
