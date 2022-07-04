package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jeanaurambault
 */
public class VirtualAssetServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = getLogger(VirtualAssetServiceTest.class);

    @Autowired
    VirtualAssetService virtualAssetService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetService assetService;

    @Autowired
    PluralFormService pluralFormService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testCreateVirtualAsset() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testCreateVirtualAsset"));
        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);
        Asset asset = assetRepository.findByPathAndRepositoryId("default", repository.getId());
        assertEquals("default", asset.getPath());
        assertEquals(false, asset.getDeleted());
    }

    @Test
    public void testUpdateVirtualAsset() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testUpdateVirtualAsset"));
        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);
        Asset asset = assetRepository.findByPathAndRepositoryId("default", repository.getId());
        assertEquals("default", asset.getPath());
        assertEquals(false, asset.getDeleted());

        virtualAsset.setDeleted(Boolean.TRUE);
        virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);
        asset = assetRepository.findByPathAndRepositoryId("default", repository.getId());
        assertEquals("default", asset.getPath());
        assertEquals(true, asset.getDeleted());
    }

    @Test(expected = VirtualAssetRequiredException.class)
    public void testCantUpdateAssetAsVirtual() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testCantUpdateAssetAsVirtual"));
        assetService.createAssetWithContent(repository.getId(), "default", "");
        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);
    }

    @Test
    public void testAddTextUnit() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testAddTextUnit"));
        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name_plural_other",
                "content plural",
                "comment plural",
                "other",
                "name_plural_other",
                false);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                true);

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(2, textUnits.size());

        int i = 0;
        assertEquals("name", textUnits.get(i).getName());
        assertEquals("content", textUnits.get(i).getContent());
        assertEquals("comment", textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());
        assertTrue(textUnits.get(i).getDoNotTranslate());

        i++;
        assertEquals("name_plural_other", textUnits.get(i).getName());
        assertEquals("content plural", textUnits.get(i).getContent());
        assertEquals("comment plural", textUnits.get(i).getComment());
        assertEquals("other", textUnits.get(i).getPluralForm());
        assertEquals("name_plural_other", textUnits.get(i).getPluralFormOther());
        assertFalse(textUnits.get(i).getDoNotTranslate());
    }

    @Test
    public void testGetTextunitWithDoNoTranslateFilter() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testGetTextunitWithDoNoTranslateFilter"));
        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name_plural_other",
                "content plural",
                "comment plural",
                "other",
                "name_plural_other",
                true);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                false);

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(2, textUnits.size());

        int i = 0;
        assertEquals("name", textUnits.get(i).getName());
        assertEquals("content", textUnits.get(i).getContent());
        assertEquals("comment", textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());
        assertFalse(textUnits.get(i).getDoNotTranslate());

        i++;
        assertEquals("name_plural_other", textUnits.get(i).getName());
        assertEquals("content plural", textUnits.get(i).getContent());
        assertEquals("comment plural", textUnits.get(i).getComment());
        assertEquals("other", textUnits.get(i).getPluralForm());
        assertEquals("name_plural_other", textUnits.get(i).getPluralFormOther());
        assertTrue(textUnits.get(i).getDoNotTranslate());

        List<VirtualAssetTextUnit> doNotTranslateTextUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), true);
        assertEquals(1, doNotTranslateTextUnits.size());

        i = 0;
        assertEquals("name_plural_other", doNotTranslateTextUnits.get(i).getName());
        assertEquals("content plural", doNotTranslateTextUnits.get(i).getContent());
        assertEquals("comment plural", doNotTranslateTextUnits.get(i).getComment());
        assertEquals("other", doNotTranslateTextUnits.get(i).getPluralForm());
        assertEquals("name_plural_other", doNotTranslateTextUnits.get(i).getPluralFormOther());
        assertTrue(doNotTranslateTextUnits.get(i).getDoNotTranslate());

        List<VirtualAssetTextUnit> translateTextUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), false);
        assertEquals(1, translateTextUnits.size());

        i = 0;
        assertEquals("name", translateTextUnits.get(i).getName());
        assertEquals("content", translateTextUnits.get(i).getContent());
        assertEquals("comment", translateTextUnits.get(i).getComment());
        assertNull(translateTextUnits.get(i).getPluralForm());
        assertNull(translateTextUnits.get(i).getPluralFormOther());
        assertFalse(translateTextUnits.get(i).getDoNotTranslate());
    }

    @Test
    public void testResetOfDoNoTranslateFilter() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testResetOfDoNoTranslateFilter"));
        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);
        Long virtualAssetId = virtualAsset.getId();

        virtualAssetService.addTextUnit(
                virtualAssetId,
                "name",
                "content",
                "comment",
                null,
                null,
                true);

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAssetId, null);
        assertEquals(1, textUnits.size());

        int i = 0;
        assertEquals("name", textUnits.get(i).getName());
        assertEquals("content", textUnits.get(i).getContent());
        assertEquals("comment", textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());
        assertTrue(textUnits.get(i).getDoNotTranslate());

        virtualAssetService.addTextUnit(
                virtualAssetId,
                "name",
                "content",
                "comment",
                null,
                null,
                false);

        List<VirtualAssetTextUnit> textUnits1 = virtualAssetService.getTextUnits(virtualAssetId, null);
        assertEquals(1, textUnits1.size());

        assertEquals("name", textUnits1.get(i).getName());
        assertEquals("content", textUnits1.get(i).getContent());
        assertEquals("comment", textUnits1.get(i).getComment());
        assertNull(textUnits1.get(i).getPluralForm());
        assertNull(textUnits1.get(i).getPluralFormOther());
        assertFalse(textUnits1.get(i).getDoNotTranslate());

        virtualAssetService.addTextUnit(
                virtualAssetId,
                "name",
                "content",
                "comment",
                null,
                null,
                true);

        List<VirtualAssetTextUnit> translateTextUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, translateTextUnits.size());

        assertEquals("name", translateTextUnits.get(i).getName());
        assertEquals("content", translateTextUnits.get(i).getContent());
        assertEquals("comment", translateTextUnits.get(i).getComment());
        assertNull(translateTextUnits.get(i).getPluralForm());
        assertNull(translateTextUnits.get(i).getPluralFormOther());
        assertTrue(translateTextUnits.get(i).getDoNotTranslate());
    }

    @Test(expected = VirtualAssetRequiredException.class)
    public void testCantAddTextUnitToNoVirtual() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testCantAddTextUnitToNoVirtual"));
        Asset asset = assetService.createAssetWithContent(repository.getId(), "default", "");

        virtualAssetService.addTextUnit(
                asset.getId(),
                "name_plural_other",
                "content plural",
                "comment plural",
                "other",
                "name_plural_other",
                false);
    }

    @Test
    public void testAddTextUnitVariant() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testAddTextUnitVariant"));
        RepositoryLocale repositoryLocaleFrFR = repositoryService.addRepositoryLocale(repository, "fr-FR");
        long frFRLocaleId = repositoryLocaleFrFR.getLocale().getId();

        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name_plural_other",
                "content plural",
                "comment plural",
                "other",
                "name_plural_other",
                false);

        TMTextUnitVariant addTextUnitVariant = virtualAssetService.addTextUnitVariant(virtualAsset.getId(), frFRLocaleId, "name_plural_other", "content-fr", "comment-fr");
        Assert.assertNotNull(addTextUnitVariant.getId());

        List<VirtualAssetTextUnit> loalizedTextUnits = virtualAssetService.getLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
        assertEquals(1, loalizedTextUnits.size());
        int i = 0;
        assertEquals("name_plural_other", loalizedTextUnits.get(i).getName());
        assertEquals("content-fr", loalizedTextUnits.get(i).getContent());
        assertEquals("comment plural", loalizedTextUnits.get(i).getComment());
        assertEquals("other", loalizedTextUnits.get(i).getPluralForm());
        assertEquals("name_plural_other", loalizedTextUnits.get(i).getPluralFormOther());
    }

    @Test
    public void testAddGetReplaceTextUnits() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testAddGetReplaceTextUnits"));

        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        List<VirtualAssetTextUnit> virtualAssetTextUnits = new ArrayList<>();

        VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name1");
        virtualAssetTextUnit.setContent("content1");
        virtualAssetTextUnit.setComment("comment1");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name2");
        virtualAssetTextUnit.setContent("content2");
        virtualAssetTextUnit.setComment("comment2");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name3_other");
        virtualAssetTextUnit.setContent("content3");
        virtualAssetTextUnit.setComment("comment3");
        virtualAssetTextUnit.setPluralForm("other");
        virtualAssetTextUnit.setPluralFormOther("name3_other");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetService.addTextUnits(virtualAsset.getId(), virtualAssetTextUnits).get();

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);

        assertEquals(3, textUnits.size());
        int i = 0;
        assertEquals("name1", textUnits.get(i).getName());
        assertEquals("content1", textUnits.get(i).getContent());
        assertEquals("comment1", textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());

        i++;
        assertEquals("name2", textUnits.get(i).getName());
        assertEquals("content2", textUnits.get(i).getContent());
        assertEquals("comment2", textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());

        i++;
        assertEquals("name3_other", textUnits.get(i).getName());
        assertEquals("content3", textUnits.get(i).getContent());
        assertEquals("comment3", textUnits.get(i).getComment());
        assertEquals("other", textUnits.get(i).getPluralForm());
        assertEquals("name3_other", textUnits.get(i).getPluralFormOther());

        List<VirtualAssetTextUnit> replaceVirtualAssetTextUnits = new ArrayList<>();
        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name1");
        virtualAssetTextUnit.setContent("content1");
        virtualAssetTextUnit.setComment("comment1");
        replaceVirtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name4");
        virtualAssetTextUnit.setContent("content4");
        virtualAssetTextUnit.setComment("comment4");
        replaceVirtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetService.replaceTextUnits(virtualAsset.getId(), replaceVirtualAssetTextUnits).get();

        List<VirtualAssetTextUnit> replacedTextUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);

        assertEquals(2, replacedTextUnits.size());
        i = 0;
        assertEquals("name1", replacedTextUnits.get(i).getName());
        assertEquals("content1", replacedTextUnits.get(i).getContent());
        assertEquals("comment1", replacedTextUnits.get(i).getComment());
        assertNull(replacedTextUnits.get(i).getPluralForm());
        assertNull(replacedTextUnits.get(i).getPluralFormOther());

        i++;
        assertEquals("name4", replacedTextUnits.get(i).getName());
        assertEquals("content4", replacedTextUnits.get(i).getContent());
        assertEquals("comment4", replacedTextUnits.get(i).getComment());
        assertNull(replacedTextUnits.get(i).getPluralForm());
        assertNull(replacedTextUnits.get(i).getPluralFormOther());
    }

    @Test
    public void testImportLocalizedTextUnits() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testImportLocalizedTextUnits"));
        RepositoryLocale repositoryLocaleFrFR = repositoryService.addRepositoryLocale(repository, "fr-FR");
        long frFRLocaleId = repositoryLocaleFrFR.getLocale().getId();

        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        List<VirtualAssetTextUnit> virtualAssetTextUnits = new ArrayList<>();

        VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name1");
        virtualAssetTextUnit.setContent("content1");
        virtualAssetTextUnit.setComment("comment1");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name2");
        virtualAssetTextUnit.setContent("content2");
        virtualAssetTextUnit.setComment("comment2");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name3_other");
        virtualAssetTextUnit.setContent("content3");
        virtualAssetTextUnit.setComment("comment3");
        virtualAssetTextUnit.setPluralForm("other");
        virtualAssetTextUnit.setPluralFormOther("name3_other");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetService.addTextUnits(virtualAsset.getId(), virtualAssetTextUnits).get();

        List<VirtualAssetTextUnit> localizedVirtualAssetTextUnits = new ArrayList<>();
        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name1");
        virtualAssetTextUnit.setContent("content1-fr");
        virtualAssetTextUnit.setComment("comment1-fr");
        localizedVirtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name2");
        virtualAssetTextUnit.setContent("content2-fr");
        virtualAssetTextUnit.setComment("comment2-fr");
        localizedVirtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name3_other");
        virtualAssetTextUnit.setContent("content3-fr");
        virtualAssetTextUnit.setComment("comment3-fr");
        virtualAssetTextUnit.setPluralForm("other");
        virtualAssetTextUnit.setPluralFormOther("name3_other");
        localizedVirtualAssetTextUnits.add(virtualAssetTextUnit);

        PollableFuture<Void> importLocalizedTextUnits = virtualAssetService.importLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, localizedVirtualAssetTextUnits);
        importLocalizedTextUnits.get();

        List<VirtualAssetTextUnit> loalizedTextUnits = virtualAssetService.getLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
        assertEquals(3, loalizedTextUnits.size());
        int i = 0;
        assertEquals("name1", loalizedTextUnits.get(i).getName());
        assertEquals("content1-fr", loalizedTextUnits.get(i).getContent());
        assertEquals("comment1", loalizedTextUnits.get(i).getComment());
        assertNull(loalizedTextUnits.get(i).getPluralForm());
        assertNull(loalizedTextUnits.get(i).getPluralFormOther());

        i++;
        assertEquals("name2", loalizedTextUnits.get(i).getName());
        assertEquals("content2-fr", loalizedTextUnits.get(i).getContent());
        assertEquals("comment2", loalizedTextUnits.get(i).getComment());
        assertNull(loalizedTextUnits.get(i).getPluralForm());
        assertNull(loalizedTextUnits.get(i).getPluralFormOther());

        i++;
        assertEquals("name3_other", loalizedTextUnits.get(i).getName());
        assertEquals("content3-fr", loalizedTextUnits.get(i).getContent());
        assertEquals("comment3", loalizedTextUnits.get(i).getComment());
        assertEquals("other", loalizedTextUnits.get(i).getPluralForm());
        assertEquals("name3_other", loalizedTextUnits.get(i).getPluralFormOther());
    }

    @Test
    public void testImportLocalizedTextUnitsOneMissing() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testImportLocalizedTextUnitsOneMissing"));
        RepositoryLocale repositoryLocaleFrFR = repositoryService.addRepositoryLocale(repository, "fr-FR");
        long frFRLocaleId = repositoryLocaleFrFR.getLocale().getId();

        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        List<VirtualAssetTextUnit> virtualAssetTextUnits = new ArrayList<>();

        VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name1");
        virtualAssetTextUnit.setContent("content1");
        virtualAssetTextUnit.setComment("comment1");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name2");
        virtualAssetTextUnit.setContent("content2");
        virtualAssetTextUnit.setComment("comment2");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name3_other");
        virtualAssetTextUnit.setContent("content3");
        virtualAssetTextUnit.setComment("comment3");
        virtualAssetTextUnit.setPluralForm("other");
        virtualAssetTextUnit.setPluralFormOther("name3_other");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetService.addTextUnits(virtualAsset.getId(), virtualAssetTextUnits).get();

        List<VirtualAssetTextUnit> localizedVirtualAssetTextUnits = new ArrayList<>();

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name2");
        virtualAssetTextUnit.setContent("content2-fr");
        virtualAssetTextUnit.setComment("comment2-fr");
        localizedVirtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name3_other");
        virtualAssetTextUnit.setContent("content3-fr");
        virtualAssetTextUnit.setComment("comment3-fr");
        virtualAssetTextUnit.setPluralForm("other");
        virtualAssetTextUnit.setPluralFormOther("name3_other");
        localizedVirtualAssetTextUnits.add(virtualAssetTextUnit);

        PollableFuture<Void> importLocalizedTextUnits = virtualAssetService.importLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, localizedVirtualAssetTextUnits);
        importLocalizedTextUnits.get();

        List<VirtualAssetTextUnit> loalizedTextUnits = virtualAssetService.getLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
        assertEquals(2, loalizedTextUnits.size());
        int i = 0;
        assertEquals("name2", loalizedTextUnits.get(i).getName());
        assertEquals("content2-fr", loalizedTextUnits.get(i).getContent());
        assertEquals("comment2", loalizedTextUnits.get(i).getComment());
        assertNull(loalizedTextUnits.get(i).getPluralForm());
        assertNull(loalizedTextUnits.get(i).getPluralFormOther());

        i++;
        assertEquals("name3_other", loalizedTextUnits.get(i).getName());
        assertEquals("content3-fr", loalizedTextUnits.get(i).getContent());
        assertEquals("comment3", loalizedTextUnits.get(i).getComment());
        assertEquals("other", loalizedTextUnits.get(i).getPluralForm());
        assertEquals("name3_other", loalizedTextUnits.get(i).getPluralFormOther());
    }

    @Test
    public void testDeleteTextUnit() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testDeleteTextUnit"));
        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name_plural_other",
                "content plural",
                "comment plural",
                "other",
                "name_plural_other",
                false);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                false);

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(2, textUnits.size());

        virtualAssetService.deleteTextUnit(virtualAsset.getId(), "name_plural_other");

        textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, textUnits.size());
        assertEquals("name", textUnits.get(0).getName());
    }

    @Test
    public void testLeveragingByName() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testLeveragingByName"));
        RepositoryLocale repositoryLocaleFrFR = repositoryService.addRepositoryLocale(repository, "fr-FR");
        long frFRLocaleId = repositoryLocaleFrFR.getLocale().getId();

        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                false);

        TMTextUnitVariant addTextUnitVariant = virtualAssetService.addTextUnitVariant(
                virtualAsset.getId(),
                frFRLocaleId,
                "name",
                "content-fr",
                null);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "new content",
                "comment",
                null,
                null,
                false);

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, textUnits.size());
        assertEquals("name", textUnits.get(0).getName());
        assertEquals("new content", textUnits.get(0).getContent());

        List<VirtualAssetTextUnit> localizedTextUnits = virtualAssetService.getLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
        assertEquals(1, localizedTextUnits.size());
        assertEquals("name", localizedTextUnits.get(0).getName());
        assertEquals("content-fr", localizedTextUnits.get(0).getContent());

        VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name");
        virtualAssetTextUnit.setContent("new 2 content");
        virtualAssetService.replaceTextUnits(virtualAsset.getId(), Arrays.asList(virtualAssetTextUnit)).get();

        textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, textUnits.size());
        assertEquals("name", textUnits.get(0).getName());
        assertEquals("new 2 content", textUnits.get(0).getContent());

        localizedTextUnits = virtualAssetService.getLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
        assertEquals(1, localizedTextUnits.size());
        assertEquals("name", localizedTextUnits.get(0).getName());
        assertEquals("content-fr", localizedTextUnits.get(0).getContent());
    }

    @Test
    public void testLeveragingByContent() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testLeveragingByContent"));
        RepositoryLocale repositoryLocaleFrFR = repositoryService.addRepositoryLocale(repository, "fr-FR");
        long frFRLocaleId = repositoryLocaleFrFR.getLocale().getId();

        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                false);

        TMTextUnitVariant addTextUnitVariant = virtualAssetService.addTextUnitVariant(
                virtualAsset.getId(),
                frFRLocaleId,
                "name",
                "content-fr",
                null);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "new name",
                "content",
                "comment",
                null,
                null,
                false);

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(2, textUnits.size());
        assertEquals("name", textUnits.get(0).getName());
        assertEquals("content", textUnits.get(0).getContent());

        assertEquals("new name", textUnits.get(1).getName());
        assertEquals("content", textUnits.get(1).getContent());

        List<VirtualAssetTextUnit> localizedTextUnits = virtualAssetService.getLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
        assertEquals(2, localizedTextUnits.size());
        assertEquals("name", localizedTextUnits.get(0).getName());
        assertEquals("content-fr", localizedTextUnits.get(0).getContent());

        assertEquals("new name", localizedTextUnits.get(1).getName());
        assertEquals("content-fr", localizedTextUnits.get(1).getContent());
    }

    @Test
    public void testLeveragingByNameBatchBatch() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testLeveragingByName"));
        RepositoryLocale repositoryLocaleFrFR = repositoryService.addRepositoryLocale(repository, "fr-FR");
        long frFRLocaleId = repositoryLocaleFrFR.getLocale().getId();

        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                false);

        TMTextUnitVariant addTextUnitVariant = virtualAssetService.addTextUnitVariant(
                virtualAsset.getId(),
                frFRLocaleId,
                "name",
                "content-fr",
                null);

        VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name");
        virtualAssetTextUnit.setContent("new content");
        virtualAssetTextUnit.setComment("comment");

        virtualAssetService.addTextUnits(virtualAsset.getId(), Arrays.asList(virtualAssetTextUnit)).get();

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, textUnits.size());
        assertEquals("name", textUnits.get(0).getName());
        assertEquals("new content", textUnits.get(0).getContent());

        List<VirtualAssetTextUnit> localizedTextUnits = virtualAssetService.getLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
        assertEquals(1, localizedTextUnits.size());
        assertEquals("name", localizedTextUnits.get(0).getName());
        assertEquals("content-fr", localizedTextUnits.get(0).getContent());

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name");
        virtualAssetTextUnit.setContent("new 2 content");
        virtualAssetService.replaceTextUnits(virtualAsset.getId(), Arrays.asList(virtualAssetTextUnit)).get();

        textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, textUnits.size());
        assertEquals("name", textUnits.get(0).getName());
        assertEquals("new 2 content", textUnits.get(0).getContent());

        localizedTextUnits = virtualAssetService.getLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
        assertEquals(1, localizedTextUnits.size());
        assertEquals("name", localizedTextUnits.get(0).getName());
        assertEquals("content-fr", localizedTextUnits.get(0).getContent());
    }

    @Test
    public void testLeveragingByContentBatch() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testLeveragingByContent"));
        RepositoryLocale repositoryLocaleFrFR = repositoryService.addRepositoryLocale(repository, "fr-FR");
        long frFRLocaleId = repositoryLocaleFrFR.getLocale().getId();

        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                false);

        TMTextUnitVariant addTextUnitVariant = virtualAssetService.addTextUnitVariant(
                virtualAsset.getId(),
                frFRLocaleId,
                "name",
                "content-fr",
                null);

        VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("new name");
        virtualAssetTextUnit.setContent("content");
        virtualAssetTextUnit.setComment("comment");

        virtualAssetService.addTextUnits(virtualAsset.getId(), Arrays.asList(virtualAssetTextUnit)).get();

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(2, textUnits.size());
        assertEquals("name", textUnits.get(0).getName());
        assertEquals("content", textUnits.get(0).getContent());

        assertEquals("new name", textUnits.get(1).getName());
        assertEquals("content", textUnits.get(1).getContent());

        List<VirtualAssetTextUnit> localizedTextUnits = virtualAssetService.getLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
        assertEquals(2, localizedTextUnits.size());
        assertEquals("name", localizedTextUnits.get(0).getName());
        assertEquals("content-fr", localizedTextUnits.get(0).getContent());

        assertEquals("new name", localizedTextUnits.get(1).getName());
        assertEquals("content-fr", localizedTextUnits.get(1).getContent());
    }

    @Test
    public void testRemovePreviousWhenExactMatches() throws Exception {
        
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testRemovePreviousWhenExactMatches"));
        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                true);
        
        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, textUnits.size());

        int i = 0;
        assertEquals("name", textUnits.get(i).getName());
        assertEquals("content", textUnits.get(i).getContent());
        assertEquals("comment", textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());
        assertTrue(textUnits.get(i).getDoNotTranslate());
        
        
        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                null,
                null,
                null,
                true);

        textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, textUnits.size());

        i = 0;
        assertEquals("name", textUnits.get(i).getName());
        assertEquals("content", textUnits.get(i).getContent());
        assertNull(textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());
        assertTrue(textUnits.get(i).getDoNotTranslate());
        
        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                true);

        textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, textUnits.size());

        i = 0;
        assertEquals("name", textUnits.get(i).getName());
        assertEquals("content", textUnits.get(i).getContent());
        assertEquals("comment", textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());
        assertTrue(textUnits.get(i).getDoNotTranslate());
    }


    @Test
    public void testChangeDoNotTranslateWhenExactMatches() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testChangeDoNotTranslateWhenExactMatches"));
        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                true);

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, textUnits.size());

        int i = 0;
        assertEquals("name", textUnits.get(i).getName());
        assertEquals("content", textUnits.get(i).getContent());
        assertEquals("comment", textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());
        assertTrue(textUnits.get(i).getDoNotTranslate());

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null,
                false);

        textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);
        assertEquals(1, textUnits.size());

        i = 0;
        assertEquals("name", textUnits.get(i).getName());
        assertEquals("content", textUnits.get(i).getContent());
        assertEquals("comment", textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());
        assertFalse(textUnits.get(i).getDoNotTranslate());
    }

    @Test
    public void testFetchAllPluralForm() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testFetchAllPluralForm"));

        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setRepositoryId(repository.getId());
        virtualAsset.setPath("default");
        virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);

        List<VirtualAssetTextUnit> virtualAssetTextUnits = new ArrayList<>();

        VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name1_other");
        virtualAssetTextUnit.setContent("content1");
        virtualAssetTextUnit.setComment("comment1");
        virtualAssetTextUnit.setPluralForm("other");
        virtualAssetTextUnit.setPluralFormOther("name1_other");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName("name1_zero");
        virtualAssetTextUnit.setContent("content1");
        virtualAssetTextUnit.setComment("comment1");
        virtualAssetTextUnit.setPluralForm("zero");
        virtualAssetTextUnit.setPluralFormOther("name1_other");
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualAssetService.addTextUnits(virtualAsset.getId(), virtualAssetTextUnits).get();
        virtualAssetService.addTextUnits(virtualAsset.getId(), virtualAssetTextUnits).get();

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId(), null);

        assertEquals(2, textUnits.size());
        int i = 0;

        assertEquals("name1_other", textUnits.get(i).getName());
        assertEquals("content1", textUnits.get(i).getContent());
        assertEquals("comment1", textUnits.get(i).getComment());
        assertEquals("other", textUnits.get(i).getPluralForm());
        assertEquals("name1_other", textUnits.get(i).getPluralFormOther());

        i++;
        assertEquals("name1_zero", textUnits.get(i).getName());
        assertEquals("content1", textUnits.get(i).getContent());
        assertEquals("comment1", textUnits.get(i).getComment());
        assertEquals("zero", textUnits.get(i).getPluralForm());
        assertEquals("name1_other", textUnits.get(i).getPluralFormOther());
    }

}
