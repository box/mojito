package com.box.l10n.mojito.react;

import com.box.l10n.mojito.rest.security.CsrfTokenController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.IllformedLocaleException;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

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
    
    @Value("${server.contextPath:}")
    String contextPath = "";
   
    //TODO(P1) For now, client routes must be copied in this controller
    @RequestMapping({
        "/",
        "/login",
        "repositories",
        "project-requests",
        "workbench",
        "settings"
    })
    @ResponseBody
    ModelAndView getIndex(HttpServletRequest httpServletRequest, @CookieValue(value = "locale", required = false, defaultValue = "en") String localeCookieValue) throws MalformedURLException, IOException {

        ModelAndView index = new ModelAndView("index");

        index.addObject("locale", getValidLocaleFromCookie(localeCookieValue));
        index.addObject("csrfToken", csrfTokenController.getCsrfToken(httpServletRequest));
        index.addObject("username", getUsername());
        index.addObject("contextPath", contextPath);

        return index;
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

    /**
     * Get the username from the security context if
     *
     * @return
     */
    String getUsername() {

        String username = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            username = authentication.getName();
        }

        return username;
    }
    
}
