package com.box.l10n.mojito.service.tm.search;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.github.pnowy.nc.core.NativeCriteria;
import com.github.pnowy.nc.core.NativeExps;
import com.github.pnowy.nc.core.expressions.NativeExp;
import com.github.pnowy.nc.core.expressions.NativeIsNotNullExp;
import com.github.pnowy.nc.core.expressions.NativeJoin;
import com.github.pnowy.nc.core.expressions.NativeJunctionExp;
import com.github.pnowy.nc.core.expressions.NativeOrderExp;
import com.github.pnowy.nc.core.jpa.JpaQueryProvider;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    EntityManager entityManager;

    /**
     * This is private and it must be used only for tests via reflection.
     *
     * HSQL and MYSQL return dataset in a different order and makes tests not
     * reproducible depending on the DB being used.
     *
     * This shouldn't be used for the actual application because of the
     * performance implication on large dataset.
     *
     */
    private boolean ordered = false;

    /**
     * Search/Build text units.
     *
     * @param searchParameters the search parameter to specify filters and
     * pagination
     * @return the list of text units
     */
    @Transactional
    public List<TextUnitDTO> search(TextUnitSearcherParameters searchParameters) {

        Preconditions.checkNotNull(searchParameters, "Search parameters should not be null");

        logger.debug("Creating the native criteria with joins");

        NativeCriteria c = new NativeCriteria(new JpaQueryProvider(entityManager), "tm_text_unit", "tu");
        c.addJoin(NativeExps.crossJoin("locale", "l"));
        c.addJoin(NativeExps.innerJoin("asset", "a", "a.id", "tu.asset_id"));
        c.addJoin(NativeExps.innerJoin("repository", "r", "r.id", "a.repository_id"));

        NativeJunctionExp onClauseRepositoryLocale = NativeExps.conjunction();
        onClauseRepositoryLocale.add(new NativeColumnEqExp("rl.locale_id", "l.id"));
        onClauseRepositoryLocale.add(new NativeColumnEqExp("rl.repository_id", "r.id"));
        if (searchParameters.isRootLocaleExcluded()) {
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
        
        // check plural form for language
        NativeJunctionExp onClausePluralForm = NativeExps.conjunction();
        onClausePluralForm.add(new NativeColumnEqExp("pffl.plural_form_id", "tu.plural_form_id"));
        onClausePluralForm.add(new NativeColumnEqExp("pffl.locale_id", "l.id"));
        c.addJoin(new NativeJoin("plural_form_for_locale", "pffl", NativeJoin.JoinType.LEFT_OUTER, onClausePluralForm));
        c.addJoin(NativeExps.leftJoin("plural_form", "pf", "pffl.plural_form_id", "pf.id"));
        
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
                addProjection("a.path", "assetPath")         
        );

        logger.debug("Add search filters");
        NativeJunctionExp conjunction = NativeExps.conjunction();
        
        // get ride of uncessary plural form per language
        NativeJunctionExp pluralFormForLocale = NativeExps.disjunction();
        pluralFormForLocale.add(NativeExps.isNotNull("pffl.plural_form_id"));
        pluralFormForLocale.add(NativeExps.isNull("tu.plural_form_id"));
        conjunction.add(pluralFormForLocale);
        
        if (searchParameters.getRepositoryIds() != null && !searchParameters.getRepositoryIds().isEmpty()) {
            conjunction.add(NativeExps.in("r.id", searchParameters.getRepositoryIds()));
        }
        
        if (searchParameters.getRepositoryNames()!= null && !searchParameters.getRepositoryNames().isEmpty()) {
            conjunction.add(NativeExps.in("r.name", searchParameters.getRepositoryNames()));
        }

        if (searchParameters.getName() != null) {
            conjunction.add(getSearchTypeNativeExp(searchParameters.getSearchType(), "tu.name", searchParameters.getName()));
        }

        if (searchParameters.getSource() != null) {
            conjunction.add(getSearchTypeNativeExp(searchParameters.getSearchType(), "tu.content", "tu.content_md5", searchParameters.getSource()));
        }

        if (searchParameters.getMd5() != null) {
            conjunction.add(NativeExps.eq("tu.md5", searchParameters.getMd5()));
        }
        
        if (searchParameters.getPluralFormOther() != null) {
            conjunction.add(getSearchTypeNativeExp(searchParameters.getSearchType(), "tu.plural_form_other", searchParameters.getPluralFormOther()));
        }

        if (searchParameters.getTarget() != null) {
            conjunction.add(getSearchTypeNativeExp(searchParameters.getSearchType(), "tuv.content", "tuv.content_md5", searchParameters.getTarget()));
        }
        
        if (searchParameters.getAssetPath()!= null) {
            conjunction.add(getSearchTypeNativeExp(searchParameters.getSearchType(), "a.path", searchParameters.getAssetPath()));
        }

        if (searchParameters.getAssetId() != null) {
            conjunction.add(NativeExps.eq("tu.asset_id", searchParameters.getAssetId()));
        }

        if (searchParameters.getLocaleTags() != null && !searchParameters.getLocaleTags().isEmpty()) {
            //TODO(P1) probably want to work on ids only at this level, here for testing (see also moving to constant for locale id)
            conjunction.add(NativeExps.in("l.bcp47_tag", searchParameters.getLocaleTags()));
        }

        if (searchParameters.getLocaleId() != null) {
            conjunction.add(NativeExps.eq("l.id", searchParameters.getLocaleId()));
        }

        if (searchParameters.getTmTextUnitId() != null) {
            conjunction.add(NativeExps.eq("tu.id", searchParameters.getTmTextUnitId()));
        }

        if (searchParameters.getTmId() != null) {
            conjunction.add(NativeExps.eq("tu.tm_id", searchParameters.getTmId()));
        }

        StatusFilter statusFilter = searchParameters.getStatusFilter();

        if (statusFilter != null) {

            switch (statusFilter) {
                case ALL:
                    break;
                case NOT_REJECTED:
                    conjunction.add(NativeExps.eq("tuv.included_in_localized_file", Boolean.TRUE));
                    break;
                case REJECTED:
                    conjunction.add(NativeExps.eq("tuv.included_in_localized_file", Boolean.FALSE));
                    break;
                case REVIEW_NEEDED:
                    conjunction.add(NativeExps.eq("tuv.status", TMTextUnitVariant.Status.REVIEW_NEEDED.toString()));
                    break;
                case REVIEW_NOT_NEEDED:
                    conjunction.add(NativeExps.notEq("tuv.status", TMTextUnitVariant.Status.REVIEW_NEEDED.toString()));
                    break;
                case TRANSLATED:
                    conjunction.add(NativeExps.isNotNull("tuv.id"));
                    break;
                case TRANSLATED_AND_NOT_REJECTED:
                    conjunction.add(NativeExps.isNotNull("tuv.id"));
                    conjunction.add(NativeExps.eq("tuv.included_in_localized_file", Boolean.TRUE));
                    break;
                case UNTRANSLATED:
                    conjunction.add(NativeExps.isNull("tuv.id"));
                    break;
                case FOR_TRANSLATION:
                    conjunction.add(NativeExps.disjunction(
                            Arrays.asList(
                                    NativeExps.isNull("tuv.id"),
                                    NativeExps.eq("tuv.status", TMTextUnitVariant.Status.TRANSLATION_NEEDED.toString()),
                                    NativeExps.eq("tuv.included_in_localized_file", Boolean.FALSE)
                            )
                    ));
                    break;
            }
        }

        UsedFilter usedFilter = searchParameters.getUsedFilter();
        if (usedFilter != null) {
            if (UsedFilter.USED.equals(usedFilter)) {
                conjunction.add(NativeExps.isNotNull("atu.id"));
                conjunction.add(NativeExps.eq("a.deleted", Boolean.FALSE));
            } else {
                conjunction.add(NativeExps.disjunction(
                            Arrays.asList(
                                    NativeExps.isNull("atu.id"),
                                    NativeExps.eq("a.deleted", Boolean.TRUE)
                            )
                    ));
            }
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

        if (ordered) {
            c.setOrder(NativeExps.order().add("tu.id", NativeOrderExp.OrderType.ASC).add("l.id", NativeOrderExp.OrderType.ASC));
        }

        logger.debug("Perform query");
        List<TextUnitDTO> resultAsList = c.criteriaResult(new TextUnitDTONativeObjectMapper());

        if (logger.isDebugEnabled()) {
            logger.debug("Query done, info: {}", c.getQueryInfo());
        }

        return resultAsList;
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
                nativeExp = NativeExps.eq(columnName, value);
            } else {
                nativeExp = NativeExps.eq(columnNameForMd5Match, DigestUtils.md5Hex(value));
            }

        } else if (SearchType.CONTAINS.equals(searchType)) {
            nativeExp = new NativeContainsExp(columnName, value);
        } else if (SearchType.ILIKE.equals(searchType)) {
            nativeExp = new NativeILikeExp(columnName, value);
        }

        return nativeExp;
    }

}
