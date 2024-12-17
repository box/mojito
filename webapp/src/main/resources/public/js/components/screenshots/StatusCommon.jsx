import keymirror from "keymirror";

export const StatusCommonTypes = keymirror({
    "ALL": null,
    "ACCEPTED": null,
    "NEEDS_REVIEW": null,
    "REJECTED": null
});

export default class StatusCommon {

    static getScreenshotStatusIntl(intl, status) {
        let statusIntl;

        switch (status) {
            case StatusCommonTypes.ACCEPTED:
                statusIntl = intl.formatMessage({id: "screenshots.reviewModal.accepted"});
                break;
            case StatusCommonTypes.NEEDS_REVIEW:
                statusIntl = intl.formatMessage({id: "screenshots.reviewModal.needsReview"});
                break;
            case StatusCommonTypes.REJECTED:
                statusIntl = intl.formatMessage({id: "screenshots.reviewModal.rejected"});
                break;
        }

        return statusIntl;
    }
}
