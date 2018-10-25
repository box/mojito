package com.box.l10n.mojito.security;

import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class MyUserInfoTokenServices extends UserInfoTokenServices {

    /**
     * logger
     */
    static Logger logger = getLogger(MyUserInfoTokenServices.class);

    UserDetailsServiceImpl userDetailsService;

    public MyUserInfoTokenServices(UserDetailsServiceImpl userDetailsService, String userInfoEndpointUrl, String clientId) {
        super(userInfoEndpointUrl, clientId);
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected Object getPrincipal(Map<String, Object> map) {
        logger.debug("Get principal: {}", map);

        UserDetails userDetails = null;

        map = getMapWithUserInfo(map);
        String username = (String) super.getPrincipal(map);

        try {
            logger.debug("Load principal: {}", username);
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            String givenName = (String) map.get("first_name");
            String surname = (String) map.get("last_name");
            String commonName = (String) map.get("name");
            logger.debug("username: {}, giveName:{}, surname: {}, commonName: {}", username, givenName, surname, commonName);
            userDetails = userDetailsService.createBasicUser(username, givenName, surname, commonName);
        }

        return userDetails;
    }

    /**
     * Some API retruns the user info directly and some have one level of indirection with a key name called "user".
     * This function get the map that contains user information.
     *
     * @param map from the response
     * @return the map that contains the user info
     */
    private Map<String, Object> getMapWithUserInfo(Map<String, Object> map) {
        Object user = map.get("user");
        if (user instanceof Map) {
            map = (Map<String, Object>) user;
        }
        return map;
    }
}
