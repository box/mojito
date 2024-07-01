package com.box.l10n.mojito;

/** Typed cache names to be used from @Cacheable annotations. */
public enum CacheType {
  DEFAULT(Names.DEFAULT),
  LOCALES(Names.LOCALES),
  PLURAL_FORMS(Names.PLURAL_FORMS),
  MACHINE_TRANSLATION(Names.MACHINE_TRANSLATION),
  AI_CHECKS(Names.AI_CHECKS);

  public static class Names {
    public static final String DEFAULT = "default";
    public static final String LOCALES = "locales";
    public static final String PLURAL_FORMS = "pluralForms";
    public static final String MACHINE_TRANSLATION = "machineTranslation";
    public static final String AI_CHECKS = "aiChecks";
  }

  CacheType(String cacheName) {
    if (!cacheName.equals(this.name())) throw new IllegalArgumentException();
  }
}
