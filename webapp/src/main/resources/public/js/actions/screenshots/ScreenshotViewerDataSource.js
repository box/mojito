import ScreenshotClient from "../../sdk/ScreenshotClient.js";

class ScreenshotViewerDataSource {
    constructor(action) {
        this.delete = {
            remote({ branchStatisticScreenshots, number }) {
                const screenshotId = branchStatisticScreenshots[number - 1].id;
                return ScreenshotClient.deleteScreenshot(screenshotId);
            },
            success: action.onDeleteScreenshotSuccess,
            error: action.onDeleteScreenshotFailure
        };
    }
}

export default ScreenshotViewerDataSource;
