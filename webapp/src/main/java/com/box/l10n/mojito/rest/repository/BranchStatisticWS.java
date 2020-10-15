package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.rest.PageView;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.fasterxml.jackson.annotation.JsonView;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.box.l10n.mojito.rest.repository.BranchStatisticSpecification.branchEquals;
import static com.box.l10n.mojito.rest.repository.BranchStatisticSpecification.branchNameEquals;
import static com.box.l10n.mojito.rest.repository.BranchStatisticSpecification.createdByUserNameEquals;
import static com.box.l10n.mojito.rest.repository.BranchStatisticSpecification.deletedEquals;
import static com.box.l10n.mojito.rest.repository.BranchStatisticSpecification.empty;
import static com.box.l10n.mojito.rest.repository.BranchStatisticSpecification.search;
import static com.box.l10n.mojito.rest.repository.BranchStatisticSpecification.totalCountLessThanOrEqualsTo;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.data.jpa.domain.Specification.where;

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
    public Page<BranchStatistic> getBranchesOfRepository(
            @RequestParam(value = "createdByUserName", required = false) String createdByUserName,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "branchName", required = false) String branchName,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "deleted", required = false) Boolean deleted,
            @RequestParam(value = "empty", required = false) Boolean empty,
            @RequestParam(value = "totalCountLte", required = false, defaultValue = "30000") Long totalCountLte,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<BranchStatistic> page = branchStatisticRepository.findAll(where(
                ifParamNotNull(createdByUserNameEquals(createdByUserName))).and(
                ifParamNotNull(branchEquals(branchId))).and(
                ifParamNotNull(branchNameEquals(branchName))).and(
                ifParamNotNull(search(search))).and(
                ifParamNotNull(deletedEquals(deleted))).and(
                ifParamNotNull(empty(empty))).and(
                totalCountLessThanOrEqualsTo(totalCountLte))
                , pageable);

        return new PageView<>(page);
    }
}
