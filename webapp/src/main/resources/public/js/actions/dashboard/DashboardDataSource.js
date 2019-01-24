import DashboardClient from "../../sdk/DashboardClient";
import DashboardPageActions from "./DashboardPageActions";
import DashboardSearcherParameters from "../../sdk/DashboardSearcherParameters";
import DashboardSearchParamStore from "../../stores/Dashboard/DashboardSearchParamStore";
import ImageClient from "../../sdk/ImageClient";
import ScreenshotClient from "../../sdk/ScreenshotClient";
import ScreenshotRun from "../../sdk/entity/ScreenshotRun";
import DashboardStore from "../../stores/Dashboard/DashboardStore";

const DashboardDataSource = {
    performDashboardSearch: {
        remote() {
            let dashboardSearchParam = DashboardSearchParamStore.getState();
            let dashboardSearcherParameters = new DashboardSearcherParameters();

            if (dashboardSearchParam.searchText) {
               dashboardSearcherParameters.search(dashboardSearchParam.searchText);
            }

            if (dashboardSearchParam.isMine) {
                dashboardSearcherParameters.createdByUserName(USERNAME);
            }

            dashboardSearcherParameters.deleted(dashboardSearchParam.deleted);
            dashboardSearcherParameters.undeleted(dashboardSearchParam.undeleted);
            dashboardSearcherParameters.page(DashboardStore.getState().currentPageNumber);
            dashboardSearcherParameters.size(DashboardStore.getState().size);



            if (DashboardSearcherParameters.isReadyForDashboardSearching(dashboardSearcherParameters)) {
                return DashboardClient.getBranches(dashboardSearcherParameters);
            }
        },
        success: DashboardPageActions.getBranchesSuccess,
        error: DashboardPageActions.getBranchesError

    },

    performUploadScreenshotImage: {
        remote(dashboardStoreState, generatedUuid) {
            let image = dashboardStoreState.image;
            return ImageClient.uploadImage(generatedUuid, image.imagePreviewUrl);
        },
        success: DashboardPageActions.uploadScreenshotImageSuccess,
        error: DashboardPageActions.uploadScreenshotImageError
    },

    performUploadScreenshot: {
        remote(dashboardStoreState) {

            let screenshotRun = ScreenshotRun.branchStatisticsToScreenshotRun(dashboardStoreState.branchStatistics[dashboardStoreState.openBranchIndex], dashboardStoreState.image, dashboardStoreState.textUnitChecked[dashboardStoreState.openBranchIndex]);
            return ScreenshotClient.createOrUpdateScreenshotRun(screenshotRun)
        },
        success: DashboardPageActions.uploadScreenshotSuccess,
        error: DashboardPageActions.uploadScreenshotError
    }
};

export default DashboardDataSource;