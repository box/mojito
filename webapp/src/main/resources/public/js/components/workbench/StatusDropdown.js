import _ from "lodash";
import React from "react";
import ReactIntl from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";
import FluxyMixin from "alt/mixins/FluxyMixin";

import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";

let {IntlMixin} = ReactIntl;

let StatusDropdown = React.createClass({

    mixins: [IntlMixin, FluxyMixin],

    statics: {
        storeListeners: {
            "onSearchParamsChanged": SearchParamsStore
        }
    },


    onSearchParamsChanged() {

        var searchParams = SearchParamsStore.getState();

        this.setState({
            "status": searchParams.status,
            "used": searchParams.used,
            "unUsed": searchParams.unUsed,
        });
    },

    getInitialState() {
        return {
            "status": this.getInitialStatus(),
            "used": false,
            "unUsed": false,
        };
    },

    /**
     * Get initial searchAttribute value
     *
     * @return {string}
     */
    getInitialStatus() {
        return this.props.status ? this.props.status : SearchParamsStore.STATUS.ALL;
    },


    onStatusSelected(status) {

        if (status !== this.state.status) {
            this.setStateAndCallSearchParamChanged({status: status});
        }
    },


    getMessageForFilterHeader(filter) {

        switch (filter) {
            case "used" :
                return this.getIntlMessage("search.statusDropdown.used");
        }
    },

    setStateAndCallSearchParamChanged(state) {
        this.setState(state, function () {
            this.callSearchParamChanged();
        });
    },

    callSearchParamChanged() {
        let actionData = {
            "changedParam": SearchConstants.SEARCHFILTER_CHANGED,
            "searchFilterParam": "status",
            "searchFilterParamValue": this.state.status
        };

        WorkbenchActions.searchParamsChanged(actionData);
    },

    /**
     * When a filter is selected, update the search params
     *
     * @param filter selected filter
     */
    onFilterSelected(filter) {

        let newFilterValue = !this.state[filter];

        let actionData = {
            "changedParam": SearchConstants.SEARCHFILTER_CHANGED,
            "searchFilterParam": filter,
            "searchFilterParamValue": newFilterValue
        };

        WorkbenchActions.searchParamsChanged(actionData);
    },

    /**
     * Renders the filter menu item.
     *
     * @param filter
     * @param isYes
     * @returns {XML}
     */
    renderFilterMenuItem(filter, isYes) {

        let msg = isYes ? this.getIntlMessage("search.statusDropdown.yes") : this.getIntlMessage("search.statusDropdown.no");

        return (
            <MenuItem eventKey={filter} active={this.state[filter]} onSelect={this.onFilterSelected} >{msg}</MenuItem>
        );
    },

    getMessageForStatus(status) {
        switch (status) {
            case SearchParamsStore.STATUS.ALL:
                return this.getIntlMessage("search.statusDropdown.all");
            case SearchParamsStore.STATUS.TRANSLATED:
                return this.getIntlMessage("search.statusDropdown.translated");
            case SearchParamsStore.STATUS.FOR_TRANSLATION:
                return this.getIntlMessage("search.statusDropdown.forTranslation");
            case SearchParamsStore.STATUS.REVIEW_NEEDED:
                return this.getIntlMessage("search.statusDropdown.needsReview");
            case SearchParamsStore.STATUS.REJECTED:
                return this.getIntlMessage("search.statusDropdown.rejected");
        }
    },

    renderStatusMenuItem(status) {
        return (
            <MenuItem eventKey={status} active={this.state.status === status} onSelect={this.onStatusSelected} >
                       {this.getMessageForStatus(status)}
            </MenuItem>
        );
    },

    render() {

        let searchParams = SearchParamsStore.getState();

        return (
            <DropdownButton title={this.getIntlMessage("search.statusDropdown.title")}>

                <MenuItem header>{this.getIntlMessage("search.statusDropdown.status")}</MenuItem>
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.ALL)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.TRANSLATED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.FOR_TRANSLATION)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.REVIEW_NEEDED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.REJECTED)}

                <MenuItem divider />

                <MenuItem header>{this.getMessageForFilterHeader("used")}</MenuItem>
                    {this.renderFilterMenuItem("used", true)}
                    {this.renderFilterMenuItem("unUsed", false)}

            </DropdownButton>
        );
    }
});


export default StatusDropdown;
