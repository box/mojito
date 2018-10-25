package com.box.l10n.mojito.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DBUtils {

    @Value("${spring.jpa.database}")
    String driver;

    public boolean isHSQL() {
        return "HSQL".equals(driver);
    }

}
