import alt from "../../alt";
import ScreenshotsPaginatorActions from "../../actions/screenshots/ScreenshotsPaginatorActions";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions";

class ScreenshotsPaginatorStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(ScreenshotsPaginatorActions);
        this.bindActions(ScreenshotsPageActions);
    }
    
    setDefaultState() {
        this.currentPageNumber = 1;
        this.hasNextPage = true;
        this.disabled = true;
        this.shown = false;
        this.limit = 3;
    }
    
    resetScreenshotSearchParams() {
        this.setDefaultState();
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
    
    performSearch() {
        this.disabled = true;
    }
    
    screenshotsSearchResultsReceivedSuccess(result) {
        this.disabled = false;
        this.shown = result.length > 0 || this.currentPageNumber > 1;
        this.hasNextPage = result.length === this.limit;
    }
    
    screenshotsSearchResultsReceivedError() {
        this.disabled = false;
        this.shown = false;
    }
   
}

export default alt.createStore(ScreenshotsPaginatorStore, 'PaginatorStore');
