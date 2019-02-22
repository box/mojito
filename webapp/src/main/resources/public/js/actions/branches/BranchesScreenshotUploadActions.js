import alt from "../../alt";

class BranchesScreenshotUploadActions {

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

export default alt.createActions(BranchesScreenshotUploadActions);