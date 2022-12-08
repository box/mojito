import _ from "lodash";
import React from "react";
import createReactClass from 'create-react-class';
import {withRouter} from "react-router";
import FluxyMixin from "alt-mixins/FluxyMixin";
import LocalesDropdown from "./LocalesDropdown";
import RepositoryDropDown from "./RepositoryDropdown";
import SearchResults from "./SearchResults";
import StatusDropdown from "./StatusDropdown";
import SearchText from "./SearchText";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import LocationHistory from "../../utils/LocationHistory";
import AltContainer from "alt-container";
import GitBlameStore from "../../stores/workbench/GitBlameStore";
import GitBlameInfoModal from "./GitBlameInfoModal";
import GitBlameActions from "../../actions/workbench/GitBlameActions";
import BranchesScreenshotViewerModal from "../branches/BranchesScreenshotViewerModal";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions";
import GitBlameScreenshotViewerStore from "../../stores/workbench/GitBlameScreenshotViewerStore";
import UrlHelper from "../../utils/UrlHelper";
import TranslationHistoryStore from "../../stores/workbench/TranslationHistoryStore";
import TranslationHistoryModal from "./TranslationHistoryModal";
import TranslationHistoryActions from "../../actions/workbench/TranslationHistoryActions";

let Workbench = createReactClass({
    displayName: 'Workbench',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onSearchParamsStoreChanged": SearchParamsStore,
            "onGitBlameStoreUpdated": GitBlameStore,
            "onTranslationHistoryStoreUpdated": TranslationHistoryStore
        }
    },

    /**
     * Handler for SearchParamsStore changes
     *
     * @param {object} searchParams The SearchParamsStore state
     */
    onSearchParamsStoreChanged: function (searchParams) {
        this.updateLocationForSearchParam(searchParams);
    },

    onGitBlameStoreUpdated(store) {
        this.setState({"isShowGitBlameModal": store.show});
    },

    onTranslationHistoryStoreUpdated(store) {
        this.setState({"isShowTranslationHistoryModal": store.show});
    },

    /**
     * Updates the browser location based to reflect search
     *
     * If the URL is only workbench replace the state (to reflect the search param) else if the query has changed
     * push a new state to keep track of the change param modification.
     *
     * @param {object} searchParams The SearchParamsStore state
     */
    updateLocationForSearchParam(searchParams) {
        LocationHistory.updateLocation(this.props.router, "/workbench", searchParams);
    },

    /**
     * @param {string} queryString Starts with ?
     * @return boolean
     */
    isCurrentQueryEqual: function (queryString) {
        return queryString === window.location.search;
    },

    /**
     * Create query string given SearchParams
     *
     * @param searchParams
     * @return {*}
     */
    buildQuery: function (searchParams) {
        let cloneParam = _.clone(searchParams);
        delete cloneParam["changedParam"];
        return UrlHelper.toQueryString(cloneParam);
    },

    render: function () {
        return (
            <div>
                <div className="pull-left">
                    <RepositoryDropDown />
                    <LocalesDropdown />
                </div>

                <SearchText />
                <StatusDropdown />

                <div className="mtl mbl">
                    <SearchResults />
                </div>
                <AltContainer store={GitBlameStore}>
                    <GitBlameInfoModal
                        onCloseModal={GitBlameActions.close}
                        onViewScreenshotClick={(branchScreenshots) => {
                            GitBlameScreenshotViewerActions.openScreenshotsViewer(branchScreenshots);
                        }}/>
                </AltContainer>
                <AltContainer store={GitBlameScreenshotViewerStore}>
                    <BranchesScreenshotViewerModal
                        onGoToPrevious={() => {
                            GitBlameScreenshotViewerActions.goToPrevious();
                        }}
                        onGoToNext={() => {
                            GitBlameScreenshotViewerActions.goToNext();
                        }}
                        onClose={() => {
                            GitBlameScreenshotViewerActions.closeScreenshotsViewer();
                        }}
                        onDelete={() => {
                            GitBlameScreenshotViewerActions.delete();
                        }}
                    />
                </AltContainer>
                <AltContainer store={TranslationHistoryStore}>
                    <TranslationHistoryModal onCloseModal={TranslationHistoryActions.close}/>
                </AltContainer>
            </div>
        );
    },
});

export default withRouter(Workbench);
