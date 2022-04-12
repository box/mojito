package com.box.l10n.mojito.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DBUtils {

    @Value("${spring.datasource.url}")
    String url;

    public boolean isMysql() {
        return url.contains("mysql");
    }

    public boolean isHsql() {
        return url.contains("hsqldb");
    }
}
