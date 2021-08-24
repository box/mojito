import alt from "../../alt";
import ScreenshotsRepositoryActions from "../../actions/screenshots/ScreenshotsRepositoryActions";
import ScreenshotsLocaleActions from "../../actions/screenshots/ScreenshotsLocaleActions";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions";
import ScreenshotsHistoryActions from "../../actions/screenshots/ScreenshotsHistoryActions";
import ScreenshotsSearchTextActions from "../../actions/screenshots/ScreenshotsSearchTextActions";
import ScreenshotsPaginatorActions from "../../actions/screenshots/ScreenshotsPaginatorActions";
import ScreenshotsPaginatorStore from "../../stores/screenshots/ScreenshotsPaginatorStore";
import ScreenshotsSearchTextStore from "../../stores/screenshots/ScreenshotsSearchTextStore";
import SearchParamsStore from "../workbench/SearchParamsStore";
import {StatusCommonTypes} from "../../components/screenshots/StatusCommon";


class ScreenshotsHistoryStore {

    constructor() {
        this.setDefaultState();
        
        this.bindActions(ScreenshotsRepositoryActions);
        this.bindActions(ScreenshotsHistoryActions);
        this.bindActions(ScreenshotsLocaleActions);
        this.bindActions(ScreenshotsPageActions);
        this.bindActions(ScreenshotsPaginatorActions);
        this.bindActions(ScreenshotsSearchTextActions);
    }
    
    setDefaultState() {
        // use to skip location update (Initiated by the ScreenshotsPage) when
        // this store is initializing from the browser location or to do 
        // group updates together
        this.skipLocationHistoryUpdate = false;
        
        this.selectedRepositoryIds = [];
        this.bcp47Tags = [];
        this.searchAttribute = SearchParamsStore.SEARCH_ATTRIBUTES.TARGET;
        this.searchText = "";
        this.searchType = SearchParamsStore.SEARCH_TYPES.CONTAINS;
        this.status = StatusCommonTypes.ALL;
        this.screenshotRunType = ScreenshotsSearchTextStore.SEARCH_SCREENSHOTRUN_TYPES.MANUAL_RUN;
        this.currentPageNumber = 1;
        this.selectedScreenshotIdx = null;
    }

    enableHistoryUpdate() {
        this.skipLocationHistoryUpdate = false;
    }

    disableHistoryUpdate() {
        this.skipLocationHistoryUpdate = true;
    }

    changeSelectedRepositoryIds(selectedRepositoryIds) {
        this.selectedRepositoryIds = selectedRepositoryIds;
    }

    changeSelectedBcp47Tags(selectedBcp47Tags) {
        this.selectedBcp47Tags = selectedBcp47Tags;
    }

    changeSelectedScreenshotIdx(selectedScreenshotIdx) {
        this.selectedScreenshotIdx = selectedScreenshotIdx;
    }

    changeSearchAttribute(searchAttribute) {
        this.searchAttribute = searchAttribute;
    }

    changeSearchType(searchType) {
        this.searchType = searchType;
    }

    changeSearchText(searchText) {
        this.searchText = searchText;
    }

    changeStatus(status) {
        this.status = status;
    }

    changeScreenshotRunType(screenshotRunType) {
        this.screenshotRunType =  screenshotRunType;
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
        let {searchAttribute, searchText, searchType, status, screenshotRunType,
            currentPageNumber, selectedScreenshotIdx} = query;

        let selectedRepositoryIds = query["selectedRepositoryIds[]"];
        let bcp47Tags = query["selectedBcp47Tags[]"];

        ScreenshotsHistoryActions.disableHistoryUpdate();

        if (selectedRepositoryIds) {
            if (!Array.isArray(selectedRepositoryIds)) {
                selectedRepositoryIds = [parseInt(selectedRepositoryIds)];
            } else {
                selectedRepositoryIds = selectedRepositoryIds.map((v) => parseInt(v));
            }
        } else {
            selectedRepositoryIds = [];
        }
        ScreenshotsRepositoryActions.changeSelectedRepositoryIds(selectedRepositoryIds);


        if (bcp47Tags) {
            if (!Array.isArray(bcp47Tags)) {
                bcp47Tags = [bcp47Tags];
            }
        } else {
            bcp47Tags = [];
        }
        ScreenshotsLocaleActions.changeSelectedBcp47Tags(bcp47Tags);

        if (!searchAttribute) {
            searchAttribute = SearchParamsStore.SEARCH_ATTRIBUTES.TARGET;
        }
        ScreenshotsSearchTextActions.changeSearchAttribute(searchAttribute);

        if (!searchText) {
            searchText = "";
        }
        ScreenshotsSearchTextActions.changeSearchText(searchText);

        if (!searchType) {
            searchType = SearchParamsStore.SEARCH_TYPES.CONTAINS;
        }
        ScreenshotsSearchTextActions.changeSearchType(searchType);
        
        if (!status) {
            status = StatusCommonTypes.ALL;
        }
        ScreenshotsSearchTextActions.changeStatus(status);

        if (!screenshotRunType) {
            screenshotRunType = ScreenshotsSearchTextStore.SEARCH_SCREENSHOTRUN_TYPES.MANUAL_RUN;
        }
        ScreenshotsSearchTextActions.changeScreenshotRunType(screenshotRunType);

        if (!currentPageNumber) {
            currentPageNumber = 1;
        }
        ScreenshotsPaginatorActions.changeCurrentPageNumber(parseInt(currentPageNumber));

        if (selectedScreenshotIdx === "") {
            selectedScreenshotIdx = null;
        } else {
            selectedScreenshotIdx = parseInt(selectedScreenshotIdx);

            if (!(selectedScreenshotIdx >= 0)) {
                selectedScreenshotIdx = 0;
            }
        }
        ScreenshotsPageActions.changeSelectedScreenshotIdx(selectedScreenshotIdx);

        ScreenshotsHistoryActions.enableHistoryUpdate();
    }
}

export default alt.createStore(ScreenshotsHistoryStore, 'ScreenshotsHistoryStore');
