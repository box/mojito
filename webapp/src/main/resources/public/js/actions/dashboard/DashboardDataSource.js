import DashboardClient from "../../sdk/DashboardClient";
import DashboardPageActions from "./DashboardPageActions";
import DashboardSearcherParameters from "../../sdk/DashboardSearcherParameters";
import DashboardSearchParamStore from "../../stores/dashboard/DashboardSearchParamStore";
import ImageClient from "../../sdk/ImageClient";
import ScreenshotClient from "../../sdk/ScreenshotClient";
import ScreenshotRun from "../../sdk/entity/ScreenshotRun";
import DashboardStore from "../../stores/dashboard/DashboardStore";

const DashboardDataSource = {
    performDashboardSearch: {
        remote() {
            let returnEmpty = false;

            let dashboardSearchParam = DashboardSearchParamStore.getState();
            let dashboardSearcherParameters = new DashboardSearcherParameters();

            if (!DashboardSearcherParameters.isReadyForDashboardSearching(dashboardSearcherParameters)) {
                // TODO???
                // returnEmpty = true;
            }

            if (dashboardSearchParam.searchText) {
                dashboardSearcherParameters.search(dashboardSearchParam.searchText);
            }

            if (dashboardSearchParam.isMine) {
                dashboardSearcherParameters.createdByUserName(USERNAME); // UserUtil
            }

            if (!dashboardSearchParam.deleted && !dashboardSearchParam.undeleted) {
                returnEmpty = true;
            } else if (dashboardSearchParam.deleted && !dashboardSearchParam.undeleted) {
                dashboardSearcherParameters.deleted(true);
            } else if (!dashboardSearchParam.deleted && dashboardSearchParam.undeleted) {
                dashboardSearcherParameters.deleted(false);
            }

            dashboardSearcherParameters.page(DashboardStore.getState().currentPageNumber);
            dashboardSearcherParameters.size(DashboardStore.getState().size);

            let promise;

            if (returnEmpty) {
                promise = new Promise(function (resolve, reject) {
                    resolve({
                        "hasNext": false,
                        "size": 10,
                        "content": [],
                        "hasPrevious": false,
                        "number": 0,
                        "first": true,
                        "numberOfElements": 0,
                        "totalPages": 1,
                        "totalElements": 0,
                        "last": true
                    });
                });
            } else {
                promise = DashboardClient.getBranches(dashboardSearcherParameters);
            }

            return promise;
        },
        success: DashboardPageActions.getBranchesSuccess,
        error: DashboardPageActions.getBranchesError

    }
};

export default DashboardDataSource;