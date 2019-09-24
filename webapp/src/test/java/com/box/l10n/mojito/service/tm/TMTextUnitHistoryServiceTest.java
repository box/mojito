package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.okapi.Status;
import com.box.l10n.mojito.okapi.XliffState;
import com.box.l10n.mojito.okapi.filters.AndroidXMLEncoder;
import com.box.l10n.mojito.okapi.filters.SimpleEncoder;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.asset.AssetUpdateException;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetExtraction.extractor.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskException;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import net.sf.okapi.common.resource.TextUnit;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.proxy.HibernateProxy;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

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

    @Autowired
    TMRepository tmRepository;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

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

    @Autowired
    TMTextUnitVariantCommentRepository tmTextUnitVariantCommentRepository;

    @Autowired
    TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

    @Autowired
    RepositoryLocaleRepository repositoryLocaleRepository;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    TMXliffRepository tmXliffRepository;

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
        assertTrue(first.equals(addCurrentTMTextUnitVariant));
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

    /*

    @Test
    public void testAddCurrentTMTextUnitVariant() throws RepositoryNameAlreadyUsedException {
        createTestData();

        String name = "name";
        String content = "this is the content";
        String comment = "some comment";
        TMTextUnit addTMTextUnit = tmService.addTMTextUnit(tmId, assetId, name, content, comment);

        logger.debug("Done creating data for test, start testing");

        Locale frFRLocale = localeService.findByBcp47Tag("fr-FR");

        logger.debug("Add a current translation for french");
        TMTextUnitVariant addCurrentTMTextUnitVariant = addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frFRLocale.getId(), "FR[this is the content]", "0a30a359b20fd4095fc17fb586e8db4d");

        logger.debug("Add the same content, it should be skipped and return previous TMTextUnitVariant");
        TMTextUnitVariant addCurrentTMTextUnitVariantSkipped = tmService.addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frFRLocale.getId(), "FR[this is the content]");
        assertEquals(addCurrentTMTextUnitVariant.getId(), addCurrentTMTextUnitVariantSkipped.getId());

        logger.debug("Add a different content, it should be added");
        TMTextUnitVariant addCurrentTMTextUnitVariant1 = addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frFRLocale.getId(), "FR[this is the content 2]", "499bd8403c69151a7a21bf756f57183b");

        logger.debug("Go back to a translation with same content as previous version, should add entities");
        TMTextUnitVariant addCurrentTMTextUnitVariant2 = addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frFRLocale.getId(), "FR[this is the content]", "0a30a359b20fd4095fc17fb586e8db4d");

        Iterator<TMTextUnitVariant> tmTextUnitVariantIteratorFr = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(frFRLocale.getId(), tmId).iterator();
        assertEquals(addCurrentTMTextUnitVariant.getId(), tmTextUnitVariantIteratorFr.next().getId());
        assertEquals(addCurrentTMTextUnitVariant1.getId(), tmTextUnitVariantIteratorFr.next().getId());
        assertEquals(addCurrentTMTextUnitVariant2.getId(), tmTextUnitVariantIteratorFr.next().getId());
        assertFalse(tmTextUnitVariantIteratorFr.hasNext());

        Locale frCALocale = localeService.findByBcp47Tag("fr-CA");

        logger.debug("Add a current translation for french France with same content as the french translation");
        TMTextUnitVariant addCurrentTMTextUnitVariant3 = addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frCALocale.getId(), "FR[this is the content]", "0a30a359b20fd4095fc17fb586e8db4d");

        Iterator<TMTextUnitVariant> tmTextUnitVariantIteratorFrFR = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(frCALocale.getId(), tmId).iterator();
        assertEquals(addCurrentTMTextUnitVariant3.getId(), tmTextUnitVariantIteratorFrFR.next().getId());

    }
     */

}
