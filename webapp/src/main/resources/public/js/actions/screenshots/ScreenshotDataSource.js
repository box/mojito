import ScreenshotActions from "./ScreenshotActions";
import ScreenshotClient from "../../sdk/ScreenshotClient";
import ScreenshotsPageStore from "../../stores/screenshots/ScreenshotsPageStore";

const ScreenshotDataSource = {
    changeStatus: {
        remote(state, status, comment, idx) {

            const screenshot = ScreenshotsPageStore.getScreenshotByIdx(idx);
            screenshot.status = status;
            screenshot.comment = comment;

            const promise = ScreenshotClient.updateScreenshot(screenshot).then(() => {
                return {
                    status: status,
                    comment: comment,
                    idx: idx
                };
            });

            return promise;
        },
        success: ScreenshotActions.changeStatusSuccess,
        error: ScreenshotActions.changeStatusError
    },
};

export default ScreenshotDataSource;
