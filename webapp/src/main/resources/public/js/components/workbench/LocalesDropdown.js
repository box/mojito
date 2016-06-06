import React from "react";
import ReactIntl from 'react-intl';
import Multiselect from "react-bootstrap-multiselect";
import FluxyMixin from "alt/mixins/FluxyMixin";

import RepositoryStore from "../../stores/RepositoryStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import Locales from "../../utils/Locales";

let {IntlMixin, FormattedMessage} = ReactIntl;

let LocalesDropDown = React.createClass({

    mixins: [IntlMixin, FluxyMixin],

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

        let searchParams = SearchParamsStore.getState();

        if (searchParams.changedParam === SearchConstants.REPOSITORIES_CHANGED ||
            searchParams.changedParam === SearchConstants.UPDATE_ALL) {

            this.updateComponent();
        }
    },

    /**
     * State the state based on the stores and sync data with the multiselect component
     */
    updateComponent: function () {

        let searchParams = SearchParamsStore.getState();

        this.setState({
            "selectedBcp47Tags": searchParams.bcp47Tags,
            "selectedRepoIds": searchParams.repoIds
        });

        this.refs.localeDropdownRef.syncData();
    },

    /**
     *
     * @return {{repositories: object[], selectedRepoIds: string[], selectedBcp47Tags: string[]}}
     */
    getInitialState: function () {
        return {
            "selectedBcp47Tags": [],
            "selectedRepoIds": []
        };
    },

    /**
     * Create locale opeion
     *
     * @param localeKey
     * @return {{value: string, selected: boolean}}
     */
    createLocaleOption: function (bcp47Tag) {

        return {
            "value": bcp47Tag,
            "label": Locales.getDisplayName(bcp47Tag),
            "selected": this.state.selectedBcp47Tags.indexOf(bcp47Tag) > -1
        };
    },

    /**
     * Get available locales for the LocalesDropDown
     *
     * @return {{value: string, selected: boolean}[]}}
     */
    getAvailableLocales: function () {
        let localeOptions = RepositoryStore.getAllBcp47TagsForRepositoryIds(this.state.selectedRepoIds)
            .map(this.createLocaleOption)
            .sort((a, b) =>   Locales.getDisplayName(a.value).localeCompare(Locales.getDisplayName(b.value)));
        return localeOptions;
    },

    /**
     * @param {object} optionArray
     * @param {bool} isSelected
     */
    localeSelected: function (optionArray, isSelected) {

        let option = optionArray[0];
        let actionData = {
            "changedParam": SearchConstants.LOCALES_CHANGED,
            "locale": option.value,
            "isSelected": isSelected
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

        let numberOfSelectedLocales = options.filter(':selected').length;

        if (numberOfSelectedLocales == 1) {
            label = options[0].label;
        } else {
            label = this.formatMessage(this.getIntlMessage("search.locale.btn.text"), {'numberOfSelectedLocales': numberOfSelectedLocales});
        }

        return label;
    },

    /**
     * @return {JSX}
     */
    render: function () {

        let localeOptions = this.getAvailableLocales();

        return (
            <span className="mlm">
                <Multiselect onChange={this.localeSelected}
                    disabled={true}
                    id="localesDropDown"
                    enableFiltering={true}
                    buttonText={this.getButtonText}
                    filterPlaceholder={this.getIntlMessage("search.locale.filterPlaceholder")}
                    ref="localeDropdownRef" data={localeOptions} multiple />
            </span>
        );
    }
});

export default LocalesDropDown;
