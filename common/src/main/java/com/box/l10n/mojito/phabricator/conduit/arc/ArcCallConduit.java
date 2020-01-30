package com.box.l10n.mojito.phabricator.conduit.arc;

import com.box.l10n.mojito.phabricator.conduit.payload.BuildSearchResponse;
import com.box.l10n.mojito.phabricator.conduit.payload.BuildableSearchResponse;
import com.box.l10n.mojito.phabricator.conduit.payload.Data;
import com.box.l10n.mojito.phabricator.conduit.payload.DiffSearchResponse;
import com.box.l10n.mojito.phabricator.conduit.payload.ResponseWithError;
import com.box.l10n.mojito.phabricator.conduit.Method;
import com.box.l10n.mojito.phabricator.conduit.payload.Constraints;
import com.box.l10n.mojito.phabricator.conduit.payload.RevisionSearchFields;
import com.box.l10n.mojito.phabricator.conduit.payload.RevisionSearchResponse;
import com.box.l10n.mojito.phabricator.conduit.payload.TargetSearchResponse;
import com.google.common.base.Preconditions;

import java.util.Arrays;

public class ArcCallConduit {

    ArcCallConduitShell arcCallConduitShell;

    public ArcCallConduit(ArcCallConduitShell arcCallConduitShell) {
        this.arcCallConduitShell = arcCallConduitShell;
    }

    public Data<RevisionSearchFields> getRevisionForTargetPhid(String targetPHID) {
        String buildPHID = getBuildPHID(targetPHID);
        String buildablePHID = getBuildablePHID(buildPHID);
        String objectPHID = getObjectPHID(buildablePHID);
        String revisionPHID = getRevisionPHID(objectPHID);
        Data<RevisionSearchFields> revisionSearchFieldsData = getRevision(revisionPHID);
        return revisionSearchFieldsData;
    }

    /**
     * @param targetPHID general build target eg. from buildkite
     * @return
     */
    String getBuildPHID(String targetPHID) {
        try {
            TargetSearchResponse targetSearchResponse = arcCallConduitShell.callConduit(Method.HARBORMASTER_TARGET_SEARCH, getConstraintsForPHID(targetPHID), TargetSearchResponse.class);
            throwIfError(targetSearchResponse);
            return targetSearchResponse.getResponse().getData().get(0).getFields().getBuildPHID();
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
            BuildSearchResponse buildSearchResponse = arcCallConduitShell.callConduit(Method.HARBORMASTER_BUILD_SEARCH, getConstraintsForPHID(buildPHID), BuildSearchResponse.class);
            throwIfError(buildSearchResponse);
            return buildSearchResponse.getResponse().getData().get(0).getFields().getBuildablePHID();
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
            BuildableSearchResponse buildableSearchResponse = arcCallConduitShell.callConduit(Method.HARBORMASTER_BUILDABLE_SEARCH, getConstraintsForPHID(buildablePHID), BuildableSearchResponse.class);
            throwIfError(buildableSearchResponse);
            return buildableSearchResponse.getResponse().getData().get(0).getFields().getObjectPHID();
        } catch (Exception e) {
            throw new RuntimeException("Can't find object PHID", e);
        }
    }

    /**
     * @param objectPHID {@link #getObjectPHID(String)}
     * @return
     */
    String getRevisionPHID(String objectPHID) {
        try {
            DiffSearchResponse diffSearchResponse = arcCallConduitShell.callConduit(Method.DIFFERENTIAL_DIFF_SEARCH, getConstraintsForPHID(objectPHID), DiffSearchResponse.class);
            throwIfError(diffSearchResponse);
            String revisionPHID = diffSearchResponse.getResponse().getData().get(0).getFields().getRevisionPHID();
            return revisionPHID;
        } catch (Exception e) {
            throw new RuntimeException("Can't find revision PHID", e);
        }
    }

    Data<RevisionSearchFields> getRevision(String revisionPHID) {
        try {
            RevisionSearchResponse revisionSearchResponse = arcCallConduitShell.callConduit(Method.DIFFERENTIAL_REVISION_SEARCH, getConstraintsForPHID(revisionPHID), RevisionSearchResponse.class);
            throwIfError(revisionSearchResponse);
            return revisionSearchResponse.getResponse().getData().get(0);
        } catch (Exception e) {
            throw new RuntimeException("Can't find revision", e);
        }
    }

    Constraints getConstraintsForPHID(String phid) {
        Constraints constraints = new Constraints();
        constraints.setPhids(Arrays.asList(phid));
        return constraints;
    }

    void throwIfError(ResponseWithError responseWithError) {
        Preconditions.checkNotNull(responseWithError);
        if (responseWithError.getErrorMessage() != null) {
            throw new RuntimeException(responseWithError.getErrorMessage());
        }
    }
}
