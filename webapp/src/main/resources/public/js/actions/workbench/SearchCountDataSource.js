import TextUnitClient from "../../sdk/TextUnitClient";
import WorkbenchActions from "./WorkbenchActions";
import { buildTextUnitSearchParameters } from "./TextUnitSearchParameterUtils";

const SearchCountDataSource = {
    getCount: {
        remote(searchResultsStoreState, searchParams) {
            const { returnEmpty, textUnitSearcherParameters } = buildTextUnitSearchParameters(searchParams);

            if (returnEmpty) {
                return Promise.resolve({ totalCount: null });
            }

            return TextUnitClient.getTextUnitCount(textUnitSearcherParameters).then((countResponse) => {
                return { totalCount: countResponse.textUnitCount };
            });
        },

        success: WorkbenchActions.searchCountReceivedSuccess,
        error: WorkbenchActions.searchCountReceivedError
    }
};

export default SearchCountDataSource;

