package com.box.l10n.mojito.service.screenshot;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.ScreenshotRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface ScreenshotRepository
    extends JpaRepository<Screenshot, Long>, JpaSpecificationExecutor<Screenshot> {

  Screenshot findByScreenshotRunAndNameAndLocale(
      ScreenshotRun screenshotRun, String name, Locale locale);

  @Query(
      value =
          """
          select s from #{#entityName} s
          inner join s.screenshotRun sr
          inner join sr.repository r
          left join  s.thirdPartyScreenshots tps
          where r = ?1 and r.manualScreenshotRun = sr and tps is null
          """)
  List<Screenshot> findUnmappedScreenshots(Repository repository);

  void deleteById(Long screenshotId);
}
