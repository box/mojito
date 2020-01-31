package com.box.l10n.mojito.phabricator;

import com.box.l10n.mojito.phabricator.payload.BuildSearchResult;
import com.box.l10n.mojito.phabricator.payload.BuildableSearchResult;
import com.box.l10n.mojito.phabricator.payload.TargetSearchResult;

public class Harbormaster {

    PhabricatorHttpClient phabricatorHttpClient;

    public Harbormaster(PhabricatorHttpClient phabricatorHttpClient) {
        this.phabricatorHttpClient = phabricatorHttpClient;
    }

    /**
     * @param targetPHID general build target eg. from buildkite
     * @return
     */
    String getBuildPHID(String targetPHID) {
        try {
            TargetSearchResult targetSearchResponse = phabricatorHttpClient.postEntityAndCheckResponse(
                    Method.HARBORMASTER_TARGET_SEARCH,
                    phabricatorHttpClient.getConstraintsForPHID(targetPHID),
                    TargetSearchResult.class);
            return targetSearchResponse.getResult().getData().get(0).getFields().getBuildPHID();
        } catch (Exception e) {
            throw new RuntimeException("Can't find build PHID", e);
        }
    }

    /**
     * @param buildPHID {@link #getBuildPHID(String)}
     * @return
     */
    String getBuildablePHID(String buildPHID) {
        try {
            BuildSearchResult buildSearchResponse = phabricatorHttpClient.postEntityAndCheckResponse(
                    Method.HARBORMASTER_BUILD_SEARCH,
                    phabricatorHttpClient.getConstraintsForPHID(buildPHID),
                    BuildSearchResult.class);
            return buildSearchResponse.getResult().getData().get(0).getFields().getBuildablePHID();
        } catch (Exception e) {
            throw new RuntimeException("Can't find buildable PHID", e);
        }
    }

    /**
     * @param buildablePHID {@link #getBuildablePHID(String)}
     * @return
     */
    String getObjectPHID(String buildablePHID) {
        try {
            BuildableSearchResult buildableSearchResponse = phabricatorHttpClient.postEntityAndCheckResponse(
                    Method.HARBORMASTER_BUILDABLE_SEARCH,
                    phabricatorHttpClient.getConstraintsForPHID(buildablePHID),
                    BuildableSearchResult.class);
            return buildableSearchResponse.getResult().getData().get(0).getFields().getObjectPHID();
        } catch (Exception e) {
            throw new RuntimeException("Can't find object PHID", e);
        }
    }
}
