import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";
import FluxyMixin from "alt/mixins/FluxyMixin";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import RepositoriesStore from "../../stores/RepositoryStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";

let RepositoryDropDown = React.createClass({

    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onRepositoriesFetched": RepositoriesStore,
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

        let searchParams = SearchParamsStore.getState();

        if (searchParams.changedParam === SearchConstants.REPOSITORIES_CHANGED ||
                searchParams.changedParam === SearchConstants.UPDATE_ALL) {

            this.updateComponent();
        }
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
            "repositories": RepositoriesStore.getState().repositories.slice().sort(),
            "selectedRepoIds": SearchParamsStore.getState().repoIds.slice().sort()
        });
    },

    /**
     *
     * @return {{repositories: number[], selectedRepoIds: string[], isDropdownOpenned: boolean}}
     */
    getInitialState: function () {
        return {
            "repositories": [],
            "selectedRepoIds": [],
            "isDropdownOpenned": false
        };
    },

    /**
     * Get list of repositories (with selected state) sorted by their display name
     *
     * @return {{id: number, name: string, selected: boolean}[]}}
     */
    getSortedRepositories() {
        let repositories = this.state.repositories
                .map((repository) => {
                    return {
                        "id": repository.id,
                        "name": repository.name,
                        "selected": this.state.selectedRepoIds.indexOf(repository.id) > -1
                    }
                }).sort((a, b) => a.name.localeCompare(b.name));

        return repositories;
    },

    /**
     * On dropdown selected event, add or remove the target repository from the
     * selected repository list base on its previous state (selected or not).
     *
     * @param repository the repository that was selected
     */
    onRepositorySelected(repository) {

        this.forceDropdownOpen = true;

        let id = repository.id;

        let newSelectedRepoIds = this.state.selectedRepoIds.slice();

        if (repository.selected) {
            _.pull(newSelectedRepoIds, id);
        } else {
            newSelectedRepoIds.push(id);
        }

        this.searchParamChanged(newSelectedRepoIds);
    },

    /**
     * Trigger the searchParamsChanged action for a given list of selected
     * repository ids.
     *
     * @param newSelectedRepoIds
     */
    searchParamChanged(newSelectedRepoIds) {

        let actionData = {
            "changedParam": SearchConstants.REPOSITORIES_CHANGED,
            "repoIds": newSelectedRepoIds
        };

        WorkbenchActions.searchParamsChanged(actionData);
    },

    /**
     * Gets the text to display on the button.
     *
     * if 1 repository selected the named is shown, else the number of selected repositories is displayed (with proper i18n support)
     *
     * @returns {string} text to display on the button
     */
    getButtonText() {

        let label = '';

        let numberOfSelectedRepositories = this.state.selectedRepoIds.length;

        if (numberOfSelectedRepositories == 1) {
            let repoId = this.state.selectedRepoIds[0];
            label = this.getRepositoryById(repoId).name;
        } else {
            label = this.props.intl.formatMessage({id: "search.repository.btn.text"}, {'numberOfSelectedRepositories': numberOfSelectedRepositories});
        }

        return label;
    },

    /**
     * Gets a repository by id from the state.
     *
     * @param repoId the repository id
     */
    getRepositoryById(repoId) {
        return _.find(this.state.repositories, {"id": repoId});
    },

    /**
     * Gets the list of repository ids from the state.
     *
     * @returns {number[]}
     */
    getRepositoryIdsFromState() {
        return this.state.repositories.map((repository) => repository.id);
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
     * Selects all locales.
     */
    onSelectAll() {
        this.forceDropdownOpen = true;
        this.searchParamChanged(this.getRepositoryIdsFromState());
    },

    /**
     * Clear all selected locales.
     */
    onSelectNone() {
        this.forceDropdownOpen = true;
        this.searchParamChanged([]);
    },

    /**
     * Indicates if the select all menu item should be active.
     *
     * @returns {boolean}
     */
    isAllActive() {
        return this.state.selectedRepoIds.length > 0 && this.state.selectedRepoIds.length === this.getRepositoryIdsFromState().length;
    },

    /**
     * Indicates if the clear all menu item should be active.
     *
     * @returns {boolean}
     */
    isNoneActive() {
        return this.state.selectedRepoIds.length === 0;
    },

    /**
     * Renders the locale menu item list.
     *
     * @returns {XML}
     */
    renderRepositories() {
        return this.getSortedRepositories().map(
                (repository) =>
                        <MenuItem eventKey={repository} active={repository.selected} onSelect={this.onRepositorySelected}>{repository.name}</MenuItem>
        );
    },

    /**
     * @return {JSX}
     */
    render() {

        return (
                <span className="mlm repository-dropdown">
                <DropdownButton title={this.getButtonText()} onToggle={this.onDropdownToggle} open={this.state.isDropdownOpenned}>
                    <MenuItem active={this.isAllActive()} onSelect={this.onSelectAll}><FormattedMessage id="search.locale.selectAll"/></MenuItem>
                    <MenuItem active={this.isNoneActive()} onSelect={this.onSelectNone}><FormattedMessage id="search.locale.selectNone"/></MenuItem>
                    <MenuItem divider/>
                    {this.renderRepositories()}
                </DropdownButton>
                </span>
        );
    }

});

export default injectIntl(RepositoryDropDown);
