package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface BranchNotificationRepository extends JpaRepository<BranchNotification, Long>, JpaSpecificationExecutor<BranchNotification> {

    BranchNotification findByBranchAndSenderType(Branch branch, String senderType);
}
