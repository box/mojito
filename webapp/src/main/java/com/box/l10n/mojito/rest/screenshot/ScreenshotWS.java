package com.box.l10n.mojito.rest.screenshot;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.ScreenshotRun;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.screenshot.ScreenshotRunType;
import com.box.l10n.mojito.service.screenshot.ScreenshotService;
import com.box.l10n.mojito.service.tm.search.SearchType;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** @author jaurambault */
@RestController
public class ScreenshotWS {

  /** logger */
  static Logger logger = getLogger(ScreenshotWS.class);

  @Autowired ScreenshotService screenshotService;

  @RequestMapping(value = "/api/screenshots", method = RequestMethod.POST)
  public ScreenshotRun createOrAddToScreenshotRun(@RequestBody ScreenshotRun screenshotRun) {
    ScreenshotRun screenshotRunSaved =
        screenshotService.createOrAddToScreenshotRun(screenshotRun, true);
    return screenshotRunSaved;
  }

  @JsonView(View.Screenshots.class)
  @RequestMapping(value = "/api/screenshots", method = RequestMethod.GET)
  public List<Screenshot> getScreeenshots(
      @RequestParam(value = "repositoryIds[]", required = false) ArrayList<Long> repositoryIds,
      @RequestParam(value = "bcp47Tags[]", required = false) ArrayList<String> bcp47Tags,
      @RequestParam(required = false) String screenshotName,
      @RequestParam(required = false) Screenshot.Status status,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "source", required = false) String source,
      @RequestParam(value = "target", required = false) String target,
      @RequestParam(value = "searchType", required = false, defaultValue = "EXACT")
          SearchType searchType,
      @RequestParam(value = "screenshotRunType", required = false)
          ScreenshotRunType screenshotRunType,
      @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
      @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) {

    return screenshotService.searchScreenshots(
        repositoryIds,
        bcp47Tags,
        screenshotName,
        status,
        name,
        source,
        target,
        searchType,
        screenshotRunType,
        offset,
        limit);
  }

  @RequestMapping(
      value = "/api/screenshots/{screenShotRunId}/{screenshotName}",
      method = RequestMethod.GET)
  public Long getScreenshotIdByScreenshotRunAndName(
      @PathVariable Long screenShotRunId, @PathVariable String screenshotName) {
    Screenshot screenshot =
        screenshotService.getScreenshotByScreenshotRunAndName(screenShotRunId, screenshotName);
    if (screenshot == null) {
      throw new ScreenshotWithNameAndScreenshotRunIdNotFoundException(
          screenshotName, screenShotRunId);
    }
    return screenshot.getId();
  }

  @RequestMapping(value = "/api/screenshots/{id}", method = RequestMethod.PUT)
  public void updateScreenshot(@PathVariable Long id, @RequestBody Screenshot screenshot) {
    screenshot.setId(id);
    screenshotService.updateScreenshot(screenshot);
  }

  @RequestMapping(value = "/api/screenshots/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void deleteScreenshot(@PathVariable Long id) {
    screenshotService.deleteScreenshot(id);
  }
}
