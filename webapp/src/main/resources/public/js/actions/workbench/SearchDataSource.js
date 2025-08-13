import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
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

            if (!SearchParamsStore.isReadyForSearching(searchParams)) {
                returnEmpty = true;
            }

            if (searchParams.searchText) {

                if (searchParams.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.SOURCE) {
                    textUnitSearcherParameters.source(searchParams.searchText);
                } else if (searchParams.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.TARGET) {
                    textUnitSearcherParameters.target(searchParams.searchText);
                } else if (searchParams.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.ASSET) {
                    textUnitSearcherParameters.assetPath(searchParams.searchText);
                } else if (searchParams.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.PLURAL_FORM_OTHER) {
                    textUnitSearcherParameters.pluralFormOther(searchParams.searchText);
                } else if (searchParams.searchAttribute === SearchParamsStore.SEARCH_ATTRIBUTES.TM_TEXT_UNIT_IDS) {
                    let ids = searchParams.searchText
                        .split(',')
                        .map(v => parseInt(v.trim(), 10))
                        .filter(v => !isNaN(v));

                    textUnitSearcherParameters.tmTextUnitIds(ids.length ? ids : [0]);
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

            if (!searchParams.translate && !searchParams.doNotTranslate) {
                returnEmpty = true;
            } else if (searchParams.translate && !searchParams.doNotTranslate) {
                textUnitSearcherParameters.doNotTranslateFilter(false);
            } else if (!searchParams.translate && searchParams.doNotTranslate) {
                textUnitSearcherParameters.doNotTranslateFilter(true);
            }

            if (searchParams.tmTextUnitCreatedBefore) {
                textUnitSearcherParameters.tmTextUnitCreatedBefore(searchParams.tmTextUnitCreatedBefore);
            }

            if (searchParams.tmTextUnitCreatedAfter) {
                textUnitSearcherParameters.tmTextUnitCreatedAfter(searchParams.tmTextUnitCreatedAfter);
            }

            if (searchParams.tmTextUnitIds != null && searchParams.tmTextUnitIds.length > 0) {
                textUnitSearcherParameters.tmTextUnitIds(searchParams.tmTextUnitIds);
            }

            if (searchParams.branchId != null) {
                textUnitSearcherParameters.branchId(searchParams.branchId);
            }

            // ask for one extra text unit to know if there are more text units
            let limit = searchParams.pageSize + 1;

            textUnitSearcherParameters.repositoryIds(repositoryIds).localeTags(bcp47Tags).offset(searchParams.pageOffset).limit(limit);

            let promise;

            if (returnEmpty) {
                promise = new Promise(function (resolve, reject) {
                    resolve({textUnits: [], hasMore: false});
                });
            } else {
                promise = TextUnitClient.getTextUnits(textUnitSearcherParameters).then(function (textUnits) {

                    let hasMore = false;

                    if (textUnits.length === limit) {
                        hasMore = true;
                        textUnits = textUnits.slice(0, limit - 1);
                    }

                    return {textUnits, hasMore};
                });
            }

            return promise;
        },

        success: WorkbenchActions.searchResultsReceivedSuccess,
        error: WorkbenchActions.searchResultsReceivedError
    }
};

export default SearchDataSource;
