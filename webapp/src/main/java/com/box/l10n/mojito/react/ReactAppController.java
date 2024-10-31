package com.box.l10n.mojito.react;

import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.mustache.MustacheBaseContext;
import com.box.l10n.mojito.mustache.MustacheTemplateEngine;
import com.box.l10n.mojito.rest.security.CsrfTokenController;
import com.box.l10n.mojito.security.AuditorAwareImpl;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.security.user.AuthorityRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.IllformedLocaleException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * The controller used to serve the React application.
 *
 * <p>We declare all the routes on both client and server side. If the route is not declared on the
 * server side, it will potentially cause 404 when a URL is accessed and the app wasn't loaded.
 * Ideally, we'd do a redirect on index.html but this is not as straight forward as it sounds with
 * Springboot.
 *
 * <p>TODO(P1) Revisit the routing server side to replace it maybe with a generic redirect to
 * index.html instead of returning 404. The current problem is that by default when no route is
 * defined for a URL the 404 thrown by the spring MVC can't be intercepted in the general exception
 * handler (@ControllerAdvice + @ExceptionHandler(value = Exception.class)). This can be configured
 * with DispatcherServerlet#setThrowExceptionIfNoHandlerFound() and returning a DispatcherServerlet
 * instance (@Bean). This requires to disable the default Springboot configuration by
 * enabling @EnableWebMvc (need to investigate the impact of this). More info here:
 * http://stackoverflow.com/questions/28902374/spring-boot-rest-service-exception-handling
 *
 * @author Jean
 */
@Controller
public class ReactAppController {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ReactAppController.class);

  private static final MediaType TEXT_HTML_UTF_8 =
      new MediaType("text", "html", StandardCharsets.UTF_8);

  @Autowired CsrfTokenController csrfTokenController;

  @Autowired ObjectMapper objectMapper;

  @Autowired ReactStaticAppConfig reactStaticAppConfig;

  @Autowired AuditorAwareImpl auditorAwareImpl;

  @Autowired AuthorityRepository authorityRepository;

  @Autowired UserService userService;

  @Autowired MustacheTemplateEngine mustacheTemplateEngine;

  @Value("${l10n.webapp.analytics.html.include:}")
  String analyticsHtmlInclude;

  @Value("${l10n.webapp.user-menu.logout.hide:false}")
  boolean userMenuLogoutHidden;

  @Value("${server.contextPath:}")
  String contextPath = "";

  // TODO(P1) For now, client routes must be copied in this controller
  @RequestMapping({
    "/",
    "/login",
    "repositories",
    "project-requests",
    "workbench",
    "branches",
    "screenshots",
    "settings",
    "settings/user-management",
    "settings/box"
  })
  ResponseEntity<String> getIndex(
      HttpServletRequest httpServletRequest,
      @CookieValue(value = "locale", required = false, defaultValue = "en")
          String localeCookieValue)
      throws MalformedURLException, IOException {

    ReactTemplateContext index = new ReactTemplateContext();

    ReactAppConfig reactAppConfig = new ReactAppConfig(reactStaticAppConfig, getReactUser());
    reactAppConfig.setLocale(getValidLocaleFromCookie(localeCookieValue));
    reactAppConfig.setIct(httpServletRequest.getHeaders("X-Mojito-Ict").hasMoreElements());
    reactAppConfig.setCsrfToken(csrfTokenController.getCsrfToken(httpServletRequest));
    reactAppConfig.setContextPath(contextPath);
    reactAppConfig.setAnalyticsHtmlInclude(analyticsHtmlInclude);
    reactAppConfig.setUserMenuLogoutHidden(this.userMenuLogoutHidden);

    index.appConfig = objectMapper.writeValueAsStringUnchecked(reactAppConfig);
    index.locale = reactAppConfig.locale;
    index.contextPath = reactAppConfig.contextPath;
    index.analyticsHtmlInclude = reactAppConfig.analyticsHtmlInclude;
    // We must keep CSRF_TOKEN = '{{csrfToken}}'; in index.html, because some client rely on that
    // for authentication
    // Removing it would require code client update. So we keep for backward compatibility
    // eventhough it is not
    // great
    index.csrfToken = reactAppConfig.csrfToken;

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(TEXT_HTML_UTF_8);

    String body = mustacheTemplateEngine.render("index.html", index);

    return new ResponseEntity<>(body, responseHeaders, HttpStatus.OK);
  }

  public static class ReactTemplateContext extends MustacheBaseContext {
    public String appConfig;
    public String locale;
    public String contextPath;
    public String csrfToken;
    public String analyticsHtmlInclude;
  }

  ReactUser getReactUser() {
    return auditorAwareImpl
        .getCurrentAuditor()
        .map(
            currentAuditor -> {
              ReactUser reactUser = new ReactUser();
              reactUser.setUsername(currentAuditor.getUsername());
              reactUser.setGivenName(currentAuditor.getGivenName());
              reactUser.setSurname(currentAuditor.getSurname());
              reactUser.setCommonName(currentAuditor.getCommonName());

              Role role = Role.ROLE_USER;
              Authority authority = authorityRepository.findByUser(currentAuditor);
              if (authority != null) {
                role = userService.createRoleFromAuthority(authority.getAuthority());
              }
              reactUser.setRole(role);
              return reactUser;
            })
        .orElse(new ReactUser());
  }

  /**
   * Get a valid locale from the cookie value.
   *
   * @param localeCookieValue
   * @return a valid locale.
   */
  String getValidLocaleFromCookie(String localeCookieValue) {

    String validLocale;

    try {
      Locale localeFromCookie = new Locale.Builder().setLanguageTag(localeCookieValue).build();
      validLocale = localeFromCookie.toLanguageTag();
    } catch (NullPointerException | IllformedLocaleException e) {
      logger.debug("Invalid localeCookieValue, fallback to en");
      validLocale = "en";
    }

    return validLocale;
  }
}
