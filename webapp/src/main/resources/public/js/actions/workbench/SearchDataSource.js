import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import TextUnitClient from "../../sdk/TextUnitClient";
import WorkbenchActions from "./WorkbenchActions";
import {buildTextUnitSearcherParameters} from "../../utils/TextUnitSearcherParametersBuilder";

const SearchDataSource = {
    performSearch: {
        remote(searchResultsStoreState, searchParams) {
            let returnEmpty = false;

            let {textUnitSearcherParameters, returnEmpty: helperReturnEmpty} = buildTextUnitSearcherParameters(searchParams);
            returnEmpty = returnEmpty || helperReturnEmpty;

            // ask for one extra text unit to know if there are more text units
            let limit = searchParams.pageSize + 1;

            textUnitSearcherParameters.offset(searchParams.pageOffset).limit(limit);

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
