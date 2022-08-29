package com.box.l10n.mojito.cli;

import com.box.l10n.mojito.cli.command.L10nJCommander;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.xml.XmlParsingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;

@EnableSpringConfigured
@ComponentScan(basePackages = "com.box.l10n.mojito")
@SpringBootApplication
public class App implements CommandLineRunner {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(App.class);

  /**
   * Application entry point.
   *
   * @param args
   */
  public static void main(String[] args) {

    XmlParsingConfiguration.disableXPathLimits();

    new SpringApplicationBuilder(App.class)
        .web(WebApplicationType.NONE)
        .bannerMode(Banner.Mode.OFF)
        .run(args);
  }

  @Override
  public void run(String... args) throws Exception {
    new L10nJCommander().run(args);
  }

  @Bean(name = "outputIndented")
  public ObjectMapper getOutputIndented() {
    return ObjectMapper.withIndentedOutput();
  }
}
