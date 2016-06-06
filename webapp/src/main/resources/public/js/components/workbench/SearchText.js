import $ from "jquery";

import FluxyMixin from "alt/mixins/FluxyMixin";
import keycode from "keycode";
import React from "react/addons";
import ReactIntl from 'react-intl';
import {DropdownButton, Input, MenuItem, Button, Glyphicon} from "react-bootstrap";

import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";

let {IntlMixin} = ReactIntl;

let SearchText = React.createClass({

    mixins: [IntlMixin, React.addons.LinkedStateMixin, FluxyMixin],

    statics: {
        storeListeners: {
            "onSearchParamsChanged": SearchParamsStore
        }
    },

    onSearchParamsChanged() {
        var searchParams = SearchParamsStore.getState();

        this.setState({
            "searchAttribute": searchParams.searchAttribute,
            "searchText": searchParams.searchText,
            "searchType": searchParams.searchType
        });
    },

    getInitialState() {
        return {
            "searchAttribute": this.getInitialSearchAttribute(),
            "searchType": this.getInitialSearchType(),
            "searchText": this.getInitialSearchText()
        };
    },

    /**
     * Get initial searchAttribute value
     *
     * @return {string}
     */
    getInitialSearchAttribute() {
        return this.props.searchAttribute ? this.props.searchAttribute : SearchParamsStore.SEARCH_ATTRIBUTES.TARGET;
    },

    /**
     * Get initial searchText value
     *
     * @return {string}
     */
    getInitialSearchText() {
        return this.props.searchText ? this.props.searchText : null;
    },

    /**
     * Get initial searchType value
     *
     * @return {string}
     */
    getInitialSearchType() {
        return this.props.searchType ? this.props.searchType : SearchParamsStore.SEARCH_TYPES.CONTAINS;
    },

    onSearchAttributeSelected(searchAttribute) {

        if (searchAttribute !== this.state.searchAttribute) {
            this.setStateAndCallSearchParamChanged({searchAttribute: searchAttribute});
        }
    },

    onSearchTypeSelected(searchType) {

        if (searchType !== this.state.searchType) {
            this.setStateAndCallSearchParamChanged({searchType: searchType});
        }
    },

    onKeyDownOnSearchText(e) {
        if (e.keyCode == keycode("enter")) {
            this.callSearchParamChanged();
        }
    },

    onSearchButtonClicked() {
        this.callSearchParamChanged();
    },

    setStateAndCallSearchParamChanged(state) {
        this.setState(state, function () {
            this.callSearchParamChanged();
        });
    },

    callSearchParamChanged() {
        let actionData = {
            "changedParam": SearchConstants.SEARCHTEXT_CHANGED,
            "data": this.state
        };

        WorkbenchActions.searchParamsChanged(actionData);
    },


    getMessageForSearchAttribute(searchAttribute) {
        switch (searchAttribute) {
            case SearchParamsStore.SEARCH_ATTRIBUTES.STRING_ID:
                return this.getIntlMessage("search.filter.id");
            case SearchParamsStore.SEARCH_ATTRIBUTES.SOURCE:
                return this.getIntlMessage("search.filter.source");
            case SearchParamsStore.SEARCH_ATTRIBUTES.TARGET:
                return this.getIntlMessage("search.filter.target");
        }
    },

    getMessageForSearchType(searchType) {
        switch (searchType) {
            case SearchParamsStore.SEARCH_TYPES.EXACT:
                return this.getIntlMessage("search.filter.exact");
            case SearchParamsStore.SEARCH_TYPES.CONTAINS:
                return this.getIntlMessage("search.filter.contains");
            case SearchParamsStore.SEARCH_TYPES.ILIKE:
                return this.getIntlMessage("search.filter.ilike");
        }
    },

    renderSearchAttributeMenuItem(searchAttribute) {
        return (
            <MenuItem eventKey={searchAttribute} active={this.state.searchAttribute === searchAttribute} onSelect={this.onSearchAttributeSelected} >
                   {this.getMessageForSearchAttribute(searchAttribute)}
            </MenuItem>
        );
    },

    renderSearchTypeMenuItem(searchType) {
        return (
            <MenuItem eventKey={searchType} active={this.state.searchType === searchType } onSelect={this.onSearchTypeSelected} >
                {this.getMessageForSearchType(searchType)}
            </MenuItem>
        );
    },

    renderDropdown() {
        return (
            <DropdownButton title={this.getMessageForSearchAttribute(this.state.searchAttribute)}>
                <MenuItem header>{this.getIntlMessage("search.filter.searchAttribute")}</MenuItem>
                            {this.renderSearchAttributeMenuItem(SearchParamsStore.SEARCH_ATTRIBUTES.STRING_ID)}
                            {this.renderSearchAttributeMenuItem(SearchParamsStore.SEARCH_ATTRIBUTES.SOURCE)}
                            {this.renderSearchAttributeMenuItem(SearchParamsStore.SEARCH_ATTRIBUTES.TARGET)}
                <MenuItem divider />
                <MenuItem header>{this.getIntlMessage("search.filter.searchType")}</MenuItem>
                            {this.renderSearchTypeMenuItem(SearchParamsStore.SEARCH_TYPES.EXACT)}
                            {this.renderSearchTypeMenuItem(SearchParamsStore.SEARCH_TYPES.CONTAINS)}
                            {this.renderSearchTypeMenuItem(SearchParamsStore.SEARCH_TYPES.ILIKE)}

            </DropdownButton>
        );
    },

    renderSearchButton() {
        return (
            <Button onClick={this.onSearchButtonClicked}>
                <Glyphicon glyph='glyphicon glyphicon-search' />
            </Button>
        );
    },

    render: function () {
        return (
            <Input type='text' valueLink={this.linkState("searchText")}
                placeholder={this.getIntlMessage("search.placeholder")}
                buttonBefore={this.renderDropdown()}
                buttonAfter={this.renderSearchButton()}
                onKeyDown={this.onKeyDownOnSearchText}
                wrapperClassName="col-xs-6"
            />
        );
    }
});

export default SearchText;
