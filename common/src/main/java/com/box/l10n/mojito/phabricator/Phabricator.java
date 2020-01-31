package com.box.l10n.mojito.phabricator;

import com.box.l10n.mojito.phabricator.payload.Data;
import com.box.l10n.mojito.phabricator.payload.RevisionSearchFields;

public class Phabricator {

    DifferentialDiff differentialDiff;

    Harbormaster harbormaster;

    DifferentialRevision differentialRevision;

    public Phabricator(DifferentialDiff differentialDiff,
                       Harbormaster harbormaster,
                       DifferentialRevision differentialRevision) {
        this.differentialDiff = differentialDiff;
        this.harbormaster = harbormaster;
        this.differentialRevision = differentialRevision;
    }

    /**
     * Get revision information for a harbormaster target phid
     *
     * @param harbormasterTargetPHID
     * @return
     */
    public Data<RevisionSearchFields> getRevisionForTargetPhid(String harbormasterTargetPHID) {
        String buildPHID = harbormaster.getBuildPHID(harbormasterTargetPHID);
        String buildablePHID = harbormaster.getBuildablePHID(buildPHID);
        String objectPHID = harbormaster.getObjectPHID(buildablePHID);
        String revisionPHID = differentialDiff.getRevisionPHID(objectPHID);
        Data<RevisionSearchFields> revisionSearchFieldsData = differentialRevision.getRevision(revisionPHID);
        return revisionSearchFieldsData;
    }

    public DifferentialDiff getDifferentialDiff() {
        return differentialDiff;
    }

    public Harbormaster getHarbormaster() {
        return harbormaster;
    }

    public DifferentialRevision getDifferentialRevision() {
        return differentialRevision;
    }
}
