import alt from "../../alt";
import BranchesDataSource from "../../actions/branches/BranchesHistoryActions";
import BranchesPaginatorStore from "../../stores/branches/BranchesPaginatorStore";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";
import BranchesPaginatorActions from "../../actions/branches/BranchesPaginatorActions";
import BranchesSearchParamsActions from "../../actions/branches/BranchesSearchParamsActions";

class BranchesHistoryStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(BranchesDataSource);
        this.bindActions(BranchesPageActions);
        this.bindActions(BranchesPaginatorActions);
        this.bindActions(BranchesSearchParamsActions);
        ;
    }

    setDefaultState() {
        // use to skip location update (Initiated by the BranchesPage) when
        // this store is initializing from the browser location or to do 
        // group updates together
        this.skipLocationHistoryUpdate = false;

        this.openBranchStatistic = null;
        // this.selectedBranchTextUnitIds = [];
        this.currentPageNumber = 1;
        this.searchText = "";
        this.deleted = false;
        this.undeleted = true;
        this.empty = true;
        this.notEmpty = false;
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

    changeSearchText(searchText) {
        this.searchText = searchText;
    }

    changeDeleted(deleted) {
        this.deleted = deleted;
    }

    changeUndeleted(undeleted) {
        this.undeleted = undeleted;
    }

    changeEmpty(empty) {
        this.empty = empty;
    }

    changeNotEmpty(notEmpty) {
        this.notEmpty = notEmpty;
    }

    changeOnlyMyBranches(onlyMyBranches) {
        this.onlyMyBranches = onlyMyBranches;
    }

    goToNextPage() {
        this.currentPageNumber = BranchesPaginatorStore.getState().currentPageNumber;
    }

    goToPreviousPage() {
        this.currentPageNumber = BranchesPaginatorStore.getState().currentPageNumber;
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
            deleted = "false", undeleted = "true", empty = "true", notEmpty = "true", onlyMyBranches = "true"
        } = query;

        let selectedBranchTextUnitIds = query["selectedBranchTextUnitIds[]"];

        BranchesDataSource.disableHistoryUpdate();

        if (selectedBranchTextUnitIds) {
            if (!Array.isArray(selectedBranchTextUnitIds)) {
                selectedBranchTextUnitIds = [parseInt(selectedBranchTextUnitIds)];
            } else {
                selectedBranchTextUnitIds = selectedBranchTextUnitIds.map((v) => parseInt(v));
            }
        } else {
            selectedBranchTextUnitIds = [];
        }
        BranchesPageActions.changeSelectedBranchTextUnitIds(selectedBranchTextUnitIds);

        if (!searchText) {
            searchText = "";
        }

        BranchesPaginatorActions.changeCurrentPageNumber(currentPageNumber);
        BranchesSearchParamsActions.changeSearchText(searchText);
        BranchesSearchParamsActions.changeDeleted(deleted === "true");
        BranchesSearchParamsActions.changeUndeleted(undeleted === "true");
        BranchesSearchParamsActions.changeEmpty(empty === "true");
        BranchesSearchParamsActions.changeNotEmpty(notEmpty === "true");
        BranchesSearchParamsActions.changeOnlyMyBranches(onlyMyBranches === "true");

        if (openBranchStatistic) {
            BranchesPageActions.changeOpenBranchStatistic(parseInt(openBranchStatistic));
        }

        BranchesDataSource.enableHistoryUpdate();
    }
}

export default alt.createStore(BranchesHistoryStore, 'BranchesHistoryStore');
