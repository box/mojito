import alt from "../../alt";

class DashboardScreenshotUploadActions {

    constructor() {
        this.generateActions(
            "openWithBranch",
            "close",
            "changeSelectedFiles",
            "changeImageForPreview",
            "changeImageForUpload",


            "uploadScreenshotImage",
            "uploadScreenshotImageSuccess",
            "uploadScreenshotImageError",

            "uploadScreenshot",
            "uploadScreenshotSuccess",
            "uploadScreenshotError"
        );
    }
}

export default alt.createActions(DashboardScreenshotUploadActions);