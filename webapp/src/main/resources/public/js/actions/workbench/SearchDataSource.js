import Error from "../../utils/Error";
import SearchParamStore from "../../stores/workbench/SearchParamsStore.js";
import TextUnitClient from "../../sdk/TextUnitClient";
import TextUnitSearcherParameters from "../../sdk/TextUnitSearcherParameters";
import WorkbenchActions from "./WorkbenchActions";

const SearchDataSource = {
    performSearch: {
        remote(searchResultsStoreState, searchParams) {
            let returnEmpty = false;

            let repositoryIds = searchParams.repoIds;

            let bcp47Tags = searchParams.bcp47Tags;


            let textUnitSearcherParameters = new TextUnitSearcherParameters();

            if (!SearchParamStore.isReadyForSearching(searchParams)) {
                returnEmpty = true;
            }

            if (searchParams.searchText) {

                if (searchParams.searchAttribute === SearchParamStore.SEARCH_ATTRIBUTES.SOURCE) {
                    textUnitSearcherParameters.source(searchParams.searchText);
                } else if (searchParams.searchAttribute === SearchParamStore.SEARCH_ATTRIBUTES.TARGET) {
                    textUnitSearcherParameters.target(searchParams.searchText);
                } else if (searchParams.searchAttribute === SearchParamStore.SEARCH_ATTRIBUTES.ASSET) {
                    textUnitSearcherParameters.assetPath(searchParams.searchText);
                } else if (searchParams.searchAttribute === SearchParamStore.SEARCH_ATTRIBUTES.PLURAL_FORM_OTHER) {
                    textUnitSearcherParameters.pluralFormOther(searchParams.searchText);
                } else {
                    textUnitSearcherParameters.name(searchParams.searchText);
                }

                textUnitSearcherParameters.searchType(searchParams.searchType.toUpperCase());
            }
            
            if (searchParams.status) {
                textUnitSearcherParameters.statusFilter(searchParams.status);
            }

            if (!searchParams.used && !searchParams.unUsed) {
                returnEmpty = true;
            } else if (searchParams.used && !searchParams.unUsed) {
                textUnitSearcherParameters.usedFilter(textUnitSearcherParameters.USED);
            } else if (!searchParams.used && searchParams.unUsed) {
                textUnitSearcherParameters.usedFilter(textUnitSearcherParameters.UNUSED);
            }

            textUnitSearcherParameters.repositoryIds(repositoryIds).localeTags(bcp47Tags)
                .offset(searchParams.pageOffset).limit(searchParams.pageSize);

            let promise;

            if (returnEmpty) {
                promise = new Promise(function (resolve, reject) {
                    resolve([]);
                });
            } else {
                promise = TextUnitClient.getTextUnits(textUnitSearcherParameters).then(function (results) {

                    return results;
                });
            }

            return promise;
        },

        success: WorkbenchActions.searchResultsReceivedSuccess,
        error: WorkbenchActions.searchResultsReceivedError
    }
};

export default SearchDataSource;
