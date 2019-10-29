package com.box.l10n.mojito.service.bla;

import com.box.l10n.mojito.Application;
import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.Repository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author jaurambault
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {BlaServiceTest.class, Application.class, Repository.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"fromtest=fromtestvalue", "blaformconfig=blaformconfigtest"}
        )
@SpringBootApplication
@EntityScan(basePackageClasses = BaseEntity.class)
public class BlaServiceTest {

    static Logger logger = LoggerFactory.getLogger(BlaServiceTest.class);

    @Value("${fromtest}")
    String somevalue;

    @Value("${blaformconfig}")
    String info;

//    @Autowired
//    private WebTestClient webClient;

//    @Autowired
//    private ApplicationContext applicationContext;

    @Autowired
    SomeClass someClass;

    @LocalServerPort
    int port;

    @Test
    public void testBla() throws Exception {
        logger.info("here we go: {}, info: {}, port: {}", somevalue, info, port);
        logger.info("someclass: {}", someClass);
    }
}