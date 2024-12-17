import alt from "../../alt";
import Error from "../../utils/Error";
import SearchConstants from "../../utils/SearchConstants";
import SearchDataSource from "../../actions/workbench/SearchDataSource";
import SearchParamsStore from "./SearchParamsStore";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import RepositoryActions from "../../actions/RepositoryActions";
import textUnitStore from "./TextUnitStore";

class SearchResultsStore {

    constructor() {
        /** @type {TextUnit[]} */
        this.searchResults = [];

        this.noMoreResults = false;

        this.isErrorOccurred = false;
        this.errorObject = null;
        this.errorResponse = null;

        /**
         * this map contains ids of all selected textunits
         */
        this.selectedTextUnitsMap = {};

        /** @type {Boolean} True when a search is being formed.  This store is responsible for setting this state to true before searching and setting to false when searching is done */
        this.isSearching = false;

        /** @type {Boolean} True when search didn't result any result, It's different than searchResults.length equals to 0 b'c it can be 0 if search has not been requested. */
        this.searchHadNoResults = false;

        this.bindActions(WorkbenchActions);
        this.bindActions(RepositoryActions);

        this.registerAsync(SearchDataSource);
    }

    /**
     * The action handler that is called when any search parameter on the workbench changes.
     * This function waits for the SearchParamsStore to finish its action handler before
     * firing the request to fetch results for the search criteria provided in the UI.
     */
    onSearchParamsChanged() {
        this.waitFor(SearchParamsStore);

        const searchParamsStoreState = SearchParamsStore.getState();

        if (SearchParamsStore.isReadyForSearching(searchParamsStoreState)) {
            const newState = {
                "noMoreResults": false,
                "isSearching": true,
                "searchHadNoResults": false
            };

            if (searchParamsStoreState.changedParam !== SearchConstants.NEXT_PAGE_REQUESTED &&
                searchParamsStoreState.changedParam !== SearchConstants.PREVIOUS_PAGE_REQUESTED) {
                newState.selectedTextUnitsMap = {};
            }

            this.setState(newState);

            this.getInstance().performSearch(searchParamsStoreState);
        } else {
            this.setState({
                "searchResults": [],
                "isSearching": false,
                "selectedTextUnitsMap": {}
            });
        }
    }

    /**
     * @param {object} -- The response sent by the promise when the search query is successful.
     */
    onSearchResultsReceivedSuccess(response) {
        this.waitFor(SearchParamsStore);
        this.noMoreResults = !response.hasMore;
        this.searchResults = response.textUnits;
        this.isSearching = false;
        this.searchHadNoResults = (response.textUnits.length === 0);
    }

    /**
     * @param {object} -- The error object sent by the promise when the search query fails.
     */
    onSearchResultsReceivedError(errorResponse) {
        console.error("SearchResultsStore::onSearchResultsReceivedError ", errorResponse);
        this.searchResults = [];
        this.noMoreResults = true;
        this.isSearching = false;
        const errorObject = this.createErrorObject(Error.IDS.SEARCH_QUERY_FAILED);
        this.setErrorState(errorObject, errorResponse);
    }

    /**
     * Marks the textunit passed in as selected in the selectedTextUnitsMap if the textunit
     * was selected in the UI. If not, the entry is deleted from the map.
     * @param textUnit - The textunit
     */
    onTextUnitSelection(textUnit) {

        for (const textUnitInStore of this.searchResults) {
            if (textUnitInStore.equals(textUnit)) {
                const textUnitInStoreId = textUnitInStore.getTextUnitKey();
                if (typeof this.selectedTextUnitsMap[textUnitInStoreId] === "undefined") {
                    this.selectedTextUnitsMap[textUnitInStoreId] = textUnitInStore;
                } else {
                    delete this.selectedTextUnitsMap[textUnitInStoreId];
                }
            }
        }
    }

    /**
     * The target string of the selected textunits will be deleted if the request is processed successfully.
     * @param {TextUnit[]} textUnits
     */
    onDeleteTextUnits(textUnits) {

        this.waitFor(textUnitStore.dispatchToken);

        textUnits.forEach(textUnit => {
            const deletedTextUnit = textUnit;
            const currentSearchParams = SearchParamsStore.getState();

            if (currentSearchParams.status === SearchParamsStore.STATUS.TRANSLATED ||
                currentSearchParams.status === SearchParamsStore.STATUS.TRANSLATED_AND_NOT_REJECTED) {

                // remove it from the list of result because now it doesn't have a translation anymore
                for (let index = 0; index < this.searchResults.length; index++) {
                    const textUnitInStore = this.searchResults[index];
                    if (textUnitInStore.getTmTextUnitVariantId() === deletedTextUnit.getTmTextUnitVariantId()) {
                        this.searchResults.splice(index, 1);
                        break;
                    }
                }
            }
        });
    }

    /**
     * @param {TextUnit} textUnit response The response sent by the promise when the delete request succeeds
     */
    onDeleteTextUnitsSuccess() {
        console.log("SearchResultsStore::onDeleteTextUnitsSuccess");
    }

    /**
     * @param {TextUnitError} errorResponse
     */
    onDeleteTextUnitsError() {
        console.log("SearchResultsStore::onDeleteTextUnitsError");
    }

    /**
     *
     * @param {TextUnit} textUnit
     */
    onSaveVirtualAssetTextUnitSuccess(textUnit) {
        this.updateSearchResultsWithTextUnit(textUnit);
    }

    /**
     * When the error modal is closed, the state of the store must be updated accordingly
     * to set the isErrorOccurred to false.
     */
    onResetErrorState() {
        this.isErrorOccurred = false;
        this.errorObject = null;
    }

    /**
     * Action handler to clear the selected textunits map when WorkbenchActions.resetSelectedTextUnits is fired.
     */
    onResetAllSelectedTextUnits() {
        this.resetSelectedTextUnitsMap();
    }

    /**
     * Action handler to remove the textunits in the current page from the selectedTextUnits map.
     */
    onResetSelectedTextUnitsInCurrentPage() {
        for (const textUnit of this.searchResults) {
            delete this.selectedTextUnitsMap[textUnit.getTextUnitKey()];
        }
    }

    /**
     * Handle onSuccess event of onSaveTextUnit
     *
     * We replace the old text unit in store with the new version version passed
     * as parameter
     * @param {TextUnit} textUnit The textUnit passed back by the SDK
     */
    onSaveTextUnitSuccess(textUnit) {
        this.updateSearchResultsWithTextUnit(textUnit);
    }

    /**
     * Handle onSuccess event of onSaveTextUnit
     *
     * We replace the old text unit in store with the new version version passed
     * as parameter
     *
     * @param {TextUnit} textUnit
     */
    onCheckAndSaveTextUnitSuccess(textUnit) {
        this.updateSearchResultsWithTextUnit(textUnit);
    }

    /**
     * @param {TextUnit} textUnit
     */
    updateSearchResultsWithTextUnit(textUnit) {
        for (let index = 0; index < this.searchResults.length; index++) {
            const textUnitInStore = this.searchResults[index];
            if (textUnitInStore.getTextUnitKey() === textUnit.getTextUnitKey()) {
                this.searchResults[index] = textUnit;
                break;
            }
        }
    }

    /**
     * Action handler to add all textunits in the current page to the selectedTextUnitsMap.
     */
    onSelectAllTextUnitsInCurrentPage() {
        for (const textUnit of this.searchResults) {
            this.selectedTextUnitsMap[textUnit.getTextUnitKey()] = textUnit;
        }
    }

    getAllRepositoriesSuccess() {
        this.onSearchParamsChanged();
    }

    /**
     * Clear the selected textunits map. Usually done when search params change or a
     * bulk operation succeeds.
     */
    resetSelectedTextUnitsMap() {
        this.selectedTextUnitsMap = {};
    }

    /**
     * @return {TextUnit[]}
     */
    static getSelectedTextUnits() {
        const result = [];
        Object.keys(this.getState().selectedTextUnitsMap).forEach((key) => {
            result.push(this.getState().selectedTextUnitsMap[key]);
        });

        return result;
    }

    /**
     * @param {string} errorId An error ID from the list of Error.IDS defined in class Error.
     * @returns {Error} The error object for the errorId passed in.
     */
    createErrorObject(errorId) {
        const error = new Error();
        error.setErrorId(errorId);
        return error;
    }

    /**
     * Sets the error state of this store so that components can react to it.
     * @param {Error} errorObject The Error object to set on the state of this store.
     * @param {object} errorResponse The error object sent be the promise.
     */
    setErrorState(errorObject, errorResponse) {
        this.errorObject = errorObject;
        this.isErrorOccurred = true;
        this.errorResponse = errorResponse;
    }
}

export default alt.createStore(SearchResultsStore, 'SearchResultsStore');
