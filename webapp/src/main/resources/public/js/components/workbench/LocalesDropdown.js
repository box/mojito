import $ from "jquery";
import _ from "lodash";
import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";
import FluxyMixin from "alt-mixins/FluxyMixin";

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
     * Currently there is no way to prevent the dropdown to close on select
     * unless using a trick based on this attribute.
     *
     * Action that shouldn't close the dropdown can set this attribute to 'true'
     * This will prevent onDropdownToggle to actually close the dropdown.
     * Subsequent calls to onDropdownToggle will behave normally.
     */
    forceDropdownOpen: false,

    /**
     * Handler for when RepositoryStore is updated
     */
    onRepositoriesFetched() {
        this.updateComponent();
    },

    /**
     * Handler for when SearchParamsStore is updated
     */
    onSearchParamsChanged() {
        this.updateComponent();
    },

    /**
     * State the state based on the stores and sync data with the multiselect component
     */
    updateComponent() {

        this.setState({
            "bcp47Tags": this.getSortedBcp47TagsFromStore(),
            "fullyTranslatedBcp47Tags": this.getSortedFullyTranslatedBcp47TagsFromStore(),
            "selectedBcp47Tags": this.getSortedSelectedBcp47TagsFromStore()
        });
    },

    /**
     *
     * @return {{bcp47Tags: string[], fullyTranslatedBcp47Tags: string[], selectedBcp47Tags: string[], isDropdownOpenned: boolean}}
     */
    getInitialState() {
        return {
            "bcp47Tags": [],
            "fullyTranslatedBcp47Tags": [],
            "selectedBcp47Tags": [],
            "isDropdownOpenned": false
        };
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
     * Get list of locales (with selected state) sorted by their display name
     *
     * @return {{bcp47Tag: string, displayName: string, selected: boolean}[]}}
     */
    getSortedLocales() {
        let locales = this.state.bcp47Tags
                .map((bcp47Tag) => {
                    return {
                        "bcp47Tag": bcp47Tag,
                        "displayName": Locales.getDisplayName(bcp47Tag),
                        "selected": this.state.selectedBcp47Tags.indexOf(bcp47Tag) > -1
                    }
                }).sort((a, b) => a.displayName.localeCompare(b.displayName));

        return locales;
    },

    /**
     * On dropdown selected event, add or remove the target locale from the
     * selected locale list base on its previous state (selected or not).
     *
     * @param locale the locale that was selected
     */
    onLocaleSelected(locale) {

        this.forceDropdownOpen = true;

        let bcp47Tag = locale.bcp47Tag;

        let newSelectedBcp47Tags = this.state.selectedBcp47Tags.slice();

        if (locale.selected) {
            _.pull(newSelectedBcp47Tags, bcp47Tag);
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
     * @returns {string} text to display on the button
     */
    getButtonText() {

        let label = '';

        let numberOfSelectedLocales = this.state.selectedBcp47Tags.length;

        if (numberOfSelectedLocales == 1) {
            label = Locales.getDisplayName(this.state.selectedBcp47Tags[0]);
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
            this.setState({"isDropdownOpenned": true});
        } else {
            this.setState({"isDropdownOpenned": newOpenState});
        }
    },

    /**
     * Selects fully translated locales.
     */
    onSelectToBeFullyTranslated() {
        this.forceDropdownOpen = true;
        this.searchParamChanged(this.state.fullyTranslatedBcp47Tags.slice());
    },

    /**
     * Selects all locales.
     */
    onSelectAll() {
        this.forceDropdownOpen = true;
        this.searchParamChanged(this.state.bcp47Tags.slice());
    },

    /**
     * Clear all selected locales.
     */
    onSelectNone() {
        this.forceDropdownOpen = true;
        this.searchParamChanged([]);
    },

    /**
     * Indicates if the select to be fully translated menu item should be active.
     *
     * @returns {boolean}
     */
    isToBeFullyTranslatedActive() {
        return this.state.selectedBcp47Tags.length > 0 && _.isEqual(this.state.selectedBcp47Tags, this.state.fullyTranslatedBcp47Tags);
    },


    /**
     * Indicates if the select all menu item should be active.
     *
     * @returns {boolean}
     */
    isAllActive() {
        return this.state.selectedBcp47Tags.length > 0 && this.state.selectedBcp47Tags.length === this.state.bcp47Tags.length;
    },

    /**
     * Indicates if the clear all menu item should be active.
     *
     * @returns {boolean}
     */
    isNoneActive() {
        return this.state.selectedBcp47Tags.length === 0;
    },


    /**
     * Renders the locale menu item list.
     *
     * @returns {XML}
     */
    renderLocales() {
        return this.getSortedLocales().map(
                (locale) =>
                        <MenuItem key={"Workbench.LocaleDropdown." + locale.displayName} eventKey={locale} active={locale.selected} onSelect={this.onLocaleSelected}>{locale.displayName}</MenuItem>
        );
    },

    /**
     * @return {JSX}
     */
    render() {

        return (
                <span className="mlm locale-dropdown">
                <DropdownButton id="WorkbenchLocaleDropdown" title={this.getButtonText()} onToggle={this.onDropdownToggle} open={this.state.isDropdownOpenned}>
                    <MenuItem active={this.isToBeFullyTranslatedActive()} onSelect={this.onSelectToBeFullyTranslated}><FormattedMessage id="search.locale.selectToBeFullyTranslated"/></MenuItem>
                    <MenuItem active={this.isAllActive()} onSelect={this.onSelectAll}><FormattedMessage id="search.locale.selectAll"/></MenuItem>
                    <MenuItem active={this.isNoneActive()} onSelect={this.onSelectNone}><FormattedMessage id="search.locale.selectNone"/></MenuItem>
                    <MenuItem divider/>
                    {this.renderLocales()}
                </DropdownButton>
                </span>
        );

    }
});

export default injectIntl(LocalesDropDown);
