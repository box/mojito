package com.box.l10n.mojito.service.gitblame;

import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.GitBlame;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.ThirdPartyTextUnit;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.screenshot.ScreenshotService;
import com.box.l10n.mojito.service.thirdparty.ThirdPartyTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jaurambault
 */
@Component
public class GitBlameService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(GitBlameService.class);

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    GitBlameRepository gitBlameRepository;

    @Autowired
    ScreenshotService screenshotService;

    @Autowired
    QuartzPollableTaskScheduler quartzPollableTaskScheduler;

    @Autowired
    ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

    /**
     * Gets the {@link GitBlameWithUsage} information that matches the search parameters.
     *
     * Use the {@link TextUnitSearcher#search(TextUnitSearcherParameters)} to get the text units for which information
     * is required.
     *
     * For each of text unit DTO that the searcher returns, get the usage and the git blame information
     * to build the payload returned.
     *
     * Usually this method should be called for the root locale since we're looking for information
     * on the source ({@link com.box.l10n.mojito.entity.TMTextUnit} but the search parameters can actuall be anything.
     *
     * @param textUnitSearcherParameters
     * @return
     */
    public List<GitBlameWithUsage> getGitBlameWithUsages(TextUnitSearcherParameters textUnitSearcherParameters) {

        logger.debug("Get the text units required for Git blame operation");
        List<TextUnitDTO> textUnitDTOS = textUnitSearcher.search(textUnitSearcherParameters);

        List<GitBlameWithUsage> gitBlameWithUsages = convertTextUnitsDTOsToGitBlameWithUsages(textUnitDTOS);

        if (!gitBlameWithUsages.isEmpty()) {
            enrichTextUnitsWithUsages(gitBlameWithUsages);
            enrichTextUnitsWithGitBlame(gitBlameWithUsages);
            enrichTextUnitsWithScreenshots(gitBlameWithUsages);
            enrichTextUnitsWithThirdPartyTextUnitId(gitBlameWithUsages);
        }

        return gitBlameWithUsages;
    }

    Set<Long> getTmTextUnitIds(List<GitBlameWithUsage> gitBlameWithUsages) {
        return gitBlameWithUsages.stream().map(GitBlameWithUsage::getTmTextUnitId).collect(Collectors.toSet());
    }

    List<GitBlameWithUsage> convertTextUnitsDTOsToGitBlameWithUsages(List<TextUnitDTO> textUnitDTOS) {
        logger.debug("Convert text unit dto to text unit with usages");

        List<GitBlameWithUsage> gitBlameWithUsages = new ArrayList<>();

        for (TextUnitDTO textUnitDTO : textUnitDTOS) {
            GitBlameWithUsage gitBlameWithUsage = new GitBlameWithUsage();
            gitBlameWithUsage.setTmTextUnitId(textUnitDTO.getTmTextUnitId());
            gitBlameWithUsage.setAssetTextUnitId(textUnitDTO.getAssetTextUnitId());
            gitBlameWithUsage.setTextUnitName(textUnitDTO.getName());
            gitBlameWithUsage.setPluralForm(textUnitDTO.getPluralForm());
            gitBlameWithUsage.setContent(textUnitDTO.getSource());
            gitBlameWithUsage.setComment(textUnitDTO.getComment());
            gitBlameWithUsages.add(gitBlameWithUsage);
        }
        return gitBlameWithUsages;
    }

    void enrichTextUnitsWithGitBlame(List<GitBlameWithUsage> gitBlameWithUsages) {
        logger.debug("Enrich text units with git info");
        Set<Long> tmTextUnitIds = getTmTextUnitIds(gitBlameWithUsages);

        logger.debug("Fetch the Git blame info");
        Map<Long, GitBlame> currentGitBlameForTmTextUnitIds = getCurrentGitBlameForTmTextUnitIds(tmTextUnitIds);

        for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            gitBlameWithUsage.setGitBlame(currentGitBlameForTmTextUnitIds.get(gitBlameWithUsage.getTmTextUnitId()));
        }
        logger.debug("End Enrich text units with git info");
    }

    void enrichTextUnitsWithUsages(List<GitBlameWithUsage> gitBlameWithUsages) {
        logger.debug("Enrich text units with usages");
        Map<Long, GitBlameWithUsage> assetTextUnitIdToGitBlameWithUsage = new HashMap<>();

        for (GitBlameWithUsage textUnitWithUsage : gitBlameWithUsages) {
            if (textUnitWithUsage.getAssetTextUnitId() != null) {
                assetTextUnitIdToGitBlameWithUsage.put(textUnitWithUsage.getAssetTextUnitId(), textUnitWithUsage);
            }
        }

        logger.debug("Fetch the asset text unit information");
        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByIdIn(new ArrayList<Long>(assetTextUnitIdToGitBlameWithUsage.keySet()));

        for (AssetTextUnit assetTextUnit : assetTextUnits) {
            GitBlameWithUsage gitBlameWithUsage = assetTextUnitIdToGitBlameWithUsage.get(assetTextUnit.getId());
            gitBlameWithUsage.setUsages(assetTextUnit.getUsages());
            gitBlameWithUsage.setBranch(assetTextUnit.getBranch());
        }

        logger.debug("End Enrich text units with usages");
    }

    void enrichTextUnitsWithScreenshots(List<GitBlameWithUsage> gitBlameWithUsages) {
        logger.debug("Enrich text units with screenshots");

        Set<Long> tmTextUnitIds = getTmTextUnitIds(gitBlameWithUsages);
        Map<Long, Set<Screenshot>> screenshotsByTextUnit = screenshotService.getScreenshotsByTmTextUnitId(tmTextUnitIds);

        gitBlameWithUsages.forEach(gitBlameWithUsage -> {
            Set<Screenshot> screenshots = screenshotsByTextUnit.get(gitBlameWithUsage.getTmTextUnitId());
            screenshots = MoreObjects.firstNonNull(screenshots, Collections.emptySet());
            gitBlameWithUsage.setScreenshots(screenshots);
        });
    }

    void enrichTextUnitsWithThirdPartyTextUnitId(List<GitBlameWithUsage> gitBlameWithUsages) {

        HashMap<Long, GitBlameWithUsage> gitBlameWithUsagesByTmTextUnitId = getGitBlameWithUsagesByTmTextUnitId(gitBlameWithUsages);
        List<ThirdPartyTextUnit> thirdPartyTextUnitList = thirdPartyTextUnitRepository.findByTmTextUnitIdIn(
                gitBlameWithUsagesByTmTextUnitId.keySet());

        for (ThirdPartyTextUnit thirdPartyTextUnit : thirdPartyTextUnitList) {
            GitBlameWithUsage gitBlameWithUsage = gitBlameWithUsagesByTmTextUnitId.get(thirdPartyTextUnit.getTmTextUnit().getId());
            gitBlameWithUsage.setThirdPartyTextUnitId(thirdPartyTextUnit.getThirdPartyId());
        }
    }

    /**
     * Save the GitBlame information for a list of {@link com.box.l10n.mojito.entity.TMTextUnit}. The text units
     * are identified by the {@link GitBlame#getId()} and the git blame info come for the {@link GitBlameWithUsage#getGitBlame()}
     * method.
     *
     * @param gitBlameWithUsages
     * @return
     */
    @Transactional
    @Pollable(async = true, message = "Save git blame information")
    public PollableFuture saveGitBlameWithUsages(List<GitBlameWithUsage> gitBlameWithUsages) {

        HashMap<Long, GitBlameWithUsage> gitBlameWithUsagesByTmTextUnitId = getGitBlameWithUsagesByTmTextUnitId(gitBlameWithUsages);

        Map<Long, GitBlame> currentGitBlameForTmTextUnitIds = getCurrentGitBlameForTmTextUnitIds(gitBlameWithUsagesByTmTextUnitId.keySet());

        for (Map.Entry<Long, GitBlameWithUsage> gitBlameWithUsageEntry : gitBlameWithUsagesByTmTextUnitId.entrySet()) {
            Long tmTextUnitId = gitBlameWithUsageEntry.getKey();
            GitBlameWithUsage gitBlameWithUsage = gitBlameWithUsageEntry.getValue();

            GitBlame gitBlame = currentGitBlameForTmTextUnitIds.get(tmTextUnitId);

            if (gitBlame == null) {
                logger.debug("No GitBlame information for tmTextUnitId: {}", tmTextUnitId);
                gitBlame = new GitBlame();
                gitBlame.setTmTextUnit(tmTextUnitRepository.getOne(tmTextUnitId));
            } else {
                logger.debug("Found GitBlame for tmTextUnitId: {}, update", tmTextUnitId);
            }


            GitBlame gitBlameFromInput = gitBlameWithUsage.getGitBlame();

            if (gitBlameFromInput != null) {
                gitBlame.setAuthorEmail(gitBlameFromInput.getAuthorEmail());
                gitBlame.setAuthorName(gitBlameFromInput.getAuthorName());
                gitBlame.setCommitName(gitBlameFromInput.getCommitName());
                gitBlame.setCommitTime(gitBlameFromInput.getCommitTime());
            }

            gitBlameRepository.save(gitBlame);
        }

        return new PollableFutureTaskResult();
    }

    Map<Long, GitBlame> getCurrentGitBlameForTmTextUnitIds(Set<Long> tmTextUnitIds) {
        Map<Long, GitBlame> gitBlameMap = new HashMap<>();

        List<GitBlame> byTmTextUnitIdIn = gitBlameRepository.findByTmTextUnitIdIn(tmTextUnitIds);

        int i = 0;
        for (GitBlame gitBlame : byTmTextUnitIdIn) {
            try {
                gitBlameMap.put(gitBlame.getTmTextUnit().getId(), gitBlame);
                i++;
            } catch (Exception e) {
                logger.error("getCurrentGitBlameForTmTextUnitIds, id: {} after: {}", gitBlame.getId(), i);
            }
        }

        return gitBlameMap;
    }

    /**
     * Builds a map from {@link com.box.l10n.mojito.entity.TMTextUnit#id} id to {@link GitBlameWithUsage}, removing
     * duplicated entries for a given {@link com.box.l10n.mojito.entity.TMTextUnit#id}. The first entry in the list
     * will be used to get the git information. This is useful in case where there are multiple entries for different
     * plural forms. The client may end up sending the same information for each form but we just need to save it
     * once since it is the same entity. We take arbitrarly the first entity.
     *
     * @param gitBlameWithUsages
     * @return
     */
    HashMap<Long, GitBlameWithUsage> getGitBlameWithUsagesByTmTextUnitId(List<GitBlameWithUsage> gitBlameWithUsages) {

        HashMap<Long, GitBlameWithUsage> tmTextUnitIdToGitBlameWithUsage = new HashMap();

        for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            Long tmTextUnitId = gitBlameWithUsage.getTmTextUnitId();

            if (tmTextUnitIdToGitBlameWithUsage.get(tmTextUnitId) == null) {
                tmTextUnitIdToGitBlameWithUsage.put(tmTextUnitId, gitBlameWithUsage);
            } else {
                logger.debug("Already provided, skip it for tmTextUnitId: {}", tmTextUnitId);
            }
        }

        return tmTextUnitIdToGitBlameWithUsage;
    }
}
