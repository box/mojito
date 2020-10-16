package com.box.l10n.mojito.service.eventlistener;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.repository.statistics.RepositoryStatisticService;
import com.box.l10n.mojito.service.repository.statistics.RepositoryStatisticsUpdatedReactor;
import com.google.common.collect.Sets;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Sets repository statistics outdated when the events that requires repository
 * statistics to re-compute occurs.
 *
 * @author jyi
 */
@Component
public class EntityCrudEventListener implements PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(EntityCrudEventListener.class);

    @Autowired
    RepositoryStatisticService repositoryStatisticService;

    @Autowired
    RepositoryStatisticsUpdatedReactor repositoryStatisticsUpdatedReactor;

    private static final Set<String> ENTITY_NAMES = Sets.newHashSet(RepositoryLocale.class.getName(), Asset.class.getName(), TMTextUnitVariant.class.getName(), TMTextUnitCurrentVariant.class.getName());

    @Override
    public void onPostInsert(PostInsertEvent event) {
        Repository repository = null;
        Object entity = event.getEntity();

        if (entity instanceof RepositoryLocale) {
            RepositoryLocale repositoryLocale = (RepositoryLocale) entity;
            repository = repositoryLocale.getRepository();
            logger.debug("Repository statistics is outdated because locale is added");
        } else if (entity instanceof TMTextUnitVariant) {
            TMTextUnitVariant tmTextUnitVariant = (TMTextUnitVariant) entity;
            TMTextUnit tmTextUnit = tmTextUnitVariant.getTmTextUnit();
            repository = tmTextUnit.getAsset().getRepository();
            logger.debug("Repository statistics is outdated because string/translation is added");
        }

        setRepositoryStatistisOutOfDate(repository);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        Repository repository = null;
        Object entity = event.getEntity();

        if (entity instanceof RepositoryLocale) {
            RepositoryLocale repositoryLocale = (RepositoryLocale) entity;
            repository = repositoryLocale.getRepository();
            logger.debug("Repository statistics is outdated because locale is updated");
        } else if (entity instanceof Asset) {
            Asset asset = (Asset) entity;
            repository = asset.getRepository();
            logger.debug("Repository statistics is outdated because asset is updated");
        }  else if (entity instanceof TMTextUnitCurrentVariant) {
            TMTextUnitCurrentVariant tmTextUnitCurrentVariant = (TMTextUnitCurrentVariant) entity;
            repository = tmTextUnitCurrentVariant.getTmTextUnit().getAsset().getRepository();
            logger.debug("Repository statistics is outdated because translation is deleted");
        }  else if (entity instanceof AssetExtraction) {
            AssetExtraction assetExtraction = (AssetExtraction) entity;
            repository = assetExtraction.getAsset().getRepository();
            logger.debug("Repository statistics is outdated because asset extraction has changed");
        }

        setRepositoryStatistisOutOfDate(repository);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        Repository repository = null;
        Object entity = event.getEntity();

        if (entity instanceof RepositoryLocale) {
            RepositoryLocale repositoryLocale = (RepositoryLocale) entity;
            repository = repositoryLocale.getRepository();
            logger.debug("Repository statistics is outdated because locale is deleted");
        }

        setRepositoryStatistisOutOfDate(repository);
    }

    private void setRepositoryStatistisOutOfDate(Repository repository) {
        if (repository != null) {
            repositoryStatisticsUpdatedReactor.generateEvent(repository.getId());
        }
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister ep) {
        String entityName = ep.getEntityName();
        return ENTITY_NAMES.contains(entityName);
    }

    @Override
    public void onPostInsertCommitFailed(PostInsertEvent pie) {
    }

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent pue) {
    }

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent pde) {
    }

}
