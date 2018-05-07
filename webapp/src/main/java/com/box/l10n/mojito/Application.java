package com.box.l10n.mojito;

import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.json.ObjectMapper;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.system.ApplicationPidFileWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan(basePackageClasses = Application.class)
@EnableAutoConfiguration
@EnableSpringConfigured
@EnableJpaAuditing
@EnableJpaRepositories
@EnableScheduling
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@EnableRetry
@EntityScan(basePackageClasses = BaseEntity.class)
public class Application {

    @Value("${org.springframework.http.converter.json.indent_output}")
    boolean shouldIndentJacksonOutput;

    public static void main(String[] args) throws IOException {

        SpringApplication springApplication = new SpringApplication(Application.class);
        springApplication.addListeners(new ApplicationPidFileWriter("application.pid"));
        springApplication.run(args);
    }

    /**
     * Fix Spring scanning issue.
     *
     * without this the ObjectMapper instance is not created/available in the
     * container.
     *
     * @return
     */
    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * Configuration Jackson ObjectMapper
     *
     * @return
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter mjhmc = new MappingJackson2HttpMessageConverter();

        Jackson2ObjectMapperFactoryBean jomfb = new Jackson2ObjectMapperFactoryBean();
        jomfb.setAutoDetectFields(false);
        jomfb.setIndentOutput(shouldIndentJacksonOutput);
        jomfb.afterPropertiesSet();

        mjhmc.setObjectMapper(jomfb.getObject());
        return mjhmc;
    }
}
