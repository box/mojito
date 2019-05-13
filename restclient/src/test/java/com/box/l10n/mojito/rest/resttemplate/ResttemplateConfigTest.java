package com.box.l10n.mojito.rest.resttemplate;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author jaurambault
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ComponentScan(basePackageClasses = {ResttemplateConfig.class})
@ContextConfiguration(classes = {ResttemplateConfigTest.class})
@EnableConfigurationProperties
public class ResttemplateConfigTest {

    @Autowired
    ResttemplateConfig resttemplateConfig;
    
    @Autowired
    FormLoginConfig formLoginConfig;

    @Test
    public void testConfig() {
        assertEquals("localhost", resttemplateConfig.getHost());
        assertEquals(new Integer(8080), resttemplateConfig.getPort());
        assertEquals("http", resttemplateConfig.getScheme());
        assertEquals("admin", resttemplateConfig.getAuthentication().getUsername());
        assertEquals("ChangeMe", resttemplateConfig.getAuthentication().getPassword());
        assertEquals("", resttemplateConfig.getContextPath());
    }

}
