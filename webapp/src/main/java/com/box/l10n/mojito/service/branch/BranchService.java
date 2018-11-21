package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service to manage {@link Branch}es.
 * <p>
 * When no branch name is specified, a branch with name: null will be used.
 *
 * @author jeanaurambault
 */
@Service
public class BranchService {

    /**
     * logger
     */
    static Logger logger = getLogger(BranchService.class);

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    EntityManager entityManager;

    public Branch createBranch(Repository repository, String branchName) {

        logger.debug("getOrCreateBranch, name: {}, repository id: {}", branchName, repository.getId());

        Branch branch = new Branch();
        branch.setName(branchName);
        branch.setRepository(repository);

        branch = branchRepository.save(branch);

        return branch;
    }


    public Branch getOrCreateBranch(Repository repository, String branchName) {

        logger.debug("getOrCreateBranch, name: {}, repository id: {}", branchName, repository.getId());

        Branch branch = branchRepository.findByNameAndRepository(branchName, repository);

        if (branch == null) {
            branch = createBranch(repository, branchName);
        }

        return branch;
    }

}