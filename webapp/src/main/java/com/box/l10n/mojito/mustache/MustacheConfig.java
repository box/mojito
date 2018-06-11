package com.box.l10n.mojito.mustache;

import com.samskivert.mustache.Mustache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure the Mustache compiler.
 *
 * @author jaurambault
 */
@Configuration
public class MustacheConfig {

    /**
     * Configure Mustache compiler to return empty string for null values.
     *
     * @return customized Mustache compiler
     */
    @Bean
    Mustache.Compiler getMustache() {
        return Mustache.compiler().nullValue("");
    }
}
