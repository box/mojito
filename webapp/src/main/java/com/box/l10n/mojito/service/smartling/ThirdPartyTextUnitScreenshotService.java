package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.rest.images.ImageWS;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.image.ImageRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.request.File;
import com.box.l10n.mojito.smartling.response.FilesResponse;
import com.box.l10n.mojito.smartling.response.StringToContextBindingResponse;
import com.box.l10n.mojito.smartling.response.UploadContextResponse;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service
public class ThirdPartyTextUnitScreenshotService {

    private static Logger logger = LoggerFactory.getLogger(ThirdPartyTextUnitScreenshotService.class);

    @Autowired
    EntityManager entityManager;

    @Autowired
    SmartlingClient smartlingClient;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    ThirdPartyTextUnitMatchingService thirdPartyTextUnitMatchingService;

    @Autowired
    ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

    @Autowired
    ThirdPartyTextUnitScreenshotRepository thirdPartyTextUnitScreenshotRepository;

    @Autowired
    ImageRepository imageRepository;

    final private int maxBindings = 150;

    ImageWS imageWS = new ImageWS();

    public void pushNewScreenshots(String projectId) throws HttpStatusCodeException {
        FilesResponse files = smartlingClient.getFiles(projectId);
        if (files.isSuccessResponse()) {
            List<File> fileList = files.getResponse().getData().getItems().stream().filter(file -> {
                Matcher fileUriMatcher = thirdPartyTextUnitMatchingService.filePattern.matcher(file.getFileUri());
                return fileUriMatcher.matches();
            }).collect(Collectors.toList());
            if (!fileList.isEmpty()) {
                List<ThirdPartyTextUnitScreenshot> newScreenshots = pushNewScreenshotsByRepoAndAsset(
                        fileList.get(0), projectId);
                createThirdPartyIdToContextIdBinding(newScreenshots, projectId);
            }
        } else {
            logger.error("API request to files API failed: {}", files.getErrorMessage());
        }
    }

    List<ThirdPartyTextUnitScreenshot> pushNewScreenshotsByRepoAndAsset(File file, String projectId) throws HttpStatusCodeException {
        Repository repository = repositoryRepository.findByName(file.getFileUri().split("/")[0]);
        List<ThirdPartyTextUnitScreenshot> newScreenshots = new ArrayList<>();
        if (repository != null) {
            List<Long> assetIds = getDistinctAssetIdsWithThirdPartyTextUnitsByRepo(repository.getId());
            List<ScreenshotTextUnit> screenshotTextUnitList = new ArrayList<>();
            assetIds.forEach(assetId -> {
                screenshotTextUnitList.addAll(getScreenshotTextUnitsWithUnpushedScreenshotsByAsset(assetId));
            });
            logger.debug("new screenshots to push to third party({}): {}", screenshotTextUnitList.size(), screenshotTextUnitList);
            newScreenshots = pushContextToThirdParty(screenshotTextUnitList, projectId);
        }
        return newScreenshots;
    }

    List<Long> getDistinctAssetIdsWithThirdPartyTextUnitsByRepo(Long repositoryId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<ThirdPartyTextUnit> thirdPartyTextUnitRoot = query.from(ThirdPartyTextUnit.class);
        Join<ThirdPartyTextUnit, TMTextUnit> tmTextUnitJoin = thirdPartyTextUnitRoot.join(ThirdPartyTextUnit_.tmTextUnit);
        Join<TMTextUnit, Asset> assetJoin = tmTextUnitJoin.join(TMTextUnit_.asset);
        Join<Asset, Repository> repositoryJoin = assetJoin.join(Asset_.repository);

        Predicate predicate = repositoryJoin.get(Repository_.id).in(repositoryId);
        query.where(predicate);

        return entityManager.createQuery(query.distinct(true).select(assetJoin.get(Asset_.id))).getResultList();
    }

    List<ScreenshotTextUnit> getScreenshotTextUnitsWithUnpushedScreenshotsByAsset(Long assetId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ScreenshotTextUnit> query = builder.createQuery(ScreenshotTextUnit.class);

        Root<ScreenshotTextUnit> screenshotTextUnitRoot = query.from(ScreenshotTextUnit.class);
        Join<ScreenshotTextUnit, Screenshot> screenshotJoin = screenshotTextUnitRoot.join(ScreenshotTextUnit_.screenshot);
        Join<ScreenshotTextUnit, TMTextUnit> tmTextUnitJoin = screenshotTextUnitRoot.join(ScreenshotTextUnit_.tmTextUnit);
        Join<TMTextUnit, Asset> assetJoin = tmTextUnitJoin.join(TMTextUnit_.asset);

        Subquery<ThirdPartyTextUnitScreenshot> thirdPartyTextUnitScreenshotSubquery = query.subquery(ThirdPartyTextUnitScreenshot.class);
        Root<ThirdPartyTextUnitScreenshot> thirdPartyTextUnitScreenshotRoot = thirdPartyTextUnitScreenshotSubquery.from(
                ThirdPartyTextUnitScreenshot.class);
        Join<ThirdPartyTextUnitScreenshot, ThirdPartyTextUnit> thirdPartyTextUnitJoin = thirdPartyTextUnitScreenshotRoot.join(
                ThirdPartyTextUnitScreenshot_.thirdPartyTextUnit);
        Join<ThirdPartyTextUnit, TMTextUnit> thirdPartyTextUnitTMTextUnitJoin = thirdPartyTextUnitJoin.join(
                ThirdPartyTextUnit_.tmTextUnit);
        Join<TMTextUnit, Asset> tmTextUnitAssetJoin = thirdPartyTextUnitTMTextUnitJoin.join(TMTextUnit_.asset);

        Predicate screenshotSubqueryConjunction = builder.conjunction();
        screenshotSubqueryConjunction.getExpressions().add(tmTextUnitAssetJoin.get(Asset_.id).in(assetId));
        thirdPartyTextUnitScreenshotSubquery.select(thirdPartyTextUnitScreenshotRoot);
        thirdPartyTextUnitScreenshotSubquery.where(screenshotSubqueryConjunction);

        Predicate mainQueryConjunction = builder.conjunction();
        mainQueryConjunction.getExpressions().add(assetJoin.get(Asset_.id).in(assetId));
        mainQueryConjunction.getExpressions().add(builder.not(builder.exists(thirdPartyTextUnitScreenshotSubquery)));
        mainQueryConjunction.getExpressions().add(screenshotJoin.get(Screenshot_.status).in(Screenshot.Status.ACCEPTED));
        query.where(mainQueryConjunction);

        return entityManager.createQuery(query.select(screenshotTextUnitRoot)).getResultList();
    }

    List<ThirdPartyTextUnitScreenshot> pushContextToThirdParty(List<ScreenshotTextUnit> screenshotTextUnitList, String projectId) throws HttpStatusCodeException {
        return screenshotTextUnitList
                .stream()
                .map(screenshotTextUnit -> {
                    ThirdPartyTextUnitScreenshot thirdPartyTextUnitScreenshot = new ThirdPartyTextUnitScreenshot();
                    Image image = imageRepository.findByName(screenshotTextUnit.getScreenshot().getName());
                    if (image != null) {
                        UploadContextResponse uploadContextResponse = smartlingClient.uploadContext(
                                    projectId, image.getContent(), image.getName(), imageWS.getMediaTypeFromImageName(image.getName()));
                        if (uploadContextResponse.isSuccessResponse()) {
                            ThirdPartyTextUnit thirdPartyTextUnit = thirdPartyTextUnitRepository.findByTmTextUnitId(
                                    screenshotTextUnit.getTmTextUnit().getId());
                            thirdPartyTextUnitScreenshot.setThirdPartyTextUnit(thirdPartyTextUnit);
                            thirdPartyTextUnitScreenshot.setScreenshotTextUnit(screenshotTextUnit);
                            thirdPartyTextUnitScreenshot.setThirdPartyScreenshotId(uploadContextResponse.getResponse().getData().getContextUid());
                            logger.debug("pushed new context {} for screenshot text unit {} and third party text unit {}",
                                    thirdPartyTextUnitScreenshot.getThirdPartyScreenshotId(),
                                    thirdPartyTextUnitScreenshot.getScreenshotTextUnit().getId(),
                                    thirdPartyTextUnitScreenshot.getThirdPartyTextUnit().getThirdPartyTextUnitId());
                        }
                    }
                    return thirdPartyTextUnitScreenshot;
                })
                .collect(Collectors.toList());
    }

    void createThirdPartyIdToContextIdBinding(List<ThirdPartyTextUnitScreenshot> newScreenshots, String projectId) {
        List<List<ThirdPartyTextUnitScreenshot>> partitionedNewScreenshots = Lists.partition(newScreenshots, maxBindings);
        partitionedNewScreenshots.forEach( newScreenshotPartition -> {
            List<Map<String, String>> bindings = new ArrayList<>();
            newScreenshotPartition.forEach( newScreenshot -> {
                Map<String, String> pairMap = new HashMap<>();
                pairMap.put("stringHashcode", newScreenshot.getThirdPartyTextUnit().getThirdPartyTextUnitId());
                pairMap.put("contextUid", newScreenshot.getThirdPartyScreenshotId());
                bindings.add(pairMap);
            });
            StringToContextBindingResponse contextBindingResponse = smartlingClient.createStringToContextBindings(bindings, projectId);
            if (contextBindingResponse.isSuccessResponse()) {
                thirdPartyTextUnitScreenshotRepository.save(newScreenshots);
            }
        });
    }

}
