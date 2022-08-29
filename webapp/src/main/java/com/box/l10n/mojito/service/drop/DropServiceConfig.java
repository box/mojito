package com.box.l10n.mojito.service.drop;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * To configure the service that import/export drops.
 *
 * @author jaurambault
 */
@Component
@ConfigurationProperties("l10n.drop-service")
public class DropServiceConfig {

  /** Week offset (from the current week) used when computing the drop name. */
  int dropNameWeekOffset = 0;

  /**
   * User for imported translations If this is not set, then the authenticated user in context who
   * is importing the drop is used for all imported translations
   */
  String dropImporterUsername;

  public int getDropNameWeekOffset() {
    return dropNameWeekOffset;
  }

  public void setDropNameWeekOffset(int dropNameWeekOffset) {
    this.dropNameWeekOffset = dropNameWeekOffset;
  }

  public String getDropImporterUsername() {
    return dropImporterUsername;
  }

  public void setDropImporterUsername(String dropImporterUsername) {
    this.dropImporterUsername = dropImporterUsername;
  }
}
