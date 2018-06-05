import _ from "lodash";
import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";
import FluxyMixin from "alt-mixins/FluxyMixin";

import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";

let StatusDropdown = React.createClass({

    mixins: [FluxyMixin],

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
            "translate" : searchParams.translate,
            "doNotTranslate" : searchParams.doNotTranslate,
        });
    },

    getInitialState() {
        return {
            "status": this.getInitialStatus(),
            "used": false,
            "unUsed": false,
            "translate" : false,
            "doNotTranslate" : false,
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

        let msg = isYes ? this.props.intl.formatMessage({ id: "search.statusDropdown.yes" }) : this.props.intl.formatMessage({ id: "search.statusDropdown.no" });

        return (
            <MenuItem eventKey={filter} active={this.state[filter]} onSelect={this.onFilterSelected} >{msg}</MenuItem>
        );
    },

    getMessageForStatus(status) {
        switch (status) {
            case SearchParamsStore.STATUS.ALL:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.all" });
            case SearchParamsStore.STATUS.TRANSLATED:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.translated" });
            case SearchParamsStore.STATUS.UNTRANSLATED:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.untranslated" });
            case SearchParamsStore.STATUS.FOR_TRANSLATION:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.forTranslation" });
            case SearchParamsStore.STATUS.REVIEW_NEEDED:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.needsReview" });
            case SearchParamsStore.STATUS.REJECTED:
                return this.props.intl.formatMessage({ id: "search.statusDropdown.rejected" });
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

        return (
            <DropdownButton id="WorkbenchStatusDropdown" title={this.props.intl.formatMessage({ id: "search.statusDropdown.title" })}>

                <MenuItem header><FormattedMessage id="search.statusDropdown.status" /></MenuItem>
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.ALL)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.TRANSLATED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.UNTRANSLATED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.FOR_TRANSLATION)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.REVIEW_NEEDED)}
                    {this.renderStatusMenuItem(SearchParamsStore.STATUS.REJECTED)}

                <MenuItem divider />

                <MenuItem header><FormattedMessage id="search.statusDropdown.used" /></MenuItem>
                    {this.renderFilterMenuItem("used", true)}
                    {this.renderFilterMenuItem("unUsed", false)}

                <MenuItem divider />

                <MenuItem header><FormattedMessage id="search.statusDropdown.translate" /></MenuItem>
                    {this.renderFilterMenuItem("translate", true)}
                    {this.renderFilterMenuItem("doNotTranslate", false)}

            </DropdownButton>
        );
    }
});


export default injectIntl(StatusDropdown);
