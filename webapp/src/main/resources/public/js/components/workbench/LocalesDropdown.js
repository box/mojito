import $ from "jquery";
import _ from "lodash";
import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";
import FluxyMixin from "alt/mixins/FluxyMixin";

import RepositoryStore from "../../stores/RepositoryStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import Locales from "../../utils/Locales";

let LocalesDropDown = React.createClass({

    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onRepositoriesFetched": RepositoryStore,
            "onSearchParamsChanged": SearchParamsStore
        }
    },

    /**
     * Handler for when RepositoryStore is updated
     */
    onRepositoriesFetched: function () {
        this.updateComponent();
    },

    /**
     * Handler for when SearchParamsStore is updated
     */
    onSearchParamsChanged: function () {
        this.updateComponent();
    },

    /**
     * State the state based on the stores and sync data with the multiselect component
     */
    updateComponent: function () {

        this.setState({
            bcp47Tags: this.getSortedBcp47TagsFromStore(),
            fullyTranslatedBcp47Tags: this.getSortedFullyTranslatedBcp47TagsFromStore(),
            selectedBcp47Tags: this.getSortedSelectedBcp47TagsFromStore()
        });
    },

    /**
     * Gets sorted bcp47tags from stores.
     *
     * Sort is important to ensure later array comparison in the component will
     * work as expected.
     *
     * @returns {string[]}
     */
    getSortedBcp47TagsFromStore() {
        return RepositoryStore.getAllBcp47TagsForRepositoryIds(SearchParamsStore.getState().repoIds).sort();
    },

    /**
     * Gets sorted fully translated bcp47tags from stores.
     *
     * Sort is important to ensure later array comparison in the component will
     * work as expected.
     *
     * @returns {string[]}
     */
    getSortedFullyTranslatedBcp47TagsFromStore() {
        return RepositoryStore.getAllBcp47TagsForRepositoryIds(SearchParamsStore.getState().repoIds, true).sort();
    },

    /**
     * Gets sorted selected bcp47tags from stores.
     *
     * Sort is important to ensure later array comparison in the component will
     * work as expected.
     *
     * @returns {string[]}
     */
    getSortedSelectedBcp47TagsFromStore() {
        return SearchParamsStore.getState().bcp47Tags.sort();
    },

    /**
     *
     * @return {{bcp47Tags: string[], fullyTranslatedBcp47Tags: string[], selectedBcp47Tags: string[], dropdownOpen: boolean}}
     */
    getInitialState: function () {
        return {
            "bcp47Tags": [],
            "fullyTranslatedBcp47Tags": [],
            "selectedBcp47Tags": [],
            "dropdownOpen": false
        };
    },

    /**
     * Get an object that contains locale information (display name, if
     * selected or not).
     *
     * @param bcp47Tag the locale bcp47 tag
     * @return {{bcp47Tag: string, displayName: string, selected: boolean}}
     */
    getLocale: function (bcp47Tag) {

        return {
            "bcp47Tag": bcp47Tag,
            "displayName": Locales.getDisplayName(bcp47Tag),
            "selected": this.state.selectedBcp47Tags.indexOf(bcp47Tag) > -1
        };
    },

    /**
     * Get list of locales sorted by their display name
     *
     * @return {{value: string, selected: boolean}[]}}
     */
    getSortedLocales: function () {
        let localeOptions = this.state.bcp47Tags
                .map(this.getLocale)
                .sort((a, b) => a.displayName.localeCompare(b.displayName));
        return localeOptions;

    },

    /**
     * On dropdown selected event, add or remove the target locale from the
     * selected locale list base on its previous state (selected or not).
     *
     * @param locale the locale that was selected
     */
    onLocaleSelected(locale) {

        // Currently there is no way to prevent the dropdown to close on select unless using this trick
        this.forceDropdownOpen = true;

        let bcp47Tag = locale.bcp47Tag;

        let newSelectedBcp47Tags = this.state.selectedBcp47Tags.slice();

        if (locale.selected) {
            newSelectedBcp47Tags = _.pull(this.state.selectedBcp47Tags, bcp47Tag);
        } else {
            newSelectedBcp47Tags.push(bcp47Tag);
        }

        this.searchParamChanged(newSelectedBcp47Tags);
    },

    /**
     * Trigger the searchParamsChanged action for a given list of selected
     * bcp47 tags.
     *
     * @param newSelectedBcp47Tags
     */
    searchParamChanged(newSelectedBcp47Tags) {

        let actionData = {
            "changedParam": SearchConstants.LOCALES_CHANGED,
            "bcp47Tags": newSelectedBcp47Tags
        };

        WorkbenchActions.searchParamsChanged(actionData);
    },

    /**
     * Gets the text to display on the button.
     *
     * if 1 locale selected the named is shown, else the number of selected locale is displayed (with proper i18n support)
     *
     * @param options
     * @param select
     * @returns {string} text to display on the button
     */
    getButtonText: function (options, select) {

        let label = '';

        let numberOfSelectedLocales = this.state.selectedBcp47Tags.length;

        if (numberOfSelectedLocales == 1) {
            label = this.getSortedLocales()[0].displayName;
        } else {
            label = this.props.intl.formatMessage({id: "search.locale.btn.text"}, {'numberOfSelectedLocales': numberOfSelectedLocales});
        }

        return label;
    },

    /**
     * Here we handle the logic to keep the dropdown open because it is not
     * supported by default react-bootstrap component.
     *
     * "forceDropdownOpen" can be set in any function that wants to prevent the
     * the dropdown to close.
     *
     * @param newOpenState
     */
    onDropdownToggle(newOpenState){

        if (this.forceDropdownOpen) {
            this.forceDropdownOpen = false;
            this.setState({dropdownOpen: true});
        } else {
            this.setState({dropdownOpen: newOpenState});
        }
    },

    /**
     * Selects fully translated locales.
     */
    onSelectTranslated() {
        // Currently there is no way to prevent the dropdown to close on select unless using this trick
        this.forceDropdownOpen = true;
        this.searchParamChanged(this.state.fullyTranslatedBcp47Tags.slice());
    },

    /**
     * Selects all locales.
     */
    onSelectAll() {
        // Currently there is no way to prevent the dropdown to close on select unless using this trick
        this.forceDropdownOpen = true;
        this.searchParamChanged(this.state.bcp47Tags.slice());
    },

    /**
     * Clear all selected locales.
     */
    onClearAll() {
        // Currently there is no way to prevent the dropdown to close on select unless using this trick
        this.forceDropdownOpen = true;
        this.searchParamChanged([]);
    },

    /**
     * Indicates if the select translated menu item should be disabled.
     *
     * @returns {boolean}
     */
    isSelectTranslatedDisabled() {
        return _.isEqual(this.state.selectedBcp47Tags, this.state.fullyTranslatedBcp47Tags);
    },


    /**
     * Indicates if the select all menu item should be disabled.
     *
     * @returns {boolean}
     */
    isSelectAllDisabled() {
        return this.state.selectedBcp47Tags.length === this.state.bcp47Tags.length;
    },

    /**
     * Indicates if the clear all menu item should be disabled.
     *
     * @returns {boolean}
     */
    isClearAllDisabled() {
        return this.state.selectedBcp47Tags.length === 0;
    },


    /**
     * Renders the locale menu item list.
     *
     * @returns {Array}
     */
    renderLocales() {
        return this.getSortedLocales().map(this.renderLocale);
    },

    /**
     * Render a locale menu item.
     *
     * @param locale
     * @returns {XML}
     */
    renderLocale(locale) {
        return (
                <MenuItem eventKey={locale} active={locale.selected} onSelect={this.onLocaleSelected}>{locale.displayName}</MenuItem>
        );
    },

    /**
     * @return {JSX}
     */
    render: function () {

        return (
                <span className="mlm localeDropdown">
                <DropdownButton title={this.getButtonText()} onToggle={this.onDropdownToggle} open={this.state.dropdownOpen}>
                    <MenuItem disabled={this.isSelectTranslatedDisabled()} onSelect={this.onSelectTranslated}>Select Translated</MenuItem>
                    <MenuItem disabled={this.isSelectAllDisabled()} onSelect={this.onSelectAll}>Select All</MenuItem>
                    <MenuItem disabled={this.isClearAllDisabled()} onSelect={this.onClearAll}>Clear All</MenuItem>
                    <MenuItem divider/>
                    {this.renderLocales()}
                </DropdownButton>
                </span>
        );

    }
});

export default injectIntl(LocalesDropDown);
