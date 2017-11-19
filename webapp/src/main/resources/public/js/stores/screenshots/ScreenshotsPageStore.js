import alt from "../../alt";
import ScreenshotsDataSource from "../../actions/screenshots/ScreenshotsDataSource";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions";
import ScreenshotActions from "../../actions/screenshots/ScreenshotActions";
import ScreenshotsRepositoryActions from "../../actions/screenshots/ScreenshotsRepositoryActions";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import ScreenshotsRepositoryStore from "./ScreenshotsRepositoryStore";
import ScreenshotsLocaleStore from "./ScreenshotsLocaleStore";
import ScreenshotsSearchTextStore from "./ScreenshotsSearchTextStore";

class ScreenshotsPageStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(ScreenshotsPageActions);
        this.bindActions(ScreenshotActions);
        this.registerAsync(ScreenshotsDataSource);
    }
    
    setDefaultState() {
        this.selectedScreenshotIdx = 0;
        this.screenshotsData = []; 
        this.searching = false;
    }
    
    resetScreenshotSearchParams() {
        this.setDefaultState();
    }
    
    performSearch() {
        this.getInstance().performScreenshotSearch();
        this.searching = true;
    }
    
    changeSelectedScreenshotIdx(selectedScreenshotIdx) {
        this.selectedScreenshotIdx = selectedScreenshotIdx;
    }

    screenshotsSearchResultsReceivedSuccess(screenshotsData) {
        this.screenshotsData = screenshotsData;
        if(this.selectedScreenshotIdx >= screenshotsData.length) {
            this.selectedScreenshotIdx = 0;
        }
        this.searching = false;
    }
    
    changeStatusSuccess(res) {
        if (this.screenshotsData[res.idx]) {
            this.screenshotsData[res.idx].status = res.status; 
            this.screenshotsData[res.idx].comment = res.comment; 
        }
    }
    
    static getScreenshotByIdx(screenshotIdx) {
    
        let screenshot = null;
        let state = this.getState();
        
        if(!(screenshotIdx >= state.screenshotsData.length)) {
           screenshot = state.screenshotsData[screenshotIdx];
        }
        
        return screenshot;
    }
}

export default alt.createStore(ScreenshotsPageStore, 'ScreenshotsPageStore');
