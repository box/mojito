import $ from "jquery";
import _ from "lodash";
import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import Multiselect from "react-bootstrap-multiselect";
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
            bcp47Tags: RepositoryStore.getAllBcp47TagsForRepositoryIds(SearchParamsStore.getState().repoIds),
            selectedBcp47Tags: SearchParamsStore.getState().bcp47Tags
        });
    },

    /**
     *
     * @return {{bcp47Tags: string[], selectedBcp47Tags: string[]}}
     */
    getInitialState: function () {
        return {
            "bcp47Tags": [],
            "selectedBcp47Tags": []
        };
    },

    /**
     * Callback for multiselect change. Compare the selected locales with the
     * locale in the state. If different, update the state and call the action
     * to propagate locale change.
     */
    onMultiSelectChange() {

        let selectedBcp47TagsFromMultiSelect = this.getSelectedBcp47TagsFromMultiSelect();

        if (!_.isEqual(this.state.selectedBcp47Tags, selectedBcp47TagsFromMultiSelect)) {

            this.setState({
                selectedBcp47Tags: selectedBcp47TagsFromMultiSelect
            }, () => {

                let actionData = {
                    "changedParam": SearchConstants.LOCALES_CHANGED,
                    "bcp47Tags": selectedBcp47TagsFromMultiSelect
                };

                WorkbenchActions.searchParamsChanged(actionData);
            });
        }
    },

    /**
     * Get the selected bcp47 tags from the multi select
     * 
     * @returns {string[]}
     */
    getSelectedBcp47TagsFromMultiSelect() {

        console.log("getSelectedBcp47TagsFromMultiSelect");

        let selected = $('#localesDropDown option:selected').map(function (a, item) {
            return item.value;
        }).toArray();

        return selected;
    },

    /**
     * Create locale option
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
     * Get locale options for the LocalesDropDown
     *
     * @return {{value: string, selected: boolean}[]}}
     */
    getLocaleOptions: function () {
        let localeOptions = this.state.bcp47Tags
                .map(this.createLocaleOption)
                .sort((a, b) => Locales.getDisplayName(a.value).localeCompare(Locales.getDisplayName(b.value)));
        return localeOptions;
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
            label = this.props.intl.formatMessage({id: "search.locale.btn.text"}, {'numberOfSelectedLocales': numberOfSelectedLocales});
        }

        return label;
    },

    /**
     * @return {JSX}
     */
    render: function () {

        let localeOptions = this.getLocaleOptions();

        return (
                <span className="mlm">
                <Multiselect
                        onChange={this.onMultiSelectChange}
                        onSelectAll={this.onMultiSelectChange}
                        selectAllText={this.props.intl.formatMessage({ id: "search.locale.selectAll" })}
                        includeSelectAllOption={true}
                        id="localesDropDown"
                        buttonText={this.getButtonText}
                        enableFiltering={true}
                        enableCaseInsensitiveFiltering={false}
                        filterPlaceholder={this.props.intl.formatMessage({ id: "search.locale.filterPlaceholder" })}
                        ref="localeDropdownRef"
                        data={localeOptions}
                        multiple/>
            </span>
        );
    }
});

export default injectIntl(LocalesDropDown);
