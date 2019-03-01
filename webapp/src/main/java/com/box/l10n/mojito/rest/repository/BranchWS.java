package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.service.branch.BranchService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import static org.slf4j.LoggerFactory.getLogger;

@RestController
public class BranchWS {
    /**
     * logger
     */
    static Logger logger = getLogger(BranchWS.class);

    @Autowired
    BranchService branchService;


    @RequestMapping(value = "/api/branch", method = RequestMethod.DELETE)
    public void deleteBranch(@RequestParam(value = "branchId") Long branchId,
                             @RequestParam(value = "repositoryId") Long repositoryId) {
        logger.debug("Deleting branch {} in repository {}", repositoryId, branchId);
        branchService.deleteBranchAsset(branchId, repositoryId);
        logger.debug("All assets in branch {} are deleted.", branchId);
        branchService.markBranchDeleted(branchId);
        logger.debug("Branch {} is marked as deleted", branchId);
    }
}
