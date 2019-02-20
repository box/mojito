import ImageClient from "../../sdk/ImageClient";
import ScreenshotClient from "../../sdk/ScreenshotClient";
import ScreenshotRun from "../../sdk/entity/ScreenshotRun";
import DashboardScreenshotUploadActions from "./DashboardScreenshotUploadActions";
import DashboardStore from "../../stores/dashboard/DashboardStore";
import Screenshot from "../../sdk/entity/Screenshot";
import uuidv4 from "uuid/v4";

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
            let dashboardStoreState = DashboardStore.getState();

            let branchStatistic = DashboardStore.getSelectedBranchStatistic();
            let repository = branchStatistic.branch.repository;

            let screenshotRun =  new ScreenshotRun();
            screenshotRun.id = repository.manualScreenshotRun.id;

            let screenshot = new Screenshot();
            screenshotRun.screenshots.push(screenshot);

            screenshot.name = uuidv4();
            screenshot.src = state.screenshotSrc;
            screenshot.locale = repository.sourceLocale;

            dashboardStoreState.selectedBranchTextUnitIds.forEach((id) => {
               screenshot.textUnits.push({id: id});
            });

            return ScreenshotClient.createOrUpdateScreenshotRun(screenshotRun);
        },
        success: DashboardScreenshotUploadActions.uploadScreenshotSuccess,
        error: DashboardScreenshotUploadActions.uploadScreenshotError
    }
};

export default DashboardScreenshotUploadDataSource;