package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Configurable
    public class MyUserInfoTokenServices extends UserInfoTokenServices {

    /**
     * logger
     */
    static Logger logger = getLogger(MyUserInfoTokenServices.class);

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    public MyUserInfoTokenServices(String userInfoEndpointUrl, String clientId) {
        super(userInfoEndpointUrl, clientId);
    }

    @Override
    protected Object getPrincipal(Map<String, Object> map) {
        logger.debug("Get principal: {}", map);

        UserDetails userDetails = null;

        map = getMapWithUserInfo(map);
        String username = (String) super.getPrincipal(map);

        User user = getOrCreateUser(map, username);

        return new UserDetailsImpl(user);
    }

    User getOrCreateUser(Map<String, Object> map, String username) {
        User user = userRepository.findByUsername(username);

        if (user == null || user.getPartiallyCreated()) {
            String givenName = (String) map.get("first_name");
            String surname = (String) map.get("last_name");
            String commonName = (String) map.get("name");

            user = userService.createOrUpdateBasicUser(user, username, givenName, surname, commonName);
        }

        return user;
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
