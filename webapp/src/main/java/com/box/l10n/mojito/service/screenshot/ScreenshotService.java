package com.box.l10n.mojito.service.screenshot;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Locale_;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Repository_;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.ScreenshotRun;
import com.box.l10n.mojito.entity.ScreenshotRun_;
import com.box.l10n.mojito.entity.ScreenshotTextUnit;
import com.box.l10n.mojito.entity.ScreenshotTextUnit_;
import com.box.l10n.mojito.entity.Screenshot_;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.tm.search.SearchType;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.comparator.NullSafeComparator;

/**
 * Service to manage screenshots.
 *
 * @author jeanaurambault
 */
@Service
public class ScreenshotService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ScreenshotService.class);

    @Autowired
    ScreenshotRunRepository screenshotRunRepository;

    @Autowired
    ScreenshotRepository screenshotRepository;

    @Autowired
    ScreenshotTextUnitRepository screenshotTextUnitRepository;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    EntityManager em;

    /**
     * Creates or update a screenshot run including the creation of related
     * screenshots. If updating a screenshot run then new screenshots get addes
     *
     * @param screenshotRun the screenshot run information, contains a list of
     * screenshot to be persisted
     *
     * @return the screenshot run object that was created. Note that the list of
     * screenshots is set to null (the limitation is related to the
     * update/create case)
     */
    @Transactional
    public ScreenshotRun createOrUpdateScreenshotRun(ScreenshotRun screenshotRun) {

        ScreenshotRun currentScreenshotRun = screenshotRunRepository.findByName(screenshotRun.getName());
        Repository repository = screenshotRun.getRepository();

        if (currentScreenshotRun != null) {
            logger.debug("Screenshot run already exsits, id: {}, update it", currentScreenshotRun.getId());
            screenshotRun.setId(currentScreenshotRun.getId());
        } else {
            logger.debug("Save screenshot run for repository: {}", repository.getName());
            screenshotRun = screenshotRunRepository.save(screenshotRun);
        }

        logger.debug("Start adding screenshots sorted by sequence");
        List<Screenshot> screenshotsToAdd = new ArrayList<>(screenshotRun.getScreenshots());
        sortScreenshotBySequence(screenshotsToAdd);

        for (Screenshot screenshot : screenshotsToAdd) {
            completeAndAddScreenshotToRun(screenshot, screenshotRun);
        }

        logger.debug("Update the last successful screenshot import");
        updateLastSucessfulScreenshotRun(repository, screenshotRun);

        // because of the create/update case the list of screenshot is not valid
        // anymore, set null to avoid confusion
        screenshotRun.setScreenshots(null);

        return screenshotRun;
    }

    void sortScreenshotBySequence(List<Screenshot> screenshots) {

        Collections.sort(screenshots, new Comparator<Screenshot>() {
            @Override
            public int compare(Screenshot o1, Screenshot o2) {
                return NullSafeComparator.NULLS_HIGH.compare(o1.getSequence(), o2.getSequence());
            }
        });
    }

    void updateLastSucessfulScreenshotRun(Repository repository, ScreenshotRun screenshotRun) {

        ScreenshotRun lastSuccesfulRun = screenshotRunRepository.findByRepositoryAndLastSuccessfulRunIsTrue(repository);

        if (lastSuccesfulRun != null) {
            lastSuccesfulRun.setLastSuccessfulRun(Boolean.FALSE);
        }

        screenshotRun.setLastSuccessfulRun(Boolean.TRUE);
        screenshotRunRepository.save(screenshotRun);
    }

    void completeAndAddScreenshotToRun(
            Screenshot screenshot,
            ScreenshotRun screenshotRun) {

        screenshot.setScreenshotRun(screenshotRun);

        for (ScreenshotTextUnit screenshotTextUnit : screenshot.getScreenshotTextUnits()) {
            completeScreenshotTextUnit(screenshotTextUnit, screenshot);
        }

        Screenshot existingScreenshot = screenshotRepository.findByScreenshotRunAndNameAndLocale(
                screenshotRun,
                screenshot.getName(),
                screenshot.getLocale());

        if (existingScreenshot != null) {
            logger.debug("Screenshot exists for locale: {} and name: {}, delete it ",
                    existingScreenshot.getLocale() == null ? null : existingScreenshot.getLocale().getBcp47Tag(),
                    existingScreenshot.getName());

            screenshotRepository.delete(existingScreenshot);
            screenshotRepository.flush();
        }

        logger.debug("Create new screenshot");
        screenshotRepository.save(screenshot);
    }

    /**
     * Get the source and target from the database from the screenshot text unit
     * name. This can get out of sync.
     *
     * An improvement would be to get them during extraction for more
     * consistency and actually show the fully translated string.
     *
     * @param screenshotTextUnit
     * @param screenshot
     */
    void completeScreenshotTextUnit(ScreenshotTextUnit screenshotTextUnit, Screenshot screenshot) {
        screenshotTextUnit.setScreenshot(screenshot);

        List<TextUnitDTO> textUnitDTOs = getTextUnitsForScreenshotTextUnitRenderedTarget(
                screenshot.getScreenshotRun().getRepository().getId(),
                screenshotTextUnit.getRenderedTarget(),
                screenshot.getLocale().getId());

        screenshotTextUnit.setNumberOfMatch(textUnitDTOs.size());

        if (textUnitDTOs.size() == 1) {
            //TODO only match if there is an embedded hidden id in the string?
            logger.debug("Found unique match, link the screenshot textunit to the tm");
            TextUnitDTO textUnitDTO = textUnitDTOs.get(0);
            screenshotTextUnit.setName(textUnitDTO.getName());
            screenshotTextUnit.setSource(textUnitDTO.getSource());
            screenshotTextUnit.setTarget(textUnitDTO.getTarget());
            // screenshotTextUnit.setTmTextUnit(textUnitForName.getTmTextUnitId());
        }

    }

    List<TextUnitDTO> getTextUnitsForScreenshotTextUnitRenderedTarget(Long repositoryId, String renderedTarget, Long localeId) {

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repositoryId);
        textUnitSearcherParameters.setLocaleId(localeId);
        textUnitSearcherParameters.setTarget(NormalizationUtils.normalize(renderedTarget));
        textUnitSearcherParameters.setSearchType(SearchType.EXACT);

        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
        return textUnitDTOs;
    }

    /**
     * Searches for screenshot given different criteria.
     *
     * @param repositoryIds mandatory, filter by repository ids
     * @param bcp47Tags can be null (no filter), filter by locale tags
     * @param screenshotName can be null (no filter), filter by screenshot name
     * @param status can be null (no filter), filter by status
     * @param name
     * @param source
     * @param target
     * @param searchType
     * @param limit number max of results to be returned
     * @param offset offset of the first result to be returned
     * @return the screenshots that matche the search parameters
     */
    @Transactional
    public List<Screenshot> searchScreenshots(
            List<Long> repositoryIds,
            List<String> bcp47Tags,
            String screenshotName,
            Screenshot.Status status,
            String name,
            String source,
            String target,
            SearchType searchType,
            int limit,
            int offset) {

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Screenshot> query = builder.createQuery(Screenshot.class);
        Root<Screenshot> screenshot = query.from(Screenshot.class);
        Join<Screenshot, ScreenshotRun> screenshotRunJoin = screenshot.join(Screenshot_.screenshotRun);
        Join<ScreenshotRun, Repository> repositoryJoin = screenshotRunJoin.join(ScreenshotRun_.repository);
        Join<Screenshot, Locale> localeJoin = screenshot.join(Screenshot_.locale);
        Join<Screenshot, ScreenshotTextUnit> screenshotTextUnits = screenshot.join(Screenshot_.screenshotTextUnits, JoinType.LEFT);

        Predicate conjunction = builder.conjunction();

        conjunction.getExpressions().add(builder.isTrue(screenshotRunJoin.get(ScreenshotRun_.lastSuccessfulRun)));

        if (repositoryIds != null) {
            conjunction.getExpressions().add(repositoryJoin.get(Repository_.id).in(repositoryIds));
        }

        if (bcp47Tags != null) {
            conjunction.getExpressions().add(localeJoin.get(Locale_.bcp47Tag).in(bcp47Tags));
        }

        if (!Strings.isNullOrEmpty(screenshotName)) {
            Predicate predicate = getPredicateForSearchType(
                    searchType,
                    builder,
                    screenshot.get(Screenshot_.name),
                    screenshotName);
            conjunction.getExpressions().add(predicate);
        }

        if (status != null) {
            Predicate predicate = builder.equal(screenshot.get(Screenshot_.status), status);
            conjunction.getExpressions().add(predicate);
        }

        if (!Strings.isNullOrEmpty(name)) {
            Predicate predicate = getPredicateForSearchType(
                    searchType,
                    builder,
                    screenshotTextUnits.get(ScreenshotTextUnit_.name),
                    name);
            conjunction.getExpressions().add(predicate);
        }

        if (!Strings.isNullOrEmpty(source)) {
            Predicate predicate = getPredicateForSearchType(
                    searchType,
                    builder,
                    screenshotTextUnits.get(ScreenshotTextUnit_.source),
                    source);
            conjunction.getExpressions().add(predicate);
        }

        if (!Strings.isNullOrEmpty(target)) {
            // for now search either on the target or the rendered target
            Predicate targetPredicate = getPredicateForSearchType(
                    searchType,
                    builder,
                    screenshotTextUnits.get(ScreenshotTextUnit_.target),
                    target);

            Predicate renderedTargetPredicate = getPredicateForSearchType(
                    searchType,
                    builder,
                    screenshotTextUnits.get(ScreenshotTextUnit_.renderedTarget),
                    target);

            conjunction.getExpressions().add(builder.or(targetPredicate, renderedTargetPredicate));
        }

        query.where(conjunction);

        List<Screenshot> screenshots = em.createQuery(query.distinct(true).select(screenshot)).setFirstResult(offset).setMaxResults(limit).getResultList();
        return screenshots;
    }

    private Predicate getPredicateForSearchType(
            SearchType searchType,
            CriteriaBuilder builder,
            Path<String> searchPath,
            String searchValue) {

        Predicate predicate = null;
        searchValue = NormalizationUtils.normalize(searchValue);

        if (searchType == null || SearchType.CONTAINS.equals(searchType)) {
            predicate = builder.like(searchPath, escapeAndWrapValueForContains(searchValue));
        } else if (SearchType.ILIKE.equals(searchType)) {
            predicate = builder.like(builder.lower(searchPath), searchValue.toLowerCase());
        } else {
            predicate = builder.equal(searchPath, searchValue);
        }

        return predicate;
    }

    String escapeAndWrapValueForContains(String value) {
        String escaped = value.replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /**
     * Updates a screenshot
     *
     * @param screenshot screenshot to be updated
     */
    public void updateScreenshot(Screenshot screenshot) {
        screenshotRepository.save(screenshot);
    }

}
