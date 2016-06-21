package com.box.l10n.mojito.service.eventlistener;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.repository.statistics.RepositoryStatisticService;
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

/**
 * Sets repository statistics outdated when the events that requires repository
 * statistics to re-compute occurs.
 *
 * @author jyi
 */
@Component
public class RepositoryStatisticsUpdatedListener implements PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepositoryStatisticsUpdatedListener.class);

    @Autowired
    RepositoryStatisticService repositoryStatisticService;

    @Override
    public void onPostInsert(PostInsertEvent event) {
        Repository repository = null;

        if (event.getEntity() instanceof RepositoryLocale) {
            RepositoryLocale repositoryLocale = (RepositoryLocale) event.getEntity();
            repository = repositoryLocale.getRepository();
            logger.debug("Repository statistics is outdated because locale is added");
        } else if (event.getEntity() instanceof Asset) {
            Asset asset = (Asset) event.getEntity();
            repository = asset.getRepository();
            logger.debug("Repository statistics is outdated because asset is added");
        } else if (event.getEntity() instanceof TMTextUnitVariant) {
            TMTextUnitVariant tmTextUnitVariant = (TMTextUnitVariant) event.getEntity();
            TMTextUnit tmTextUnit = tmTextUnitVariant.getTmTextUnit();
            repository = tmTextUnit.getAsset().getRepository();
            logger.debug("Repository statistics is outdated because string/translation is added");
        }

        setRepositoryStatistisOutOfDate(repository);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        Repository repository = null;

        if (event.getEntity() instanceof RepositoryLocale) {
            RepositoryLocale repositoryLocale = (RepositoryLocale) event.getEntity();
            repository = repositoryLocale.getRepository();
            logger.debug("Repository statistics is outdated because locale is updated");
        } else if (event.getEntity() instanceof Asset) {
            Asset asset = (Asset) event.getEntity();
            repository = asset.getRepository();
            logger.debug("Repository statistics is outdated because asset is updated");
        }

        setRepositoryStatistisOutOfDate(repository);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        Repository repository = null;

        if (event.getEntity() instanceof RepositoryLocale) {
            RepositoryLocale repositoryLocale = (RepositoryLocale) event.getEntity();
            repository = repositoryLocale.getRepository();
            logger.debug("Repository statistics is outdated because locale is deleted");
        } else if (event.getEntity() instanceof TMTextUnitCurrentVariant) {
            TMTextUnitCurrentVariant tmTextUnitCurrentVariant = (TMTextUnitCurrentVariant) event.getEntity();
            repository = tmTextUnitCurrentVariant.getTmTextUnit().getAsset().getRepository();
            logger.debug("Repository statistics is outdated because translation is deleted");
        }

        setRepositoryStatistisOutOfDate(repository);
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister event) {
        return true;
    }

    private void setRepositoryStatistisOutOfDate(Repository repository) {
        if (repository != null) {
            repositoryStatisticService.generateRepositoryStatsOutOfDateEvent(repository.getId());
        }
    }

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent pde) {
    }

    @Override
    public void onPostInsertCommitFailed(PostInsertEvent pie) {
    }

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent pue) {
    }
}
