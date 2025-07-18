import TextUnitClient from "../../sdk/TextUnitClient.js";
import WorkbenchActions from "./WorkbenchActions.js";
import { buildTextUnitSearchParameters } from "./TextUnitSearchParameterUtils.js";

const SearchDataSource = {
    performSearch: {
        remote(searchResultsStoreState, searchParams) {
            const { returnEmpty, textUnitSearcherParameters, limit } = buildTextUnitSearchParameters(searchParams);

            if (returnEmpty) {
                return Promise.resolve({ textUnits: [], hasMore: false });
            }

            return TextUnitClient.getTextUnits(textUnitSearcherParameters).then((textUnits) => {
                let hasMore = false;

                if (textUnits.length === limit) {
                    hasMore = true;
                    textUnits = textUnits.slice(0, limit - 1);
                }

                return { textUnits, hasMore };
            });

        },

        success: WorkbenchActions.searchResultsReceivedSuccess,
        error: WorkbenchActions.searchResultsReceivedError
    }
};

export default SearchDataSource;
