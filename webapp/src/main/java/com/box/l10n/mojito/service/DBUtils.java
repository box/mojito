package com.box.l10n.mojito.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DBUtils {

    //TODO(spring2) looks like old prop doesn't exist anymore so look at the URL
    @Value("${spring.datasource.url}")
    String url;

    public boolean isHSQL() {
        return url.contains("hsql");
    }

}
