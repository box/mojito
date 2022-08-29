package com.box.l10n.mojito.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      AmazonS3ConfigurationTest.class,
      AmazonS3ConfigurationProperties.class,
      AmazonS3Configuration.class
    })
@EnableConfigurationProperties
public class AmazonS3ConfigurationTest {

  static Logger logger = LoggerFactory.getLogger(AmazonS3ConfigurationTest.class);

  @Autowired(required = false)
  AmazonS3 amazonS3;

  @Autowired AmazonS3ConfigurationProperties amazonS3ConfigurationProperties;

  @Before
  public void before() {
    Assume.assumeNotNull(amazonS3);
  }

  @Ignore("Placeholder to test amazon client")
  @Test
  public void testPutString() {
    amazonS3.putObject("change-me", "testkey", "काहीतरी");
  }
}
