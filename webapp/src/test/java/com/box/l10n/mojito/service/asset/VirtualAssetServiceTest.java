package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
        assetService.createAsset(repository.getId(), "", "default");
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
                pluralFormService.findByPluralFormString("other"),
                "name_plural_other");

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null);

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId());
        assertEquals(2, textUnits.size());

        int i = 0;
        assertEquals("name", textUnits.get(i).getName());
        assertEquals("content", textUnits.get(i).getContent());
        assertEquals("comment", textUnits.get(i).getComment());
        assertNull(textUnits.get(i).getPluralForm());
        assertNull(textUnits.get(i).getPluralFormOther());

        i++;
        assertEquals("name_plural_other", textUnits.get(i).getName());
        assertEquals("content plural", textUnits.get(i).getContent());
        assertEquals("comment plural", textUnits.get(i).getComment());
        assertEquals("other", textUnits.get(i).getPluralForm());
        assertEquals("name_plural_other", textUnits.get(i).getPluralFormOther());
    }

    @Test(expected = VirtualAssetRequiredException.class)
    public void testCantAddTextUnitToNoVirtual() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testCantAddTextUnitToNoVirtual"));
        Asset asset = assetService.createAsset(repository.getId(), "", "default");

        virtualAssetService.addTextUnit(
                asset.getId(),
                "name_plural_other",
                "content plural",
                "comment plural",
                pluralFormService.findByPluralFormString("other"),
                "name_plural_other");
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
                pluralFormService.findByPluralFormString("other"),
                "name_plural_other");

        TMTextUnitVariant addTextUnitVariant = virtualAssetService.addTextUnitVariant(virtualAsset.getId(), frFRLocaleId, "name_plural_other", "content-fr", "comment-fr");
        Assert.assertNotNull(addTextUnitVariant.getId());

        List<VirtualAssetTextUnit> loalizedTextUnits = virtualAssetService.getLoalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
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
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testAddTextUnitVariant"));

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

        virtualAssetService.addTextUnits(virtualAssetTextUnits, virtualAsset.getId());

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId());

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

        virtualAssetService.replaceTextUnits(virtualAsset.getId(), replaceVirtualAssetTextUnits);

        List<VirtualAssetTextUnit> replacedTextUnits = virtualAssetService.getTextUnits(virtualAsset.getId());

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
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("testAddTextUnitVariant"));
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

        virtualAssetService.addTextUnits(virtualAssetTextUnits, virtualAsset.getId());

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

        virtualAssetService.importLocalizedTextUnits(virtualAsset.getId(), frFRLocaleId, localizedVirtualAssetTextUnits);

        List<VirtualAssetTextUnit> loalizedTextUnits = virtualAssetService.getLoalizedTextUnits(virtualAsset.getId(), frFRLocaleId, InheritanceMode.REMOVE_UNTRANSLATED);
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
    public void testDeleteTextUnit() throws Exception {

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
                pluralFormService.findByPluralFormString("other"),
                "name_plural_other");

        virtualAssetService.addTextUnit(
                virtualAsset.getId(),
                "name",
                "content",
                "comment",
                null,
                null);

        List<VirtualAssetTextUnit> textUnits = virtualAssetService.getTextUnits(virtualAsset.getId());
        assertEquals(2, textUnits.size());

        virtualAssetService.deleteTextUnit(virtualAsset.getId(), "name_plural_other");

        textUnits = virtualAssetService.getTextUnits(virtualAsset.getId());
        assertEquals(1, textUnits.size());
        assertEquals("name", textUnits.get(0).getName());
    }
}
