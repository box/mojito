import React from "react";
import {injectIntl} from "react-intl";
import Multiselect from "react-bootstrap-multiselect";
import FluxyMixin from "alt/mixins/FluxyMixin";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import RepositoriesStore from "../../stores/RepositoryStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";

let RepositoryDropDown = React.createClass({

    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "repositoriesFetched": RepositoriesStore,
            "onSearchParamsStoreChanged": SearchParamsStore
        }
    },

    onSearchParamsStoreChanged: function (e) {

        let searchParams = SearchParamsStore.getState();

        if (searchParams.changedParam === SearchConstants.REPOSITORIES_CHANGED ||
            searchParams.changedParam === SearchConstants.UPDATE_ALL) {

            this.updateComponent();
        }
    },

    repositoriesFetched: function () {
        this.updateComponent();
    },

    updateComponent: function () {

        this.setState({
            "repositories": RepositoriesStore.getState().repositories,
            "selectedRepoIds": SearchParamsStore.getState().repoIds
        });

        this.refs.repositoryDropdownRef.syncData();
    },

    getInitialState: function () {
        return {
            "repositories": [],
            "selectedRepoIds": []
        };
    },

    createDropDownOption: function (repository) {

        var result = {
            "value": repository.id,
            "label": repository.name,
            "selected": this.state.selectedRepoIds.indexOf(repository.id) > -1
        };

        return result;
    },

    repositorySelected: function (optionArray, isSelected) {

        let option = optionArray[0];

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.REPOSITORIES_CHANGED,
            "repositoryId": parseInt(option.value),
            "isSelected": isSelected
        });
    },

    getRepositoryOptions: function () {
        return this.state.repositories.map(this.createDropDownOption);
    },

    /**
     * Gets the text to display on the button.
     *
     * if 1 repository selected the named is shown, else the number of selected repositories is displayed (with proper i18n support)
     *
     * @param options
     * @param select
     * @returns {string} text to display on the button
     */
    getButtonText: function (options, select) {

        let label = '';

        let numberOfSelectedRepositories = options.filter(':selected').length;

        if (numberOfSelectedRepositories == 1) {
            label = options[0].label;
        } else {
            label = this.props.intl.formatMessage({ id: "search.repository.btn.text"}, {"numberOfSelectedRepositories": numberOfSelectedRepositories});
        }

        return label;
    },

    render: function () {

        let options = this.getRepositoryOptions();

        return (
            <span>
                <Multiselect onChange={this.repositorySelected}
                             enableFiltering={true}
                             enableCaseInsensitiveFiltering={false}
                             buttonText={this.getButtonText}
                             filterPlaceholder={this.props.intl.formatMessage({ id: "search.repository.filterPlaceholder" })}
                             ref="repositoryDropdownRef" data={options} multiple/>
            </span>
        );
    }
});

export default injectIntl(RepositoryDropDown);
