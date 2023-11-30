import alt from "../../alt";
import ShareSearchParamsModalActions from "../../actions/workbench/ShareSearchParamsModalActions";
import ShareSearchParamsDataSource from "../../actions/workbench/ShareSearchParamsDataSource";
import UrlHelper from "../../utils/UrlHelper";
import SearchParamsStore from "./SearchParamsStore";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import keymirror from "keymirror";

class ShareSearchParamsModalStore {

    static ERROR_TYPES = keymirror({
        "GET_SEARCH_PARAMS": null,
        "SAVE_SEARCH_PARAMS": null,
        "COPY_TO_CLIPBOARD": null,
    });

    constructor() {
        this.setDefaultState();
        this.bindActions(ShareSearchParamsModalActions);
        this.bindListeners({
            onSearchParamsChanged: WorkbenchActions.SEARCH_PARAMS_CHANGED,
        });
        this.registerAsync(ShareSearchParamsDataSource);
    }

    setDefaultState() {
        this.show = false;
        this.isLoadingParams = false;
        this.errorType = null;
    }

    open() {
        this.show = true;
        if (!this.isLoadingParams && this.url == null) {
            this.waitFor(SearchParamsStore);
            this.isLoadingParams = true;
            this.getInstance().saveSearchParams(SearchParamsStore.getState());
        }
    }

    close() {
        this.show = false;

        if (this.errorType != null) {
            this.errorType = null;
            this.url = null;
        }
    }

    onGetSearchParams(uuid) {
        this.getInstance().getSearchParams(uuid);
    }

    onGetSearchParamsSuccess(result) {
        this.waitFor(SearchParamsStore);
        this.isLoadingParams = false;
        this.url = this.getUrlForUUID(result.uuid);
    }

    onGetSearchParamsError() {
        this.isLoadingParams = false;
        this.errorType = ShareSearchParamsModalStore.ERROR_TYPES.GET_SEARCH_PARAMS;
    }

    onSaveSearchParamsSuccess(uuid) {
        this.isLoadingParams = false;
        this.url = this.getUrlForUUID(uuid);
    }

    onSaveSearchParamsError() {
        this.isLoadingParams = false;
        this.errorType = ShareSearchParamsModalStore.ERROR_TYPES.SAVE_SEARCH_PARAMS;
    }

    onSearchParamsChanged(searchParams) {
        this.isLoadingParams = false;
        this.url = null;
    }

    onSetErrorType(errorType) {
        this.errorType = errorType;
    }

    getUrlForUUID(uuid) {
        return new URL(UrlHelper.getUrlWithContextPath("/workbench?") + UrlHelper.toQueryString({"link": uuid}), location.origin).href
    }
}



export default alt.createStore(ShareSearchParamsModalStore, 'ShareSearchParamsModalStore');
