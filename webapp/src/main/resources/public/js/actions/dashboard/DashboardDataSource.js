import DashboardClient from "../../sdk/DashboardClient";
import DashboardPageActions from "./DashboardPageActions";
import DashboardSearcherParameters from "../../sdk/DashboardSearcherParameters";
import DashboardSearchParamStore from "../../stores/dashboard/DashboardSearchParamStore";
import DashboardPaginatorStore from "../../stores/dashboard/DashboardPaginatorStore";

const DashboardDataSource = {
    performDashboardSearch: {
        remote() {
            let returnEmpty = false;

            let dashboardSearchParam = DashboardSearchParamStore.getState();
            let dashboardPaginatorStore = DashboardPaginatorStore.getState();


            let dashboardSearcherParameters = new DashboardSearcherParameters();

            if (!DashboardSearcherParameters.isReadyForDashboardSearching(dashboardSearcherParameters)) {
                // TODO???
                // returnEmpty = true;
            }

            if (dashboardSearchParam.searchText) {
                dashboardSearcherParameters.search(dashboardSearchParam.searchText);
            }

            if (dashboardSearchParam.onlyMyBranches) {
                dashboardSearcherParameters.createdByUserName(USERNAME); //TODO(ja) UserUtil
            }

            if (!dashboardSearchParam.deleted && !dashboardSearchParam.undeleted) {
                returnEmpty = true;
            } else if (dashboardSearchParam.deleted && !dashboardSearchParam.undeleted) {
                dashboardSearcherParameters.deleted(true);
            } else if (!dashboardSearchParam.deleted && dashboardSearchParam.undeleted) {
                dashboardSearcherParameters.deleted(false);
            }

            dashboardSearcherParameters.page(dashboardPaginatorStore.currentPageNumber - 1);
            dashboardSearcherParameters.size(dashboardPaginatorStore.limit);

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