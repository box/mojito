package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.service.reason.Reason;
import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotationType;
import net.sf.okapi.common.annotation.ITSLQIAnnotations;
import net.sf.okapi.common.annotation.IssueAnnotation;
import net.sf.okapi.common.resource.TextContainer;
import java.util.List;

/**
 * @author wyau
 */
public class IssueWithReasonAnnotation {

    protected static final String LOC_QUALITY_ISSUE_REASON = "locQualityIssueReason";

    /**
     * Add an issue {@link Reason} to the {@link TextContainer}
     *
     * @param textContainer
     * @param reason
     */
    public static void addIssueReasonToTextContainer(TextContainer textContainer, Reason reason) {
        IssueAnnotation issueAnnotation = new IssueAnnotation();
        issueAnnotation.setString(LOC_QUALITY_ISSUE_REASON, reason.toString());
        ITSLQIAnnotations.addAnnotations(textContainer, issueAnnotation);
    }

    /**
     * Get an issue {@link Reason} that is in the {@link TextContainer}.
     *
     * @param textContainer the {@link TextContainer} to get the {@link Reason} from
     * @return null if there isn't a {@link Reason} in the {@link TextContainer}
     */
    public static Reason getIssueReasonFromTextContainer(TextContainer textContainer) {
        ITSLQIAnnotations annotation = textContainer.getAnnotation(ITSLQIAnnotations.class);

        Reason result = null;
        if (annotation != null) {
            List<GenericAnnotation> annotations = annotation.getAnnotations(GenericAnnotationType.LQI);

            if (!annotations.isEmpty()) {
                IssueAnnotation issueAnnotation = (IssueAnnotation) annotations.get(0);
                String issueAnnotationString = issueAnnotation.getString(LOC_QUALITY_ISSUE_REASON);
                result = Reason.valueOf(issueAnnotationString);
            }
        }

        return result;
    }
}
