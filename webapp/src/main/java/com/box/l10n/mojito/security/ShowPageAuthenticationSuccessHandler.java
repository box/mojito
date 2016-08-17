package com.box.l10n.mojito.security;

import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Shows the specific page on success if it is provided.  Otherwise, falls back to saved request.
 *
 * @author wyau
 */
public class ShowPageAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    /**
     * logger
     */
    static Logger logger = getLogger(ShowPageAuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        String showPage = request.getParameter("showPage");

        logger.debug("Authentication success, showPage = {}", showPage);

        if (showPage != null) {
            getRedirectStrategy().sendRedirect(request, response, "/" + showPage);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}
