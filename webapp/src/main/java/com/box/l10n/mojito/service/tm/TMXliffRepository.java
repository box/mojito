package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.TMXliff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jyi
 */
@RepositoryRestResource(exported = false)
public interface TMXliffRepository extends JpaRepository<TMXliff, Long> {

  TMXliff findByPollableTask(PollableTask pollableTask);
}
