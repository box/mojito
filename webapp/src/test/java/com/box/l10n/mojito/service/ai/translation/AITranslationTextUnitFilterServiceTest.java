package com.box.l10n.mojito.service.ai.translation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AITranslationTextUnitFilterServiceTest {

  AITranslationTextUnitFilterService textUnitFilterService;

  Asset testAsset;

  Repository repository;

  @Before
  public void setUp() {
    testAsset = Mockito.mock(Asset.class);
    repository = Mockito.mock(Repository.class);
    when(testAsset.getRepository()).thenReturn(repository);
    when(repository.getName()).thenReturn("test");
    textUnitFilterService = new AITranslationTextUnitFilterService();
    AITranslationFilterConfiguration translationFilterConfiguration =
        new AITranslationFilterConfiguration();
    setTestParameters(true, true, true);
    textUnitFilterService.excludePlaceholdersPatternMap =
        Map.of("test", Pattern.compile("\\{[^\\}]*\\}"));
  }

  @Test
  public void testIsTranslatable() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setName("test");
    tmTextUnit.setContent("test content");
    tmTextUnit.setAsset(testAsset);
    assertTrue(textUnitFilterService.isTranslatable(tmTextUnit, repository));
  }

  @Test
  public void testIsTranslatableWithPlural() {
    setTestParameters(true, false, false);
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setName("test");
    tmTextUnit.setContent("test content");
    tmTextUnit.setAsset(testAsset);

    PluralForm otherPluralForm = new PluralForm();
    otherPluralForm.setName("other");
    tmTextUnit.setPluralForm(otherPluralForm);
    assertFalse(textUnitFilterService.isTranslatable(tmTextUnit, repository));
  }

  @Test
  public void testIsTranslatableWithPlaceholder() {
    setTestParameters(false, true, false);
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setName("test");
    tmTextUnit.setContent("test {content}");
    tmTextUnit.setAsset(testAsset);
    assertFalse(textUnitFilterService.isTranslatable(tmTextUnit, repository));
  }

  @Test
  public void testIsTranslatableWithHtmlTags() {
    setTestParameters(false, false, true);
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setName("test");
    tmTextUnit.setContent("test <b>content</b>");
    tmTextUnit.setAsset(testAsset);
    assertFalse(textUnitFilterService.isTranslatable(tmTextUnit, repository));
  }

  @Test
  public void testIsTranslatableWithHtmlTagsAndPlaceholders() {
    setTestParameters(false, true, true);
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setName("test");
    tmTextUnit.setContent("test <b>{content}</b>");
    tmTextUnit.setAsset(testAsset);
    assertFalse(textUnitFilterService.isTranslatable(tmTextUnit, repository));
  }

  @Test
  public void isTranslatableShouldReturnTrueWhenAllExclusionsAreFalse() {
    setTestParameters(false, false, false);

    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setContent("Text with <b>html</b> and {placeholder}, including plurals.");
    tmTextUnit.setAsset(testAsset);
    assertTrue(textUnitFilterService.isTranslatable(tmTextUnit, repository));
  }

  @Test
  public void isTranslatableShouldReturnFalseForTextWithMultipleExclusions() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setContent("This text has <b>html</b>, {placeholder}, and could be a plural form.");
    tmTextUnit.setAsset(testAsset);
    tmTextUnit.setPluralForm(new PluralForm());
    assertFalse(textUnitFilterService.isTranslatable(tmTextUnit, repository));
  }

  @Test
  public void isTranslatableShouldReturnTrueWhenNoExclusionEnabled() {
    setTestParameters(false, false, false);

    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setPluralForm(new PluralForm());
    tmTextUnit.setContent("Text with <b>html</b> and {placeholder}");
    tmTextUnit.setAsset(testAsset);
    assertTrue(textUnitFilterService.isTranslatable(tmTextUnit, repository));
  }

  @Test
  public void isTranslatableTrueWhenNoRepositoryConfig() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setContent("Text with <b>html</b> and {placeholder}");
    tmTextUnit.setAsset(testAsset);
    textUnitFilterService.aiTranslationFilterConfiguration = new AITranslationFilterConfiguration();
    assertTrue(textUnitFilterService.isTranslatable(tmTextUnit, repository));
  }

  private void setTestParameters(
      boolean excludePlurals, boolean excludePlaceholders, boolean excludeHtmlTags) {
    AITranslationFilterConfiguration translationFilterConfiguration =
        new AITranslationFilterConfiguration();
    AITranslationFilterConfiguration.RepositoryConfig repositoryConfig =
        new AITranslationFilterConfiguration.RepositoryConfig();
    repositoryConfig.setExcludePlurals(excludePlurals);
    repositoryConfig.setExcludePlaceholders(excludePlaceholders);
    repositoryConfig.setExcludeHtmlTags(excludeHtmlTags);
    repositoryConfig.setExcludePlaceholdersRegex("\\{[^\\}]*\\}");
    translationFilterConfiguration.setRepositoryConfig(Map.of("test", repositoryConfig));

    textUnitFilterService.aiTranslationFilterConfiguration = translationFilterConfiguration;
    textUnitFilterService.excludePlaceholdersPatternMap =
        Map.of("test", Pattern.compile("\\{[^\\}]*\\}"));
  }
}
