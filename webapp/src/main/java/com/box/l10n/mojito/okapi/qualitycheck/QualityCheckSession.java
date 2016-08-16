package com.box.l10n.mojito.okapi.qualitycheck;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.lib.verification.Issue;
import net.sf.okapi.lib.verification.Parameters;
import java.util.List;

/**
 * This class overrides the report generation behavior of the base QualityCheckSession.
 * Because we are only interested in getting the errors found, we don't need to generate
 * any report.
 *
 * @author aloison
 */
public class QualityCheckSession extends net.sf.okapi.lib.verification.QualityCheckSession {

    public QualityCheckSession() {
        super();
    }

    @Override
    public void setParameters(Parameters params) {
        // Overwrite some params to make sure we don't save anything on disk
        params.setSaveSession(false);
        params.setAutoOpen(false);
        super.setParameters(params);
    }

    @Override
    public void generateReport(String rootDir) {
        // do not generate any report
    }

    /**
     * @param textUnit
     * @return The issues currently found for the given text units
     */
    protected List<Issue> getIssuesForTextUnit(ITextUnit textUnit) {

        final String textUnitId = textUnit.getId();
        List<Issue> textUnitIssues = this.getIssues();

        Predicate<Issue> filterPredicate = new Predicate<Issue>() {
            @Override
            public boolean apply(Issue issue) {
                return issue.getTuId().equals(textUnitId);
            }
        };

        return Lists.newArrayList(Iterables.filter(textUnitIssues, filterPredicate));
    }
}
