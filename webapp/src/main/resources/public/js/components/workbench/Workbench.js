import $ from "jquery";
import _ from "lodash";

import React from "react";
import { withRouter } from "react-router";
import {FormattedMessage, FormattedNumber} from 'react-intl';

import FluxyMixin from "alt-mixins/FluxyMixin";

import LocalesDropdown from "./LocalesDropdown";

import RepositoryActions from "../../actions/RepositoryActions";
import RepositoryDropDown from "./RepositoryDropdown";
import RepositoryStore from "../../stores/RepositoryStore";

import SearchResults from "./SearchResults";
import StatusDropdown from "./StatusDropdown";
import SearchText from "./SearchText";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";

import WorkbenchActions from "../../actions/workbench/WorkbenchActions";

import LocationHistory from "../../utils/LocationHistory";
import AltContainer from "alt-container";
import GitBlameStore from "../../stores/workbench/GitBlameStore";
import GitBlameInfoModal from "./GitBlameInfoModal";
import GitBlameActions from "../../actions/workbench/GitBlameActions";
import {OverlayTrigger, Tooltip} from "react-bootstrap";

let Workbench = React.createClass({

    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onSearchParamsStoreChanged": SearchParamsStore,
            "onGitBlameStoreUpdated": GitBlameStore
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
        return $.param(cloneParam);
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
                    <GitBlameInfoModal onCloseModal={GitBlameActions.close}/>
                </AltContainer>

            </div>
        );
    }
});

export default withRouter(Workbench);
