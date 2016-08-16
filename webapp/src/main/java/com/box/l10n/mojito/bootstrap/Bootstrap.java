package com.box.l10n.mojito.bootstrap;

import com.box.l10n.mojito.aspect.security.RunAs;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.security.user.UserRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author wyau
 */
@Component
public class Bootstrap implements ApplicationListener<ContextRefreshedEvent> {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    boolean initialized = false;

    @Autowired
    BootstrapConfig bootstrapConfig;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!initialized) {

            if (bootstrapConfig.isEnabled()) {
                createData();
            }

            initialized = true;
        }
    }

    /**
     * Checks if a system user exists to know if we need to create data or not
     *
     * @return {@code true} if data should be created else {@code false}
     */
    public boolean shouldCreateData() {
        return userRepository.count() == 0;
    }

    /**
     * Create data in the database if none exist.
     */
    public void createData() {
        if (shouldCreateData()) {
            createSystemUser();
            createDefaultUser();
        } else {
            logger.debug("Data already present in the database, don't create data");
        }
    }

    @Transactional
    public void createSystemUser() {
        logger.info("Creating system user with random password");
        String randomPassword = RandomStringUtils.randomAlphanumeric(15);

        User systemUser = userService.createUserWithRole(UserService.SYSTEM_USERNAME, randomPassword, Role.ADMIN);

        logger.debug("Disabling System user so that it can't be authenticated");
        systemUser.setEnabled(false);
        userRepository.save(systemUser);

        logger.debug("Setting created by user manually because there is no authenticated user context");
        userService.updateCreatedByUserToSystemUser(systemUser);
    }

    @RunAs(username = UserService.SYSTEM_USERNAME)
    public void createDefaultUser() {
        userService.createUserWithRole(bootstrapConfig.getDefaultUser().getUsername(), bootstrapConfig.getDefaultUser().getPassword(), Role.ADMIN);
    }
}
