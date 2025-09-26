package com.box.l10n.mojito.service.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.box.l10n.mojito.rest.cli.CliWS;
import com.box.l10n.mojito.rest.cli.GitInfo;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;

public class CliServiceTest extends ServiceTestBase {

  @Autowired CliService cliService;

  @Test
  public void testGetCliUrlDefault() throws RepositoryNameAlreadyUsedException {
    String url = cliService.getCliUrl();
    Assert.assertEquals(
        "https://github.com/box/mojito/releases/download/v"
            + cliService.version
            + "/mojito-cli-"
            + cliService.version
            + "-exec.jar",
        url);
  }

  @Test
  public void testGetCliUrl() {
    CliService cliService = new CliService();
    cliService.version = "0.1";

    CliConfig cliConfig = new CliConfig();
    cliConfig.url = "http://someserver.io/{version}/{gitCommit}|{gitShortCommit}";
    cliService.cliConfig = cliConfig;

    cliService.gitInfo = new GitInfo();
    cliService.gitInfo.getCommit().setId("141708fc7e80556d69261c2cf4cdc82acfa337bc");

    Assert.assertEquals(
        "http://someserver.io/0.1/141708fc7e80556d69261c2cf4cdc82acfa337bc|141708f",
        cliService.getCliUrl());
  }

  @Test
  public void getLocalCliFileDefaultNoFile() {
    Optional<FileSystemResource> localCliFile = cliService.getLocalCliFile();
    assertFalse(localCliFile.isPresent());
  }

  @Test
  public void generateInstallCliScript() throws IOException {
    String installScript =
        cliService.generateInstallCliScript(
            "http://localhost:8080/cli/install.sh", "${PWD}/.mojito", Collections.emptyMap());
    //        Files.write(installScript, new
    // File("src/test/resources/com/box/l10n/mojito/service/cli/install.sh"),
    // StandardCharsets.UTF_8);
    String expected =
        Resources.toString(
            Resources.getResource("com/box/l10n/mojito/service/cli/install.sh"),
            StandardCharsets.UTF_8);
    assertEquals(expected, installScript);
  }

  @Test
  public void generateInstallCliScriptWithHeaders() throws IOException {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(
        CliWS.CF_ACCESS_HEADER_CLIENT_ID, "L10N_RESTTEMPLATE_HEADER_HEADERS_CF_ACCESS_CLIENT_ID");
    headers.put(
        CliWS.CF_ACCESS_HEADER_CLIENT_SECRET,
        "L10N_RESTTEMPLATE_HEADER_HEADERS_CF_ACCESS_CLIENT_SECRET");

    String installScript =
        cliService.generateInstallCliScript(
            "http://localhost:8080/cli/install.sh", "${PWD}/.mojito", headers);

    String expected =
        Resources.toString(
            Resources.getResource("com/box/l10n/mojito/service/cli/install_with_cf_headers.sh"),
            StandardCharsets.UTF_8);
    assertEquals(expected, installScript);
  }

  @Test
  public void getInstallCliContext() {
    InstallCliContext installCliContext =
        cliService.getInstallCliContext(
            "https://someinstall.org/cli/install.sh", "someplace", Collections.emptyMap());
    assertEquals("https", installCliContext.scheme);
    assertEquals("someinstall.org", installCliContext.host);
    assertEquals("443", installCliContext.port);
    assertEquals("someplace", installCliContext.installDirectory);
  }

  @Test
  public void getPort() throws IOException {
    assertEquals("443", cliService.getPort(new URL("https://somehost.org")));
    assertEquals("8080", cliService.getPort(new URL("http://localhost:8080")));
  }
}
