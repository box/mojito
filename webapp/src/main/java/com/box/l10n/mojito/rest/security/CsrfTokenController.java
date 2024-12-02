package com.box.l10n.mojito.rest.security;

import static org.slf4j.LoggerFactory.getLogger;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * This endpoint is specifically created so that the CLI can easily get the token without parsing
 * the html. It is to the best of our knowledge not any worse than having the CSRF token embedded as
 * an input field of the html page.
 *
 * @author wyau
 */
@RestController
public class CsrfTokenController {

  /** logger */
  static Logger logger = getLogger(CsrfTokenController.class);

  public static final String CSRF_TOKEN_PATH = "/api/csrf-token";

  @RequestMapping(method = RequestMethod.GET, value = CSRF_TOKEN_PATH)
  @ResponseStatus(HttpStatus.OK)
  public String getCsrfToken(HttpServletRequest httpServletRequest) {

    CsrfToken csrfToken = (CsrfToken) httpServletRequest.getAttribute(CsrfToken.class.getName());

    return csrfToken != null ? csrfToken.getToken() : null;
  }
}
