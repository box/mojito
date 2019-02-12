import ScreenshotsPageActions from "./ScreenshotsPageActions";

import ScreenshotsRepositoryStore from "../../stores/screenshots/ScreenshotsRepositoryStore";
import ScreenshotsLocaleStore from "../../stores/screenshots/ScreenshotsLocaleStore";
import ScreenshotsSearchTextStore from "../../stores/screenshots/ScreenshotsSearchTextStore";
import ScreenshotsPaginatorStore from "../../stores/screenshots/ScreenshotsPaginatorStore";
import ScreenshotClient from "../../sdk/ScreenshotClient";
import {StatusCommonTypes} from "../../components/screenshots/StatusCommon";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";

const ScreenshotsDataSource = {
    performScreenshotSearch: {
        remote(state) {
            let screenshotsRepositoryStoreState = ScreenshotsRepositoryStore.getState();
            let screenshotsLocaleStoreState = ScreenshotsLocaleStore.getState();
            let screenshotsSearchTextStoreState = ScreenshotsSearchTextStore.getState();
            let screenshotsPaginatorStoreState = ScreenshotsPaginatorStore.getState();

            let promise;

            if (screenshotsRepositoryStoreState.selectedRepositoryIds.length === 0
                    || screenshotsLocaleStoreState.selectedBcp47Tags.length === 0) {

                promise = new Promise((resolve) => {
                    setTimeout(function () {
                        resolve({'content': [], 'hasNext': false, 'size' : 0});
                    }, 0);
                });
            } else {
                let params = {
                    repositoryIds: screenshotsRepositoryStoreState.selectedRepositoryIds,
                    bcp47Tags: screenshotsLocaleStoreState.selectedBcp47Tags,
                    status: screenshotsSearchTextStoreState.status === StatusCommonTypes.ALL ? null : screenshotsSearchTextStoreState.status,
                    screenshotRunType: screenshotsSearchTextStoreState.screenshotRunType,
                    limit: screenshotsPaginatorStoreState.limit + 1,
                    offset: screenshotsPaginatorStoreState.limit * (screenshotsPaginatorStoreState.currentPageNumber - 1),
                };

                if (screenshotsSearchTextStoreState.searchText) {

                    if (screenshotsSearchTextStoreState.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.SOURCE) {
                        params.source = screenshotsSearchTextStoreState.searchText;
                    } else if (screenshotsSearchTextStoreState.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.TARGET) {
                        params.target = screenshotsSearchTextStoreState.searchText;
                    } else if (screenshotsSearchTextStoreState.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.STRING_ID) {
                        params.name = screenshotsSearchTextStoreState.searchText;
                    } else {
                        params.screenshotName = screenshotsSearchTextStoreState.searchText;
                    }

                    params.searchType = screenshotsSearchTextStoreState.searchType.toUpperCase();
                }

                promise = ScreenshotClient.getScreenshots(params).then(function (screenshots) {

                    let hasNext = false;

                    if (screenshots.length === screenshotsPaginatorStoreState.limit + 1) {
                        hasNext = true;
                        screenshots = screenshots.slice(0, screenshotsPaginatorStoreState.limit);
                    }

                    return {'content': screenshots, 'hasNext': hasNext, 'size': screenshots.length};
                });
            }

            return promise;
        },
        success: ScreenshotsPageActions.screenshotsSearchResultsReceivedSuccess,
        error: ScreenshotsPageActions.screenshotsSearchResultsReceivedError
    },
};

export default ScreenshotsDataSource;
