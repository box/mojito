package com.box.l10n.mojito.aspect.security;

import org.aspectj.lang.Aspects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration; 

/**
 *
 * @author jaurambault
 */
@Configuration
public class RunAsAspectConfig {
    
    @Bean
    RunAsAspect getRunAsAspect() {
        return Aspects.aspectOf(RunAsAspect.class);
    }
}
