package com.box.l10n.mojito.react;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.security.CsrfTokenController;
import com.box.l10n.mojito.security.AuditorAwareImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.IllformedLocaleException;
import java.util.Locale;

/**
 * The controller used to serve the React application.
 *
 * We declare all the routes on both client and server side. If the route is not
 * declared on the server side, it will potentially cause 404 when a URL is
 * accessed and the app wasn't loaded. Ideally, we'd do a redirect on index.html
 * but this is not as straight forward as it sounds with Springboot.
 *
 * TODO(P1) Revisit the routing server side to replace it maybe with a generic
 * redirect to index.html instead of returning 404. The current problem is that
 * by default when no route is defined for a URL the 404 thrown by the spring
 * MVC can't be intercepted in the general exception handler (@ControllerAdvice
 * + @ExceptionHandler(value = Exception.class)). This can be configured with
 * DispatcherServerlet#setThrowExceptionIfNoHandlerFound() and returning a
 * DispatcherServerlet instance (@Bean). This requires to disable the default
 * Springboot configuration by enabling @EnableWebMvc (need to investigate the
 * impact of this). More info here:
 * http://stackoverflow.com/questions/28902374/spring-boot-rest-service-exception-handling
 *
 * @author Jean
 */
@Controller
public class ReactAppController {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ReactAppController.class);

    @Autowired
    CsrfTokenController csrfTokenController;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ReactStaticAppConfig reactStaticAppConfig;

    @Autowired
    AuditorAwareImpl auditorAwareImpl;

    @Value("${server.contextPath:}")
    String contextPath = "";

    //TODO(P1) For now, client routes must be copied in this controller
    @RequestMapping({
        "/",
        "/login",
        "repositories",
        "project-requests",
        "workbench",
        "branches",
        "settings",
        "screenshots"
    })
    @ResponseBody
    ModelAndView getIndex(
            HttpServletRequest httpServletRequest,
            @CookieValue(value = "locale", required = false, defaultValue = "en") String localeCookieValue) throws MalformedURLException, IOException {

        ModelAndView index = new ModelAndView("index");

        ReactUser reactUser = getReactUser();

        index.addObject("locale", getValidLocaleFromCookie(localeCookieValue));
        index.addObject("ict", httpServletRequest.getHeaders("X-Mojito-Ict").hasMoreElements());
        index.addObject("csrfToken", csrfTokenController.getCsrfToken(httpServletRequest));
        index.addObject("username", reactUser.getUsername());
        index.addObject("contextPath", contextPath);

        //TODO(spring2) this is just a POC - remove old config & update frontend next
        ReactAppConfig reactAppConfig = new ReactAppConfig(reactStaticAppConfig, reactUser);
        index.addObject("appConfig", objectMapper.writeValueAsStringUnchecked(reactAppConfig));

        return index;
    }

    ReactUser getReactUser() {
        return auditorAwareImpl.getCurrentAuditor().map(currentAuditor -> {
            ReactUser reactUser = new ReactUser();
            reactUser.setUsername(currentAuditor.getUsername());
            reactUser.setGivenName(currentAuditor.getGivenName());
            reactUser.setSurname(currentAuditor.getSurname());
            reactUser.setCommonName(currentAuditor.getCommonName());
            return reactUser;
        }).orElse(new ReactUser());
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
