import alt from "../../alt";
import ScreenshotsPaginatorActions from "../../actions/screenshots/ScreenshotsPaginatorActions";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions";
import DashboardPaginatorAction from "../../actions/dashboard/DashboardPaginatorActions";
import DashboardPageAction from "../../actions/dashboard/DashboardPageActions";

class DashboardPaginatorStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(DashboardPaginatorAction);
        this.bindActions(DashboardPageAction);
    }

    setDefaultState() {
        this.currentPageNumber = 1;
        this.hasNextPage = true;
        this.disabled = true;
        this.shown = false;
        this.limit = 3;
    }

    goToNextPage() {
        if (this.hasNextPage) {
            this.currentPageNumber++;
        } else {
            console.error("There is no next page, goToNextPage shouldn't be called");
        }
    }

    goToPreviousPage() {
        if (this.currentPageNumber > 1) {
            this.currentPageNumber--;
        } else {
            console.error("There is no previous page, goToPreviousPage shouldn't be called");
        }
    }

    changeCurrentPageNumber(currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
    }

    getBranches() {
        this.disabled = true;
    }

    getBranchesSuccess(result) {
        this.disabled = false;
        this.shown = result.length > 0 || this.currentPageNumber > 1;
        this.hasNextPage = result.length === this.limit;
    }

    getBranchesError() {
        this.disabled = false;
        this.shown = false;
    }

}

export default alt.createStore(DashboardPaginatorAction, 'DashboardPaginatorAction');
