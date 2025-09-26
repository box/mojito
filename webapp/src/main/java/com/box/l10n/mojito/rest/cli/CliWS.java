package com.box.l10n.mojito.rest.cli;

import com.box.l10n.mojito.service.cli.CliService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jaurambault
 */
@RestController
public class CliWS {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(CliWS.class);

  @Autowired CliService cliService;

  public static final String CF_ACCESS_HEADER_CLIENT_ID = "CF-Access-Client-Id";
  public static final String CF_ACCESS_HEADER_CLIENT_SECRET = "CF-Access-Client-Secret";

  static final Map<String, String> SUPPORTED_HEADER_TO_ENV_VAR;

  static {
    Map<String, String> headerToEnvVar = new LinkedHashMap<>();
    headerToEnvVar.put(
        CF_ACCESS_HEADER_CLIENT_ID, "L10N_RESTTEMPLATE_HEADER_HEADERS_CF_ACCESS_CLIENT_ID");
    headerToEnvVar.put(
        CF_ACCESS_HEADER_CLIENT_SECRET, "L10N_RESTTEMPLATE_HEADER_HEADERS_CF_ACCESS_CLIENT_SECRET");
    SUPPORTED_HEADER_TO_ENV_VAR = Collections.unmodifiableMap(headerToEnvVar);
  }

  /**
   * Entry point do download the CLI that corresponds to this server version.
   *
   * <p>Serve the local file if it exists or redirects to the URL where the CLI can be downloaded.
   *
   * <p>By default Github release is referenced (no local file), so that it works for standard
   * releases process.
   *
   * @param httpServletResponse
   * @return
   * @throws IOException
   */
  @RequestMapping(value = "/cli/mojito-cli.jar", method = RequestMethod.GET)
  @ResponseBody
  public FileSystemResource getFile(HttpServletResponse httpServletResponse) throws IOException {

    Optional<FileSystemResource> fileSystemResource = cliService.getLocalCliFile();

    return fileSystemResource.orElseGet(
        () -> {
          unsafeSendRedirect(httpServletResponse, cliService.getCliUrl());
          return null;
        });
  }

  void unsafeSendRedirect(HttpServletResponse httpServletResponse, String cliUrl) {
    try {
      httpServletResponse.sendRedirect(cliUrl);
    } catch (IOException ioe) {
      throw new RuntimeException("Can't send redirect for the CLI", ioe);
    }
  }

  /**
   * Entry point to download a bash script to install the CLI
   *
   * @param httpServletRequest
   * @param installDirectory
   * @return
   * @throws IOException
   */
  @RequestMapping(value = "/cli/install.sh", method = RequestMethod.GET)
  @ResponseBody
  public String getInstallCliScript(
      HttpServletRequest httpServletRequest,
      @RequestParam(value = "installDirectory", defaultValue = "#{'$'}{PWD}/.mojito")
          String installDirectory)
      throws IOException {
    String requestUrl = httpServletRequest.getRequestURL().toString();
    Map<String, String> authenticationHeaders = getAuthenticationHeaders(httpServletRequest);

    return cliService.generateInstallCliScript(requestUrl, installDirectory, authenticationHeaders);
  }

  Map<String, String> getAuthenticationHeaders(HttpServletRequest httpServletRequest) {
    Map<String, String> headers = new LinkedHashMap<>();

    SUPPORTED_HEADER_TO_ENV_VAR.forEach(
        (headerName, envVar) -> {
          if (httpServletRequest.getHeader(headerName) != null) {
            headers.put(headerName, envVar);
          }
        });

    return headers;
  }

  /**
   * Gets current CLI version of this server.
   *
   * @return
   */
  @RequestMapping(value = "/cli/version", method = RequestMethod.GET)
  public String getVersion() {
    return cliService.getVersion();
  }
}
