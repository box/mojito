package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.fasterxml.jackson.annotation.JsonView;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author jeanaurambault
 */
@RestController
public class BranchStatisticWS {

    /**
     * logger
     */
    static Logger logger = getLogger(BranchStatisticWS.class);

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    BranchStatisticRepository branchStatisticRepository;

    @Autowired
    BranchStatisticService branchStatisticService;

    @JsonView(View.BranchStatistic.class)
    @RequestMapping(value = "/api/branchStatistics", method = RequestMethod.GET)
    public List<BranchStatistic> getBranchesOfRepository() {
        List<BranchStatistic> all = branchStatisticRepository.findAll();
        return all;
    }
}
