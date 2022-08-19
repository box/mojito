package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.PullRunTextUnitVariant;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service to manage PullRun data.
 * 
 * @author garion
 */
@Service
public class PullRunService {

    @Autowired
    PullRunRepository pullRunRepository;

    public PullRun getOrCreate(String pullRunName, Repository repository) {
        return pullRunRepository.findByName(pullRunName).orElseGet(() -> {
            PullRun pullRun = new PullRun();
            pullRun.setName(pullRunName);
            pullRun.setRepository(repository);
            pullRunRepository.save(pullRun);
            return pullRun;
        });
    }
}
