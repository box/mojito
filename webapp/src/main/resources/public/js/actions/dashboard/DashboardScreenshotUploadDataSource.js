import ImageClient from "../../sdk/ImageClient";
import ScreenshotClient from "../../sdk/ScreenshotClient";
import ScreenshotRun from "../../sdk/entity/ScreenshotRun";
import DashboardScreenshotUploadActions from "./DashboardScreenshotUploadActions";
import DashboardStore from "../../stores/dashboard/DashboardStore";

const DashboardScreenshotUploadDataSource = {

    performUploadScreenshotImage: {
        remote(state, generatedUuid) {
            return ImageClient.uploadImage(generatedUuid, state.imageForUpload);
        },
        success: DashboardScreenshotUploadActions.uploadScreenshotImageSuccess,
        error: DashboardScreenshotUploadActions.uploadScreenshotImageError
    },

    performUploadScreenshot: {
        remote(state) {
            let dashboardStoreState =  DashboardStore.getState();

            let screenshotRun = ScreenshotRun.branchStatisticsToScreenshotRun(
                dashboardStoreState.branchStatistics[dashboardStoreState.openBranchIndex],
                state.screenshotSrc,
                dashboardStoreState.textUnitChecked[dashboardStoreState.openBranchIndex]);

            return ScreenshotClient.createOrUpdateScreenshotRun(screenshotRun);
        },
        success: DashboardScreenshotUploadActions.uploadScreenshotSuccess,
        error: DashboardScreenshotUploadActions.uploadScreenshotError
    }
};

export default DashboardScreenshotUploadDataSource;