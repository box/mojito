package com.box.l10n.mojito.service.tm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.hibernate.proxy.HibernateProxy;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.ArrayList;

/**
 * @author jaurambault
 */
public class TMTextUnitHistoryServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TMTextUnitHistoryServiceTest.class);

    @Autowired
    TMService tmService;

    @Autowired
    TMTextUnitHistoryService tmHistoryService;

    /*
    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;
    */

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetService assetService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    Repository repository;
    Asset asset;
    Long tmId;
    Long assetId;

    protected void createTestData() throws RepositoryNameAlreadyUsedException {
        logger.debug("Create data for test");
        if (repository == null) {
            repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

            try {
                repositoryService.addRepositoryLocale(repository, "fr-FR");
                repositoryService.addRepositoryLocale(repository, "fr-CA");
            } catch (RepositoryLocaleCreationException e) {
                throw new RuntimeException(e);
            }

            asset = assetService.createAssetWithContent(repository.getId(), "test-asset-path.xliff", "test asset content");

            //make sure asset and its relationships are loaded
            asset = assetRepository.findOne(asset.getId());

            assetId = asset.getId();
            tmId = repository.getTm().getId();
        }
    }

    @Test
    public void testEmptyHistory() throws RepositoryNameAlreadyUsedException {
        createTestData();

        logger.debug("Done creating data for test, start testing");

        Long addTextUnitAndCheck1 = addTextUnitAndCheck(tmId, assetId, "name", "this is the content", "some comment", "3063c39d3cf8ab69bcabbbc5d7187dc9", "cf8ea6b6848f23345648038bc3abf324");

        Locale frFRLocale = localeService.findByBcp47Tag("fr-FR");

        // then get the history of it without adding any variants
        List<TMTextUnitVariant> history = tmHistoryService.findHistory(frFRLocale.getId(), addTextUnitAndCheck1);
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    public void testHistoryOneVariant() throws RepositoryNameAlreadyUsedException {
        createTestData();

        logger.debug("Done creating data for test, start testing");

        Long addTextUnitAndCheck1 = addTextUnitAndCheck(tmId, assetId, "name", "this is the content", "some comment", "3063c39d3cf8ab69bcabbbc5d7187dc9", "cf8ea6b6848f23345648038bc3abf324");

        logger.debug("Add a current translation for french");
        Locale frFRLocale = localeService.findByBcp47Tag("fr-FR");

        logger.debug("TMTextUnit tmTextUnit = tmTextUnitRepository.findByMd5AndTmIdAndAssetId(md5, assetId, tmId);");
        String md5 = tmService.computeTMTextUnitMD5("name", "this is the content", "some comment");
        TMTextUnit tmTextUnit = tmTextUnitRepository.findByMd5AndTmIdAndAssetId(md5, tmId, assetId);
        logger.debug("tmtextunit: {}", tmTextUnit);

        TMTextUnitVariant addCurrentTMTextUnitVariant = addCurrentTMTextUnitVariant(tmTextUnit.getId(), frFRLocale.getId(), "FR[this is the content]", "0a30a359b20fd4095fc17fb586e8db4d");

        // then get the history of it without adding any variants
        List<TMTextUnitVariant> history = tmHistoryService.findHistory(frFRLocale.getId(), addTextUnitAndCheck1);
        assertNotNull(history);
        assertFalse(history.isEmpty());

        Iterator<TMTextUnitVariant> iterator = history.iterator();
        assertTrue(iterator.hasNext());

        TMTextUnitVariant first = iterator.next();

        assertEquals(first.getContent(), "FR[this is the content]");
        assertEquals(first.getLocale().getBcp47Tag(), "fr-FR");
        assertEquals(first.getStatus(), TMTextUnitVariant.Status.APPROVED);
        assertNotNull(first.getCreatedByUser());
    }

    @Test
    public void testHistoryMultipleVariants() throws RepositoryNameAlreadyUsedException {
        createTestData();

        logger.debug("Done creating data for test, start testing");

        Long addTextUnitAndCheck1 = addTextUnitAndCheck(tmId, assetId, "name", "this is the content", "some comment", "3063c39d3cf8ab69bcabbbc5d7187dc9", "cf8ea6b6848f23345648038bc3abf324");

        logger.debug("Add a current translation for french");
        Locale frFRLocale = localeService.findByBcp47Tag("fr-FR");
        Long frLocaleId = frFRLocale.getId();

        logger.debug("TMTextUnit tmTextUnit = tmTextUnitRepository.findByMd5AndTmIdAndAssetId(md5, assetId, tmId);");
        String md5 = tmService.computeTMTextUnitMD5("name", "this is the content", "some comment");
        TMTextUnit tmTextUnit = tmTextUnitRepository.findByMd5AndTmIdAndAssetId(md5, tmId, assetId);
        logger.debug("tmtextunit: {}", tmTextUnit);

        addCurrentTMTextUnitVariant(tmTextUnit.getId(), frLocaleId, "FR[this is the content]", "0a30a359b20fd4095fc17fb586e8db4d");

        tmService.addTMTextUnitVariant(tmTextUnit.getId(), frLocaleId, "Ceci est le content", "comment 1", TMTextUnitVariant.Status.REVIEW_NEEDED, true);
        tmService.addTMTextUnitVariant(tmTextUnit.getId(), frLocaleId, "Ceci, c'est le content", "comment 2", TMTextUnitVariant.Status.TRANSLATION_NEEDED, true);

        // then get the history of it without adding any variants
        List<TMTextUnitVariant> history = tmHistoryService.findHistory(frFRLocale.getId(), addTextUnitAndCheck1);
        assertNotNull(history);
        assertFalse(history.isEmpty());
        assertEquals(history.size(), 3);

        Iterator<TMTextUnitVariant> iterator = history.iterator();
        assertTrue(iterator.hasNext());

        TMTextUnitVariant variant = iterator.next();
        logger.debug("first: " + variant.toString());
        assertEquals(variant.getContent(), "FR[this is the content]");
        assertEquals(variant.getLocale().getBcp47Tag(), "fr-FR");
        assertEquals(variant.getStatus(), TMTextUnitVariant.Status.APPROVED);
        assertNotNull(variant.getCreatedByUser());

        variant = iterator.next();
        logger.debug("second: " + variant.toString());
        assertEquals(variant.getContent(), "Ceci est le content");
        assertEquals(variant.getLocale().getBcp47Tag(), "fr-FR");
        assertEquals(variant.getStatus(), TMTextUnitVariant.Status.REVIEW_NEEDED);
        assertNotNull(variant.getCreatedByUser());

        variant = iterator.next();
        logger.debug("third: " + variant.toString());
        assertEquals(variant.getContent(), "Ceci, c'est le content");
        assertEquals(variant.getLocale().getBcp47Tag(), "fr-FR");
        assertEquals(variant.getStatus(), TMTextUnitVariant.Status.TRANSLATION_NEEDED);
        assertNotNull(variant.getCreatedByUser());
    }


    @Test
    public void testHistorySortedByDate() throws RepositoryNameAlreadyUsedException, InterruptedException {
        createTestData();

        logger.debug("Done creating data for test, start testing");

        Long addTextUnitAndCheck1 = addTextUnitAndCheck(tmId, assetId, "name", "this is the content", "some comment", "3063c39d3cf8ab69bcabbbc5d7187dc9", "cf8ea6b6848f23345648038bc3abf324");

        logger.debug("Add a current translation for french");
        Locale frFRLocale = localeService.findByBcp47Tag("fr-FR");
        Long frLocaleId = frFRLocale.getId();

        logger.debug("TMTextUnit tmTextUnit = tmTextUnitRepository.findByMd5AndTmIdAndAssetId(md5, assetId, tmId);");
        String md5 = tmService.computeTMTextUnitMD5("name", "this is the content", "some comment");
        TMTextUnit tmTextUnit = tmTextUnitRepository.findByMd5AndTmIdAndAssetId(md5, tmId, assetId);
        logger.debug("tmtextunit: {}", tmTextUnit);

        addCurrentTMTextUnitVariant(tmTextUnit.getId(), frLocaleId, "FR[this is the content]", "0a30a359b20fd4095fc17fb586e8db4d");

        Thread.sleep(200);

        tmService.addTMTextUnitVariant(tmTextUnit.getId(), frLocaleId, "Ceci est le content", "comment 1", TMTextUnitVariant.Status.REVIEW_NEEDED, true);

        Thread.sleep(200);

        tmService.addTMTextUnitVariant(tmTextUnit.getId(), frLocaleId, "Ceci, c'est le content", "comment 2", TMTextUnitVariant.Status.TRANSLATION_NEEDED, true);

        // then get the history of it without adding any variants
        List<TMTextUnitVariant> history = tmHistoryService.findHistory(frFRLocale.getId(), addTextUnitAndCheck1);
        assertNotNull(history);
        assertFalse(history.isEmpty());
        assertEquals(history.size(), 3);

        ArrayList<TMTextUnitVariant> historyArray = new ArrayList(history);

        assertTrue(historyArray.get(0).getCreatedDate().getMillis() < historyArray.get(1).getCreatedDate().getMillis());
        assertTrue(historyArray.get(1).getCreatedDate().getMillis() < historyArray.get(2).getCreatedDate().getMillis());
    }

    private Long addTextUnitAndCheck(Long tmId, Long assetId, String name, String content, String comment, String md5Check, String contentMd5Check) {
        TMTextUnit addTMTextUnit = tmService.addTMTextUnit(tmId, assetId, name, content, comment);

        assertNotNull(addTMTextUnit.getId());
        assertEquals(name, addTMTextUnit.getName());
        assertEquals(content, addTMTextUnit.getContent());
        assertEquals(comment, addTMTextUnit.getComment());
        assertEquals(md5Check, addTMTextUnit.getMd5());
        assertEquals(contentMd5Check, addTMTextUnit.getContentMd5());
        assertEquals(tmId, addTMTextUnit.getTm().getId());
        assertNotNull(addTMTextUnit.getCreatedByUser());

        return addTMTextUnit.getId();
    }

    private TMTextUnitVariant addCurrentTMTextUnitVariant(Long tmTextUnitId, Long localeId, String content, String contentMD5) {
        TMTextUnitVariant addCurrentTMTextUnitVariant = tmService.addCurrentTMTextUnitVariant(tmTextUnitId, localeId, content);
        assertEquals(content, addCurrentTMTextUnitVariant.getContent());
        assertEquals(contentMD5, addCurrentTMTextUnitVariant.getContentMD5());
        assertEquals(localeId, ((HibernateProxy) addCurrentTMTextUnitVariant.getLocale()).getHibernateLazyInitializer().getIdentifier());
        assertEquals(tmTextUnitId, ((HibernateProxy) addCurrentTMTextUnitVariant.getTmTextUnit()).getHibernateLazyInitializer().getIdentifier());
        assertNotNull(addCurrentTMTextUnitVariant.getCreatedByUser());
        return addCurrentTMTextUnitVariant;
    }
}
