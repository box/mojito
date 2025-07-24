import alt from "../../alt";

import _ from "lodash";

import SearchConstants from "../../utils/SearchConstants";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import RepositoryActions from "../../actions/RepositoryActions";

import RepositoryStore from "../RepositoryStore";
import ShareSearchParamsModalActions from "../../actions/workbench/ShareSearchParamsModalActions";

class SearchParamsStore {

    constructor() {
        this.changedParam = "";
        this.setDefaultParameters();
        this.bindActions(WorkbenchActions);
        this.bindActions(RepositoryActions);
        this.bindActions(ShareSearchParamsModalActions);
    }

    /**
     * Convert location query to search parameters
     *
     * @param {string} query The query string from location.query
     * @return {{repoIds: number[], bcp47Tags: string[], searchAttribute: string, searchText: string,
     * searchType: string, used: boolean, unUsed: boolean, translate: boolean,
     * doNotTranslate: boolean, status: SearchParamsStore.STATUS,
     * tmTextUnitCreatedBefore: SearchParamsStore.CREATED_BEFORE
     * tmTextUnitCreatedAfter: SearchParamsStore.CREATED_AFTER
     * pageSize: Number, currentPageNumber: Number, pageOffset: Number }}
     */
    static convertQueryToSearchParams(query) {

        let {
            searchAttribute, searchText, searchType,
            status, used, unUsed, translate, doNotTranslate, tmTextUnitCreatedBefore, tmTextUnitCreatedAfter,
            pageSize, currentPageNumber, pageOffset,
            pluralFormOther, branchId
        } = query;

        let repoIds = query["repoIds[]"];
        let tmTextUnitIds = query["tmTextUnitIds[]"];
        let repoNames = query["repoNames[]"];
        let bcp47Tags = query["bcp47Tags[]"];

        if (typeof repoIds !== "undefined") {
            if (Array.isArray(repoIds)) {
                repoIds = repoIds.map((value) => parseInt(value));
            } else {
                repoIds = [parseInt(repoIds)];
            }
        }

        if (typeof repoNames !== "undefined" && !Array.isArray(repoNames)) {
            repoNames = [repoNames];
        }

        if (typeof bcp47Tags !== "undefined" && !Array.isArray(bcp47Tags)) {
            bcp47Tags = [bcp47Tags];
        }

        if (typeof tmTextUnitIds !== "undefined") {
            if (Array.isArray(tmTextUnitIds)) {
                tmTextUnitIds = tmTextUnitIds.map((value) => parseInt(value));
            } else {
                tmTextUnitIds = [parseInt(tmTextUnitIds)];
            }
        }

        let converted = {
            "changedParam": SearchConstants.UPDATE_ALL_LOCATION_UPDATE,
            "repoIds": typeof repoIds !== "undefined" ? repoIds : null,
            "repoNames": typeof repoNames !== "undefined" ? repoNames : null,
            "bcp47Tags": typeof bcp47Tags !== "undefined" ? bcp47Tags : null,
            "searchAttribute": typeof searchAttribute !== "undefined" ? searchAttribute : null,
            "searchText": typeof searchText !== "undefined" ? searchText : null,
            "searchType": typeof searchType !== "undefined" ? searchType : null,
            "status": typeof status !== "undefined" ? status : null,
            "used": typeof used !== "undefined" ? (used === "true") : null,
            "unUsed": typeof unUsed !== "undefined" ? (unUsed === "true") : null,
            "translate": typeof translate !== "undefined" ? (translate === "true") : null,
            "doNotTranslate": typeof doNotTranslate !== "undefined" ? (doNotTranslate === "true") : null,
            "tmTextUnitCreatedBefore": typeof tmTextUnitCreatedBefore !== "undefined" ? tmTextUnitCreatedBefore : null,
            "tmTextUnitCreatedAfter": typeof tmTextUnitCreatedAfter !== "undefined" ? tmTextUnitCreatedAfter : null,
            "pageSize": typeof pageSize !== "undefined" ? parseInt(pageSize) : null,
            "currentPageNumber": typeof currentPageNumber !== "undefined" ? parseInt(currentPageNumber) : null,
            "pageOffset": typeof pageOffset !== "undefined" ? parseInt(pageOffset) : null,
            "pluralFormOther": typeof pluralFormOther !== "undefined" ? pluralFormOther : null,
            "tmTextUnitIds": typeof tmTextUnitIds !== "undefined" ? tmTextUnitIds : null,
            "branchId": typeof branchId !== "undefined" ? branchId : null
        };

        return converted;
    }

    /**
     * Filter the bcp47Tags to contain only tags that are part of the selected
     * repositories.
     */
    filterBcp47TagsForSelectedRepositories() {
        let bcp47TagsOfSelectedRepository = RepositoryStore.getAllBcp47TagsForRepositoryIds(this.repoIds);
        this.bcp47Tags = _.intersection(this.bcp47Tags, bcp47TagsOfSelectedRepository);
    }

    /**
     * @param {object} paramData
     * @param {string} paramData.changedParam
     * @param {object} paramData.repository
     *
     * @param {string[]} paramData.bcp47Tags BCP47 tags
     *
     * @param {object} paramData.repoIds
     * @param {object} paramData.bcp47Tags Array of BCP47 Tag to be selected
     *
     * @param {string} paramData.searchText
     * @param {string} paramData.searchAttribute
     *
     * @param {object} paramData.searchFilterParam
     * @param {object} paramData.searchFilterParamValue
     */
    onSearchParamsChanged(paramData) {

        switch (paramData.changedParam) {
            case SearchConstants.REPOSITORIES_CHANGED:

                this.setFirstPageAsCurrent();
                this.repoIds = paramData.repoIds;
                this.filterBcp47TagsForSelectedRepositories();
                break;

            case SearchConstants.LOCALES_CHANGED:

                this.setFirstPageAsCurrent();
                this.bcp47Tags = paramData.bcp47Tags;
                break;

            case SearchConstants.SEARCHTEXT_CHANGED:

                this.setFirstPageAsCurrent();
                this.searchText = paramData.data.searchText;
                this.searchAttribute = paramData.data.searchAttribute;
                this.searchType = paramData.data.searchType;
                break;

            case SearchConstants.SEARCHFILTER_CHANGED:

                this.setFirstPageAsCurrent();
                this[paramData.searchFilterParam] = paramData.searchFilterParamValue;
                break;

            case SearchConstants.UPDATE_ALL:
            case SearchConstants.UPDATE_ALL_LOCATION_UPDATE:
            case SearchConstants.UPDATE_ALL_LOCATION_NONE:

                this.setDefaultParameters();
                this.updateAllParameters(paramData);
                break;

            case SearchConstants.NEXT_PAGE_REQUESTED:

                this.incrementPageNumber();
                break;

            case SearchConstants.PREVIOUS_PAGE_REQUESTED:

                this.decrementPageNumber();
                break;

            case SearchConstants.PAGE_SIZE_CHANGED:

                this.changePageSize(paramData.pageSize);
                break;

            default:
                console.error("SearchParamsStore::Unknown param type");
                break;
        }

        this.updatePageOffset();

        this.changedParam = paramData.changedParam;
    }

    onGetSearchParamsSuccess(result) {
        result.searchParams["changedParam"] = SearchConstants.UPDATE_ALL_LOCATION_UPDATE;
        this.onSearchParamsChanged(result.searchParams);
    }

    /**
     * Add newValue to a state that is array based (such as bcp47Tags or repoIds).
     *
     * @param {array} arrayState A state in this store that is array based.
     * @param {boolean} isAdding True to control add. False to remove.
     * @param {object} newValue The new value to add.
     */
    updateArrayState(arrayState, isAdding, newValue) {
        if (isAdding) {
            this.pushIfNotExist(arrayState, newValue);
        } else {
            let foundIndex = arrayState.indexOf(newValue);
            if (foundIndex >= 0) {
                arrayState.splice(foundIndex, 1);
            }
        }
    }

    /**
     * Push new value into array if it doesn't exist.  Otherwise, it doesn't nothing.
     * @param {array} array
     * @param {object} newValue
     */
    pushIfNotExist(array, newValue) {
        if (array.indexOf(newValue) < 0) {
            array.push(newValue);
        }
    }

    setDefaultParameters() {

        this.repoIds = [];
        this.branchId = null;
        this.tmTextUnitIds = [];
        this.repoNames = [];
        this.bcp47Tags = [];

        this.searchText = "";
        this.searchAttribute = SearchParamsStore.SEARCH_ATTRIBUTES.TARGET;
        this.searchType = SearchParamsStore.SEARCH_TYPES.CONTAINS;

        this.pluralFormOther = null;

        // 'Filter by' related
        this.status = SearchParamsStore.STATUS.ALL;
        this.used = true;
        this.unUsed = false;

        this.translate = true;
        this.doNotTranslate = true;

        this.tmTextUnitCreatedBefore = null;
        this.tmTextUnitCreatedAfter = null;

        // pagination related attributes
        this.pageSize = 10;
        this.currentPageNumber = 1;
        this.pageOffset = 0;
    }

    /**
     * Merges all values with the current state of the store.
     *
     * If a null value is passed in, then it keeps the current state of the store.
     *
     * @param {string[]} repoIds
     * @param {string[]} bcp47Tags
     * @param {string} searchText
     * @param {string} searchAttribute
     * @param {string} searchType
     * @param {SearchParamsStore.STATUS} status
     * @param {bool} used
     * @param {bool} unUsed
     * @param {int} pageSize
     * @param {int} currentPageNumber
     * @param {int} pageOffset
     */
    updateAllParameters( {
                             repoIds = null, branchId = null, tmTextUnitIds = null, repoNames = null, bcp47Tags = null, searchText = null,
            searchAttribute = null, searchType = null, status = null, used = null,
            unUsed = null, pageSize = null, currentPageNumber = null,
            pageOffset = null, pluralFormOther = null, translate = null,
            doNotTranslate = null, tmTextUnitCreatedBefore = null, tmTextUnitCreatedAfter = null
    } = {}) {


        //TODO merge this with SEARCHTEXT_CHANGED

        if (repoIds !== null) {
            this.repoIds = repoIds.slice();
        }

        if (branchId != null) {
            this.branchId = branchId;
        }

        if (tmTextUnitIds != null) {
            this.tmTextUnitIds = tmTextUnitIds;
        }

        if (repoNames !== null) {
            this.repoNames = repoNames.slice();
        }

        if (bcp47Tags !== null) {
            this.bcp47Tags = bcp47Tags;
        }

        if (pluralFormOther !== null) {
            this.pluralFormOther = pluralFormOther;
        }

        if (searchText !== null)
            this.searchText = searchText;

        if (searchAttribute !== null)
            this.searchAttribute = searchAttribute;

        if (searchType !== null)
            this.searchType = searchType;

        if (status !== null)
            this.status = status;

        if (used !== null)
            this.used = used;

        if (unUsed !== null)
            this.unUsed = unUsed;

        if (translate !== null)
            this.translate = translate;

        if (doNotTranslate !== null)
            this.doNotTranslate = doNotTranslate;

        if (tmTextUnitCreatedBefore !== null)
            this.tmTextUnitCreatedBefore = tmTextUnitCreatedBefore;

        if (tmTextUnitCreatedAfter !== null)
            this.tmTextUnitCreatedAfter = tmTextUnitCreatedAfter;

        if (pageSize !== null)
            this.pageSize = pageSize;

        if (currentPageNumber !== null)
            this.currentPageNumber = currentPageNumber;

        if (pageOffset !== null)
            this.pageOffset = pageOffset;
    }

    getAllRepositoriesSuccess(repositories) {
        if (this.repoNames && this.repoNames.length > 0) {
            this.waitFor(RepositoryStore);
            this.repoIds = [];
            this.repoNames.forEach(repoName => {
                var repositoryByName = RepositoryStore.getRepositoryByName(repoName);
                if (repositoryByName) {
                    this.repoIds.push(repositoryByName.id);
                } else {
                    console.log("Can't find repository for name: ", repoName);
                }
            });
        }
    }

    updatePageOffset() {
        this.pageOffset = (this.currentPageNumber - 1) * this.pageSize;
    }

    setFirstPageAsCurrent() {
        this.setCurrentPageNumber(1);
    }

    decrementPageNumber() {
        if (this.currentPageNumber > 1) {
            this.setCurrentPageNumber(this.currentPageNumber - 1);
        }
    }

    incrementPageNumber() {
        this.setCurrentPageNumber(this.currentPageNumber + 1);
    }

    changePageSize(pageSize) {
        this.pageSize = pageSize;
        this.setCurrentPageNumber(1);
    }

    setCurrentPageNumber(pageNumber) {
        if (pageNumber >= 1) {
            this.currentPageNumber = pageNumber;
        }
    }

    /**
     * Checks the minimum set state requirement to perform searching.  True when it is ready for searching
     * @return {SearchParamsStore}
     */
    static isReadyForSearching(searchParamStoreState) {
        return (searchParamStoreState.repoIds.length > 0 && searchParamStoreState.bcp47Tags.length > 0);
    }
}

SearchParamsStore.SEARCH_TYPES = {
    "EXACT": "exact",
    "CONTAINS": "contains",
    "ILIKE": "ilike"
};

SearchParamsStore.SEARCH_ATTRIBUTES = {
    "STRING_ID": "stringId",
    "SOURCE": "source",
    "TARGET": "target",
    "ASSET": "asset",
    "PLURAL_FORM_OTHER": "pluralFormOther",
    "TM_TEXT_UNIT_ID": "tmTextUnitId"
};

SearchParamsStore.STATUS = {
    /**
     * All TextUnits
     */
    "ALL": "ALL",
    /**
     * All TextUnits that have a translation with any status
     */
    "TRANSLATED": "TRANSLATED",
    /**
     * All TextUnits that have no translation
     */
    "UNTRANSLATED": "UNTRANSLATED",
    /**
     * All TextUnits that have a translation with any status
     */
    "TRANSLATED_AND_NOT_REJECTED": "TRANSLATED_AND_NOT_REJECTED",
    /**
     * TextUnits for translation (ie. without translation or with status TRANSLATION_NEEDED
     * or that are rejected).
     */
    "FOR_TRANSLATION": "FOR_TRANSLATION",
    /**
     * TextUnits with status REVIEW_NEEDED.
     */
    "REVIEW_NEEDED": "REVIEW_NEEDED",
    /**
     * TextUnits that don't have status REVIEW_NEEDED.
     */
    "REVIEW_NOT_NEEDED": "REVIEW_NOT_NEEDED",
    /**
     * TextUnits that are rejected, ie includedInLocalizedFile is false.
     */
    "REJECTED": "REJECTED",
    /**
     * TextUnits that are not rejected, ie includedInLocalizedFile is true.
     */
    "NOT_REJECTED": "NOT_REJECTED",
    /**
     * TextUnits that are approved and not rejected (aka Accepted in UI)
     */
    "APPROVED_AND_NOT_REJECTED": "APPROVED_AND_NOT_REJECTED",

};

export default alt.createStore(SearchParamsStore, 'SearchParamsStore');
