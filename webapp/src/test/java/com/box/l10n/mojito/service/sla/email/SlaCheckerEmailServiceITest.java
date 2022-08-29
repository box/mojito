package com.box.l10n.mojito.service.sla.email;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** @author jeanaurambault */
public class SlaCheckerEmailServiceITest extends ServiceTestBase {

  @Autowired SlaCheckerEmailService slaCheckerEmailService;

  @Test
  public void testGetCloseIncidentEmailContent() throws IOException {
    long incidentId = 0L;
    String email = slaCheckerEmailService.getCloseIncidentEmailContent(incidentId);
    // Files.write(email, new
    // File("src/test/resources/com/box/l10n/mojito/service/sla/email/closeIncident.html"),
    // StandardCharsets.UTF_8);
    String expected =
        Resources.toString(
            Resources.getResource("com/box/l10n/mojito/service/sla/email/closeIncident.html"),
            StandardCharsets.UTF_8);
    assertEquals(expected, email);
  }

  @Test
  public void testGetOpenIncidentEmailContent() throws IOException {
    long incidentId = 0L;
    String email =
        slaCheckerEmailService.getOpenIncidentEmailContent(incidentId, getRepositoriesForTest());
    // Files.write(email, new
    // File("src/test/resources/com/box/l10n/mojito/service/sla/email/openIncident.html"),
    // StandardCharsets.UTF_8);
    String expected =
        Resources.toString(
            Resources.getResource("com/box/l10n/mojito/service/sla/email/openIncident.html"),
            StandardCharsets.UTF_8);
    assertEquals(expected, email);
  }

  List<Repository> getRepositoriesForTest() {
    Repository repository1 = new Repository();
    repository1.setName("test1");
    repository1.setRepositoryStatistic(new RepositoryStatistic());
    repository1.getRepositoryStatistic().setOoslaTextUnitWordCount(10L);

    Repository repository2 = new Repository();
    repository2.setName("test2");
    repository2.setRepositoryStatistic(new RepositoryStatistic());
    repository2.getRepositoryStatistic().setOoslaTextUnitWordCount(12L);

    return Arrays.asList(repository1, repository2);
  }
}
