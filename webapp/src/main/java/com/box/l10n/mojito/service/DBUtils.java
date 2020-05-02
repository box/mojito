package com.box.l10n.mojito.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DBUtils {

    //TODO(spring2) looks like this doesn't exist anymore
    @Value("${l10n.spring.jpa.database}")
    String driver;

    public boolean isHSQL() {
        return "HSQL".equals(driver);
    }

}
