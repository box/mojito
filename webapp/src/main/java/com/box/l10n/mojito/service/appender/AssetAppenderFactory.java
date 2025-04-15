package com.box.l10n.mojito.service.appender;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AssetAppenderFactory {

  public Optional<AbstractAssetAppender> fromExtension(String extension, String sourceConent) {
    if (extension == null) return Optional.empty();

    switch (extension.toLowerCase()) {
      case "pot":
        return Optional.of(new POTAssetAppender(sourceConent));
      default:
        return Optional.empty();
    }
  }
}
