package com.box.l10n.mojito.rest.sometest;


import com.box.l10n.mojito.rest.sometest2.SomeRepository2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SomeTest.TestConfig.class,
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SomeTest {

    @Autowired
    @Qualifier("bla")
    String bla;

    @Autowired
    SomeRepository someRepository;

    @Autowired
    SomeRepository2 someRepository2;

    @LocalServerPort
    int serverPort;

    @Test
    public void some() {
        System.out.println("server port: " + serverPort);
        System.out.println("some test:" + bla);
    }

    @SpringBootApplication(exclude = QuartzAutoConfiguration.class)
    @EnableJpaRepositories(basePackageClasses = {SomeTest.TestConfig.class, SomeRepository2.class})
    public static class TestConfig {

        @Bean("bla")
        public String bla() {
            return "bla";
        }
    }

}


