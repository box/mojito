package com.box.l10n.mojito.service.tm.search;

import com.box.l10n.mojito.nativecriteria.NativeContainsExp;
import com.box.l10n.mojito.nativecriteria.NativeILikeExp;
import com.box.l10n.mojito.nativecriteria.NativeColumnEqExp;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.nativecriteria.JpaQueryProvider;
import com.box.l10n.mojito.nativecriteria.NativeDateGteExp;
import com.box.l10n.mojito.nativecriteria.NativeDateLteExp;
import com.box.l10n.mojito.nativecriteria.NativeEqExpFix;
import com.box.l10n.mojito.nativecriteria.NativeInExpFix;
import com.github.pnowy.nc.core.CriteriaResult;
import com.github.pnowy.nc.core.NativeCriteria;
import com.github.pnowy.nc.core.NativeExps;
import com.github.pnowy.nc.core.expressions.NativeExp;
import com.github.pnowy.nc.core.expressions.NativeIsNotNullExp;
import com.github.pnowy.nc.core.expressions.NativeIsNullExp;
import com.github.pnowy.nc.core.expressions.NativeJoin;
import com.github.pnowy.nc.core.expressions.NativeJunctionExp;
import com.github.pnowy.nc.core.expressions.NativeOrderExp;
import com.github.pnowy.nc.core.expressions.NativeProjection;
import com.github.pnowy.nc.core.mappers.CriteriaResultTransformer;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Text Unit searcher allows to search/build a list of translated and/or
 * untranslated text units for multiple locales at the same time.
 *
 * <p>
 * It is possible to filter the result set based on different criteria: by text
 * unit name, source and target content, comment, etc. Only exact matches on
 * content is supported for now.
 *
 * <p>
 * Result set can be paginated, all filter are passed using
 * {@link TextUnitSearcherParameters}
 *
 * <p>
 * This class is aimed be used by the workbench. It can also be used to generate
 * translation kits (query could be simplified in that case).
 *
 * @author jaurambault
 */
@Service
public class TextUnitSearcher {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TextUnitSearcher.class);

    @Retryable(
            value = {TextUnitSearcherError.class},
            backoff = @Backoff(delay = 500, multiplier = 2))
    public TextUnitAndWordCount countTextUnitAndWordCount(TextUnitSearcherParameters searchParameters) throws TextUnitSearcherError {

        NativeCriteria c = getCriteriaForSearch(searchParameters);

        c.setProjection(NativeExps.projection().
                addAggregateProjection("tu.id", "tu_count", NativeProjection.AggregateProjection.COUNT).
                addAggregateProjection("tu.word_count", "tu_word_count", NativeProjection.AggregateProjection.SUM));

        try {
            TextUnitAndWordCount textUnitAndWordCount = c.criteriaResult(new CriteriaResultTransformerTextUnitAndWordCount());
            return textUnitAndWordCount;
        } catch (Exception e) {
            logger.warn("TextUnitSearcher failed to count, query: {}", c.getQueryInfo().toString());
            throw new TextUnitSearcherError(c, "count text unit", e);
        }
    }

    @Recover
    public TextUnitAndWordCount recoverCountTextUnitAndWordCount(TextUnitSearcherError textUnitSearcherError, TextUnitSearcherParameters searchParameters) throws Throwable {
        logTextUnitSearcherError(textUnitSearcherError);
        throw textUnitSearcherError.getCause();
    }

    @Recover
    public List<TextUnitDTO> recoverSearch(TextUnitSearcherError textUnitSearcherError, TextUnitSearcherParameters searchParameters) throws Throwable {
        logTextUnitSearcherError(textUnitSearcherError);
        throw textUnitSearcherError.getCause();
    }

    void logTextUnitSearcherError(TextUnitSearcherError textUnitSearcherError) throws TextUnitSearcherError {
        logger.error("TextUnitSearcher couldn't recover for \"call\": {}\n{}",
                textUnitSearcherError.getMessage(),
                textUnitSearcherError.nativeCriteria.getQueryInfo().toString());
    }

    /**
     * Search/Build text units.
     *
     * @param searchParameters the search parameter to specify filters and
     * pagination
     * @return the list of text units
     */
    @Transactional
    @Retryable(
            value = {TextUnitSearcherError.class},
            backoff = @Backoff(delay = 500, multiplier = 2))
    public List<TextUnitDTO> search(TextUnitSearcherParameters searchParameters) {
   
        NativeCriteria c = getCriteriaForSearch(searchParameters);

        try {
            logger.debug("Perform query");
            List<TextUnitDTO> resultAsList = c.criteriaResult(new TextUnitDTONativeObjectMapper());
            
            if (logger.isDebugEnabled()) {
                logger.debug("Query done, info: {}", c.getQueryInfo());
            }

            return resultAsList;
        } catch (Exception e) {
            logger.warn("TextUnitSearcher failed to search, query: {}", c.getQueryInfo().toString());
            throw new TextUnitSearcherError(c, "search", e);
        }
    }

    NativeCriteria getCriteriaForSearch(TextUnitSearcherParameters searchParameters) {

        Preconditions.checkNotNull(searchParameters, "Search parameters should not be null");

        logger.debug("Creating the native criteria with joins");

        NativeCriteria c = new NativeCriteria(new JpaQueryProvider(), "tm_text_unit", "tu");
        c.addJoin(NativeExps.crossJoin("locale", "l"));
        c.addJoin(NativeExps.innerJoin("asset", "a", "a.id", "tu.asset_id"));
        c.addJoin(NativeExps.innerJoin("repository", "r", "r.id", "a.repository_id"));

        NativeJunctionExp onClauseRepositoryLocale = NativeExps.conjunction();
        onClauseRepositoryLocale.add(new NativeColumnEqExp("rl.locale_id", "l.id"));
        onClauseRepositoryLocale.add(new NativeColumnEqExp("rl.repository_id", "r.id"));
        if (searchParameters.isForRootLocale()) {
            onClauseRepositoryLocale.add(new NativeIsNullExp("rl.parent_locale"));
        } else if (searchParameters.isRootLocaleExcluded()) {
            onClauseRepositoryLocale.add(new NativeIsNotNullExp("rl.parent_locale"));
        }
        c.addJoin(NativeExps.innerJoin("repository_locale", "rl", onClauseRepositoryLocale));

        NativeJunctionExp onClauseTMTextUnitCurrentVariant = NativeExps.conjunction();
        onClauseTMTextUnitCurrentVariant.add(new NativeColumnEqExp("tu.id", "tuvc.tm_text_unit_id"));
        onClauseTMTextUnitCurrentVariant.add(new NativeColumnEqExp("l.id", "tuvc.locale_id"));
        c.addJoin(new NativeJoin("tm_text_unit_current_variant", "tuvc", NativeJoin.JoinType.LEFT_OUTER, onClauseTMTextUnitCurrentVariant));
        c.addJoin(NativeExps.leftJoin("tm_text_unit_variant", "tuv", "tuvc.tm_text_unit_variant_id", "tuv.id"));

        // We only want mapping for the last asset extraction
        NativeJunctionExp onClauseAssetTextUnit = NativeExps.conjunction();
        onClauseAssetTextUnit.add(new NativeColumnEqExp("map.tm_text_unit_id", "tu.id"));
        onClauseAssetTextUnit.add(new NativeColumnEqExp("a.last_successful_asset_extraction_id", "map.asset_extraction_id"));
        c.addJoin(new NativeJoin("asset_text_unit_to_tm_text_unit", "map", NativeJoin.JoinType.LEFT_OUTER, onClauseAssetTextUnit));

        c.addJoin(NativeExps.leftJoin("asset_text_unit", "atu", "atu.id", "map.asset_text_unit_id"));

        // Handle plural forms with potential filter per locale
        c.addJoin(NativeExps.leftJoin("plural_form", "pf", "tu.plural_form_id", "pf.id"));
        NativeJunctionExp onClausePluralForm = NativeExps.conjunction();
        onClausePluralForm.add(new NativeColumnEqExp("pffl.plural_form_id", "tu.plural_form_id"));
        onClausePluralForm.add(new NativeColumnEqExp("pffl.locale_id", "l.id"));
        c.addJoin(new NativeJoin("plural_form_for_locale", "pffl", NativeJoin.JoinType.LEFT_OUTER, onClausePluralForm));

        logger.debug("Set projections");

        //TODO(P1) Might want to some of those projection as optional for perf reason
        c.setProjection(NativeExps.projection().
                addProjection("tu.id", "tmTextUnitId").
                addProjection("tuv.id", "tmTextUnitVariantId").
                //TODO(PO) THIS NOT CONSISTANT !! chooose
                addProjection("l.id", "localeId").
                addProjection("l.bcp47_tag", "targetLocale").
                addProjection("tu.name", "name").
                addProjection("tu.content", "source").
                addProjection("tu.comment", "comment").
                addProjection("tuv.content", "target").
                addProjection("tuv.comment", "targetComment").
                addProjection("tu.asset_id", "assetId").
                addProjection("a.last_successful_asset_extraction_id", "lastSuccessfulAssetExtractionId").
                addProjection("atu.asset_extraction_id", "assetExtractionId").
                addProjection("tuvc.id", "tmTextUnitCurrentVariantId").
                addProjection("tuv.status", "status").
                addProjection("tuv.included_in_localized_file", "includedInLocalizedFile").
                addProjection("tuv.created_date", "createdDate").
                addProjection("a.deleted", "assetDeleted").
                addProjection("pf.name", "pluralForm").
                addProjection("tu.plural_form_other", "pluralFormOther").
                addProjection("r.name", "repositoryName").
                addProjection("a.path", "assetPath").
                addProjection("atu.id", "assetTextUnitId").
                addProjection("tu.created_date", "tmTextUnitCreatedDate").
                addProjection("atu.do_not_translate", "doNotTranslate")
        );

        logger.debug("Add search filters");
        NativeJunctionExp conjunction = NativeExps.conjunction();

        if (searchParameters.isPluralFormsFiltered()) {
            NativeJunctionExp pluralFormForLocale = NativeExps.disjunction();
            pluralFormForLocale.add(NativeExps.isNotNull("pffl.plural_form_id"));
            pluralFormForLocale.add(NativeExps.isNull("tu.plural_form_id"));
            conjunction.add(pluralFormForLocale);
        }

        if (searchParameters.getRepositoryIds() != null && !searchParameters.getRepositoryIds().isEmpty()) {
            conjunction.add(new NativeInExpFix("r.id", searchParameters.getRepositoryIds()));
        }

        if (searchParameters.getRepositoryNames() != null && !searchParameters.getRepositoryNames().isEmpty()) {
            conjunction.add(new NativeInExpFix("r.name", searchParameters.getRepositoryNames()));
        }

        if (searchParameters.getName() != null) {
            conjunction.add(getSearchTypeNativeExp(searchParameters.getSearchType(), "tu.name", searchParameters.getName()));
        }

        if (searchParameters.getSource() != null) {
            conjunction.add(getSearchTypeNativeExp(searchParameters.getSearchType(), "tu.content", "tu.content_md5", searchParameters.getSource()));
        }

        if (searchParameters.getMd5() != null) {
            conjunction.add(new NativeEqExpFix("tu.md5", searchParameters.getMd5()));
        }

        if (searchParameters.getPluralFormOther() != null) {
            conjunction.add(getSearchTypeNativeExp(searchParameters.getSearchType(), "tu.plural_form_other", searchParameters.getPluralFormOther()));
        }

        if (searchParameters.getTarget() != null) {
            conjunction.add(getSearchTypeNativeExp(searchParameters.getSearchType(), "tuv.content", "tuv.content_md5", searchParameters.getTarget()));
        }

        if (searchParameters.getAssetPath() != null) {
            conjunction.add(getSearchTypeNativeExp(searchParameters.getSearchType(), "a.path", searchParameters.getAssetPath()));
        }

        if (searchParameters.getAssetId() != null) {
            conjunction.add(new NativeEqExpFix("tu.asset_id", searchParameters.getAssetId()));
        }

        if (searchParameters.getLocaleTags() != null && !searchParameters.getLocaleTags().isEmpty()) {
            //TODO(P1) probably want to work on ids only at this level, here for testing (see also moving to constant for locale id)
            conjunction.add(new NativeInExpFix("l.bcp47_tag", searchParameters.getLocaleTags()));
        }

        if (searchParameters.getLocaleId() != null) {
            conjunction.add(new NativeEqExpFix("l.id", searchParameters.getLocaleId()));
        }

        if (searchParameters.getToBeFullyTranslatedFilter() != null) {
            conjunction.add(new NativeEqExpFix("rl.to_be_fully_translated", searchParameters.getToBeFullyTranslatedFilter()));
        }

        if (searchParameters.getTmTextUnitId() != null) {
            conjunction.add(new NativeEqExpFix("tu.id", searchParameters.getTmTextUnitId()));
        }

        if (searchParameters.getTmId() != null) {
            conjunction.add(new NativeEqExpFix("tu.tm_id", searchParameters.getTmId()));
        }

        if (searchParameters.getPluralFormId() != null) {
            conjunction.add(new NativeEqExpFix("tu.plural_form_id", searchParameters.getPluralFormId()));
        }

        if (searchParameters.getDoNotTranslateFilter() != null) {
            conjunction.add(new NativeEqExpFix("atu.do_not_translate", searchParameters.getDoNotTranslateFilter()));
        }

        StatusFilter statusFilter = searchParameters.getStatusFilter();

        if (statusFilter != null) {

            switch (statusFilter) {
                case ALL:
                    break;
                case NOT_REJECTED:
                    conjunction.add(new NativeEqExpFix("tuv.included_in_localized_file", Boolean.TRUE));
                    break;
                case REJECTED:
                    conjunction.add(new NativeEqExpFix("tuv.included_in_localized_file", Boolean.FALSE));
                    break;
                case REVIEW_NEEDED:
                    conjunction.add(new NativeEqExpFix("tuv.status", TMTextUnitVariant.Status.REVIEW_NEEDED.toString()));
                    break;
                case REVIEW_NOT_NEEDED:
                    conjunction.add(NativeExps.notEq("tuv.status", TMTextUnitVariant.Status.REVIEW_NEEDED.toString()));
                    break;
                case TRANSLATED:
                    conjunction.add(NativeExps.isNotNull("tuv.id"));
                    break;
                case APPROVED_AND_NOT_REJECTED:
                    conjunction.add(new NativeEqExpFix("tuv.status", TMTextUnitVariant.Status.APPROVED.toString()));
                    conjunction.add(new NativeEqExpFix("tuv.included_in_localized_file", Boolean.TRUE));
                    break;
                case APPROVED_OR_NEEDS_REVIEW_AND_NOT_REJECTED:
                    List<String> statuses = Arrays.asList(
                            TMTextUnitVariant.Status.APPROVED.toString(),
                            TMTextUnitVariant.Status.REVIEW_NEEDED.toString()
                    );
                    conjunction.add(new NativeInExpFix("tuv.status", statuses));
                    conjunction.add(new NativeEqExpFix("tuv.included_in_localized_file", Boolean.TRUE));
                    break;
                case TRANSLATED_AND_NOT_REJECTED:
                    conjunction.add(NativeExps.isNotNull("tuv.id"));
                    conjunction.add(new NativeEqExpFix("tuv.included_in_localized_file", Boolean.TRUE));
                    break;
                case UNTRANSLATED:
                    conjunction.add(NativeExps.isNull("tuv.id"));
                    break;
                case FOR_TRANSLATION:
                    conjunction.add(NativeExps.disjunction(
                            Arrays.asList(
                                    NativeExps.isNull("tuv.id"),
                                    new NativeEqExpFix("tuv.status", TMTextUnitVariant.Status.TRANSLATION_NEEDED.toString()),
                                    new NativeEqExpFix("tuv.included_in_localized_file", Boolean.FALSE)
                            )
                    ));
                    break;
            }
        }

        UsedFilter usedFilter = searchParameters.getUsedFilter();
        if (usedFilter != null) {
            if (UsedFilter.USED.equals(usedFilter)) {
                conjunction.add(NativeExps.isNotNull("atu.id"));
                conjunction.add(new NativeEqExpFix("a.deleted", Boolean.FALSE));
            } else {
                conjunction.add(NativeExps.disjunction(
                        Arrays.asList(
                                NativeExps.isNull("atu.id"),
                                new NativeEqExpFix("a.deleted", Boolean.TRUE)
                        )
                ));
            }
        }

        if (searchParameters.getTmTextUnitCreatedBefore() != null) {
            conjunction.add(new NativeDateLteExp("tu.created_date", searchParameters.getTmTextUnitCreatedBefore()));
        }

        if (searchParameters.getTmTextUnitCreatedAfter() != null) {
            conjunction.add(new NativeDateGteExp("tu.created_date", searchParameters.getTmTextUnitCreatedAfter()));
        }

        if (!conjunction.toSQL().isEmpty()) {
            c.add(conjunction);
        }

        if (searchParameters.getLimit() != null) {
            c.setLimit(searchParameters.getLimit());
        }

        if (searchParameters.getOffset() != null) {
            c.setOffset(searchParameters.getOffset());
        }

        if (searchParameters instanceof TextUnitSearcherParametersForTesting) {
            TextUnitSearcherParametersForTesting textUnitSearcherParametersForTesting = (TextUnitSearcherParametersForTesting) searchParameters;

            if (textUnitSearcherParametersForTesting.isOrdered()) {
                c.setOrder(NativeExps.order().add("tu.id", NativeOrderExp.OrderType.ASC).add("l.id", NativeOrderExp.OrderType.ASC));
            }
        }

        return c;
    }

    /**
     * Based on the {@link SearchType} builds the proper {@link NativeExp} to be
     * used in the search query.
     *
     * @param searchType the search type
     * @param columnName the column name
     * @param value the value
     * @return the {@link NativeExp} to be used in the query
     */
    NativeExp getSearchTypeNativeExp(SearchType searchType, String columnName, String value) {
        return getSearchTypeNativeExp(searchType, columnName, null, value);
    }

    /**
     * Based on the {@link SearchType} builds the proper {@link NativeExp} to be
     * used in the search query.
     *
     * @param searchType the search type
     * @param columnName the column name
     * @param columnNameForMd5Match optional column name that will be used to to
     * an optimized comparison by check MD5s
     * @param value the value
     * @return the {@link NativeExp} to be used in the query
     */
    NativeExp getSearchTypeNativeExp(SearchType searchType, String columnName, String columnNameForMd5Match, String value) {

        NativeExp nativeExp = null;

        if (searchType == null || SearchType.EXACT.equals(searchType)) {

            if (columnNameForMd5Match == null) {
                nativeExp = new NativeEqExpFix(columnName, value);
            } else {
                nativeExp = new NativeEqExpFix(columnNameForMd5Match, DigestUtils.md5Hex(value));
            }

        } else if (SearchType.CONTAINS.equals(searchType)) {
            nativeExp = new NativeContainsExp(columnName, value);
        } else if (SearchType.ILIKE.equals(searchType)) {
            nativeExp = new NativeILikeExp(columnName, value);
        }

        return nativeExp;
    }

    private static class CriteriaResultTransformerTextUnitAndWordCount implements CriteriaResultTransformer<TextUnitAndWordCount> {

        @Override
        public TextUnitAndWordCount transform(CriteriaResult cr) {
            cr.next();
            return new TextUnitAndWordCount(cr.getLong(0), cr.getLong(1) == null ? 0 : cr.getLong(1));
        }
    }

}
