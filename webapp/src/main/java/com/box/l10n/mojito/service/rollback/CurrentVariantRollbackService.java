package com.box.l10n.mojito.service.rollback;

import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant_;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.google.common.base.Preconditions;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * @author aloison
 */
@Service
public class CurrentVariantRollbackService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(CurrentVariantRollbackService.class);

    @Autowired
    EntityManager entityManager;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    /**
     * Rollbacks the current variants of the given TM to the state at the given date.
     *
     * @param rollbackDateTime   Date at which the {@link TMTextUnitCurrentVariant}s will be rollbacked to
     * @param tmId               ID of the TM the {@link TMTextUnitCurrentVariant}s to be rolled back should belong to
     */
    public void rollbackCurrentVariantsFromTMToDate(DateTime rollbackDateTime, Long tmId) {
        rollbackCurrentVariantsFromTMToDate(rollbackDateTime, tmId, new CurrentVariantRollbackParameters());
    }

    /**
     * Rollbacks the current variants of the given TM to the state at the given date.
     * It will rollback the current variants for the given parameters (per locale, per TMTextUnit).
     *
     * @param rollbackDateTime Date at which the {@link TMTextUnitCurrentVariant}s will be rollbacked to
     * @param tmId             ID of the TM the {@link TMTextUnitCurrentVariant}s to be rolled back should belong to
     * @param extraParameters  Extra parameters to filter what to rollback (should not be null)
     */
    @Transactional
    public void rollbackCurrentVariantsFromTMToDate(DateTime rollbackDateTime, Long tmId, CurrentVariantRollbackParameters extraParameters) {

        Preconditions.checkNotNull(extraParameters, "Extra parameters should not be null");

        deleteExistingCurrentVariants(tmId, extraParameters);
        addCurrentVariantsAsOfRollbackDate(rollbackDateTime, tmId, extraParameters);
    }

    /**
     * Deletes the {@link com.box.l10n.mojito.entity.TMTextUnitCurrentVariant}s based on the given parameters.
     * These variants will be recreated later in the same state as at the rollback date.
     *
     * @param tmId             ID of the TM the {@link TMTextUnitCurrentVariant}s to be rolled back should belong to
     * @param extraParameters  Extra parameters to filter what to rollback
     */
    protected void deleteExistingCurrentVariants(Long tmId, CurrentVariantRollbackParameters extraParameters) {

        logger.debug("Deleting tmTextUnitCurrentVariants for the TM {}", tmId);

        Query deleteQuery = buildDeleteQuery(tmId, extraParameters);
        deleteQuery.executeUpdate();
    }

    /**
     * Builds the query to delete {@link com.box.l10n.mojito.entity.TMTextUnitCurrentVariant}s that will be rolled back
     *
     * @param tmId            ID of the TM the {@link TMTextUnitCurrentVariant}s to be rolled back should belong to
     * @param extraParameters Extra parameters to filter what to rollback
     * @return The delete query
     */
    protected Query buildDeleteQuery(Long tmId, CurrentVariantRollbackParameters extraParameters) {

        logger.trace("Building the delete tmTextUnitCurrentVariants query");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaDelete<TMTextUnitCurrentVariant> deleteCriteria = criteriaBuilder.createCriteriaDelete(TMTextUnitCurrentVariant.class);
        Root<TMTextUnitCurrentVariant> root = deleteCriteria.from(TMTextUnitCurrentVariant.class);

        Predicate whereClause = criteriaBuilder.conjunction();

        Predicate tmPredicate = criteriaBuilder.equal(root.get(TMTextUnitCurrentVariant_.tm), tmId);
        whereClause = criteriaBuilder.and(whereClause, tmPredicate);

        List<Long> localeIdsToRollback = extraParameters.getLocaleIds();
        if (localeIdsToRollback != null && !localeIdsToRollback.isEmpty()) {
            Predicate localesPredicate = criteriaBuilder.isTrue(root.get(TMTextUnitCurrentVariant_.locale).in(localeIdsToRollback));
            whereClause = criteriaBuilder.and(whereClause, localesPredicate);
        }

        List<Long> tmTextUnitIdsToRollback = extraParameters.getTmTextUnitIds();
        if (tmTextUnitIdsToRollback != null && !tmTextUnitIdsToRollback.isEmpty()) {
            Predicate tmTextUnitPredicate = criteriaBuilder.isTrue(root.get(TMTextUnitCurrentVariant_.tmTextUnit).in(tmTextUnitIdsToRollback));
            whereClause = criteriaBuilder.and(whereClause, tmTextUnitPredicate);
        }

        deleteCriteria.where(whereClause);

        return entityManager.createQuery(deleteCriteria);
    }

    /**
     * Deletes the {@link com.box.l10n.mojito.entity.TMTextUnitCurrentVariant}s based on the given parameters.
     * These variants will be recreated later in the same state as at the rollback date.
     *
     * @param rollbackDateTime Date at which the {@link TMTextUnitCurrentVariant}s will be rollbacked to
     * @param tmId             ID of the TM the {@link TMTextUnitCurrentVariant}s to be rolled back should belong to
     * @param extraParameters  Extra parameters to filter what to rollback
     */
    protected void addCurrentVariantsAsOfRollbackDate(DateTime rollbackDateTime, Long tmId, CurrentVariantRollbackParameters extraParameters) {

        logger.debug("Adding back TMTextUnitCurrentVariants as of {}", rollbackDateTime);

        AuditQuery auditQuery = buildInsertAuditQuery(rollbackDateTime, tmId, extraParameters);
        List<TMTextUnitCurrentVariant> tmTextUnitCurrentVariantsToAdd = (List<TMTextUnitCurrentVariant>) auditQuery.getResultList();
        tmTextUnitCurrentVariantRepository.saveAll(tmTextUnitCurrentVariantsToAdd);
    }

    /**
     * Builds the query to insert new {@link com.box.l10n.mojito.entity.TMTextUnitCurrentVariant}s
     * as they were at the rollback date.
     *
     * @param rollbackDateTime Date at which the {@link TMTextUnitCurrentVariant}s will be rollbacked to
     * @param tmId             ID of the TM the {@link TMTextUnitCurrentVariant}s to be rolled back should belong to
     * @param extraParameters  Extra parameters to filter what to rollback
     * @return The insert audit query
     */
    protected AuditQuery buildInsertAuditQuery(DateTime rollbackDateTime, Long tmId, CurrentVariantRollbackParameters extraParameters) {

        logger.trace("Building the insert tmTextUnitCurrentVariants audit query");

        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        Number revNumberAtDate = auditReader.getRevisionNumberForDate(rollbackDateTime.toDate());

        AuditQuery auditQuery = auditReader.createQuery()
                .forEntitiesAtRevision(TMTextUnitCurrentVariant.class, TMTextUnitCurrentVariant.class.getName(), revNumberAtDate, true)
                .add(AuditEntity.property("tm_id").eq(tmId));

        List<Long> localeIdsToRollback = extraParameters.getLocaleIds();
        if (localeIdsToRollback != null && !localeIdsToRollback.isEmpty()) {
            // Using "in" does not work with relatedId() nor property() so using loop instead
            for (Long localeIdToRollback : localeIdsToRollback) {
                auditQuery.add(AuditEntity.relatedId("locale").eq(localeIdToRollback));
            }
        }

        List<Long> tmTextUnitIdsToRollback = extraParameters.getTmTextUnitIds();
        if (tmTextUnitIdsToRollback != null && !tmTextUnitIdsToRollback.isEmpty()) {
            // Using "in" does not work with relatedId() nor property() so using loop instead
            for (Long tmTextUnitIdToRollback : tmTextUnitIdsToRollback) {
                auditQuery.add(AuditEntity.relatedId("tmTextUnit").eq(tmTextUnitIdToRollback));
            }
        }

        return auditQuery;
    }
}
