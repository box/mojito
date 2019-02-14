import alt from "../../alt";
import DashboardHistoryActions from "../../actions/dashboard/DashboardHistoryActions";
import ScreenshotsPaginatorStore from "../../stores/screenshots/ScreenshotsPaginatorStore";
import DashboardPageActions from "../../actions/dashboard/DashboardPageActions";
import DashboardPaginatorActions from "../../actions/dashboard/DashboardPaginatorActions";
import DashboardSearchParamsActions from "../../actions/dashboard/DashboardSearchParamsActions";


class DashboardHistoryStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(DashboardHistoryActions);
        this.bindActions(DashboardPageActions);
        this.bindActions(DashboardPaginatorActions);
        this.bindActions(DashboardSearchParamsActions);
        ;
    }

    setDefaultState() {
        // use to skip location update (Initiated by the DashboardPage) when
        // this store is initializing from the browser location or to do 
        // group updates together
        this.skipLocationHistoryUpdate = false;

        this.openBranchStatistic = null;
        // this.selectedBranchTextUnitIds = [];
        this.currentPageNumber = 1;
        this.searchText = "";
        this.deleted = false;
        this.undeleted = true;
        this.onlyMyBranches = true;
    }

    enableHistoryUpdate() {
        this.skipLocationHistoryUpdate = false;
    }

    disableHistoryUpdate() {
        this.skipLocationHistoryUpdate = true;
    }

    changeOpenBranchStatistic(openBranchStatistic) {
        this.openBranchStatistic = openBranchStatistic;
    }

    // changeSelectedBranchTextUnitIds(selectedBranchTextUnitIds) {
    //     this.selectedBranchTextUnitIds = selectedBranchTextUnitIds;
    // }

    changeSearchText(searchText) {
        this.searchText = searchText;
    }

    changeDeleted(deleted) {
        this.deleted = deleted;
    }

    changeUndeleted(undeleted) {
        this.undeleted = undeleted;
    }

    changeOnlyMyBranches(onlyMyBranches) {
        this.onlyMyBranches = onlyMyBranches;
    }

    goToNextPage() {
        this.currentPageNumber = ScreenshotsPaginatorStore.getState().currentPageNumber;
    }

    goToPreviousPage() {
        this.currentPageNumber = ScreenshotsPaginatorStore.getState().currentPageNumber;
    }

    changeCurrentPageNumber(currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
    }

    static getQueryParams() {
        let params = this.getState();
        delete params.skipLocationHistoryUpdate;
        return params;
    }

    static initStoreFromLocationQuery(query) {
        let {
            openBranchStatistic, currentPageNumber, searchText,
            deleted = "false", undeleted = "true", onlyMyBranches = "true"
        } = query;

        let selectedBranchTextUnitIds = query["selectedBranchTextUnitIds[]"];

        DashboardHistoryActions.disableHistoryUpdate();

        if (selectedBranchTextUnitIds) {
            if (!Array.isArray(selectedBranchTextUnitIds)) {
                selectedBranchTextUnitIds = [parseInt(selectedBranchTextUnitIds)];
            } else {
                selectedBranchTextUnitIds = selectedBranchTextUnitIds.map((v) => parseInt(v));
            }
        } else {
            selectedBranchTextUnitIds = [];
        }
        DashboardPageActions.changeSelectedBranchTextUnitIds(selectedBranchTextUnitIds);

        if (!searchText) {
            searchText = "";
        }
        DashboardSearchParamsActions.changeSearchText(searchText);
        DashboardSearchParamsActions.changeDeleted(deleted === "true");
        DashboardSearchParamsActions.changeUndeleted(undeleted === "true");
        DashboardSearchParamsActions.changeOnlyMyBranches(onlyMyBranches === "true");

        if (openBranchStatistic) {
            DashboardPageActions.changeOpenBranchStatistic(parseInt(openBranchStatistic));
        }

        DashboardHistoryActions.enableHistoryUpdate();

        DashboardPageActions.getBranches();
    }
}

export default alt.createStore(DashboardHistoryStore, 'DashboardHistoryStore');
