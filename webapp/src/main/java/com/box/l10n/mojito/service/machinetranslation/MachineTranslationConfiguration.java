package com.box.l10n.mojito.service.machinetranslation;

import com.box.l10n.mojito.service.machinetranslation.microsoft.MicrosoftMTEngine;
import com.box.l10n.mojito.service.machinetranslation.microsoft.MicrosoftMTEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the implementation for {@link MachineTranslationEngine} The default implementation is
 * the {@link NoOpEngine} which is a stub. Consider implementing & configuring an actual MT Engine
 * using the "l10n.mt.impl" property.
 *
 * @author garion
 */
@Configuration
public class MachineTranslationConfiguration {

  static Logger logger = LoggerFactory.getLogger(MachineTranslationConfiguration.class);

  @ConditionalOnProperty(value = "l10n.mt.impl", havingValue = "MicrosoftMTEngine")
  @Configuration
  static class MicrosoftEngineConfiguration {

    final MicrosoftMTEngineConfiguration microsoftMTEngineConfiguration;
    final PlaceholderEncoder placeholderEncoder;

    public MicrosoftEngineConfiguration(
        MicrosoftMTEngineConfiguration microsoftMTEngineConfiguration,
        PlaceholderEncoder placeholderEncoder) {
      this.microsoftMTEngineConfiguration = microsoftMTEngineConfiguration;
      this.placeholderEncoder = placeholderEncoder;
    }

    @Bean
    public MicrosoftMTEngine microsoftMTEngine() {
      logger.info("Configure microsoftMTEngine");
      return new MicrosoftMTEngine(microsoftMTEngineConfiguration, placeholderEncoder);
    }
  }

  @ConditionalOnProperty(value = "l10n.mt.impl", havingValue = "NoOpEngine", matchIfMissing = true)
  @Configuration
  static class NoOpEngineConfiguration {

    @Bean
    public NoOpEngine noOpEngine() {
      logger.info("Configure noOpEngine");
      return new NoOpEngine();
    }
  }
}
