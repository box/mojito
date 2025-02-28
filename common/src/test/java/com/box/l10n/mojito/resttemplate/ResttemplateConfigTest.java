package com.box.l10n.mojito.resttemplate;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author jaurambault
 */
@RunWith(SpringRunner.class)
@ComponentScan(
    basePackageClasses = {
      ResttemplateConfig.class,
    })
@SpringBootTest(classes = {ResttemplateConfigTest.class})
@EnableConfigurationProperties
public class ResttemplateConfigTest {

  @Autowired ResttemplateConfig resttemplateConfig;

  @Autowired FormLoginConfig formLoginConfig;

  @Test
  public void testConfig() {
    assertEquals("localhost", resttemplateConfig.getHost());
    assertEquals(Integer.valueOf(8080), resttemplateConfig.getPort());
    assertEquals("http", resttemplateConfig.getScheme());
    assertEquals("admin", resttemplateConfig.getAuthentication().getUsername());
    assertEquals("ChangeMe", resttemplateConfig.getAuthentication().getPassword());
    assertEquals("", resttemplateConfig.getContextPath());
  }
}
