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

let Workbench = React.createClass({

    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onSearchParamsStoreChanged": SearchParamsStore
        }
    },

    componentDidMount: function () { 
        RepositoryActions.getAllRepositories();
    },

    /**
     * Handler for SearchParamsStore changes
     *
     * @param {object} searchParams The SearchParamsStore state
     */
    onSearchParamsStoreChanged: function (searchParams) {
        this.updateLocationForSearchParam(searchParams);
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

            </div>
        );
    }
});

export default withRouter(Workbench);
