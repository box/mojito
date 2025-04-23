import React from "react";
import createReactClass from 'create-react-class';
import {FormattedMessage, injectIntl} from "react-intl";
import {DropdownButton, MenuItem} from "react-bootstrap";
import FluxyMixin from "alt-mixins/FluxyMixin";
import JobStore from "../../stores/jobs/JobStore";
import JobActions from "../../actions/jobs/JobActions";

let RepositoryDropDown = createReactClass({
    displayName: 'RepositoryDropDown',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "getAllJobsSuccess": JobStore
        }
    },
    forceDropdownOpen: false,

    getAllJobsSuccess(store) {
        if(!this.loaded) {
            const repositories = store.jobs.map(j => j.repository).sort((r1, r2) => r1.localeCompare(r2))
            this.setState({ repositories: [...new Set(repositories)] })
            this.loaded = true;
        }
    },

    onDropdownToggle(newOpenState){
        if (this.forceDropdownOpen) {
            this.forceDropdownOpen = false;
            this.setState({"isDropdownOpenned": true});
        } else {
            this.setState({"isDropdownOpenned": newOpenState});
        }
    },

    getInitialState() {
        return {
            "repositories": [],
            "selectedRepositories": []
        };
    },

    onRepositorySelected(repository, event) {
        this.forceDropdownOpen = true;

        let selectedRepos = [...this.state.selectedRepositories];

        if(selectedRepos.includes(repository)) {
            selectedRepos = selectedRepos.filter(repo => repo !== repository);
        } else {
            selectedRepos.push(repository);
        }

        this.setState({selectedRepositories: selectedRepos});
        JobActions.setJobFilter(selectedRepos);
    },

    onSelectAll() {
        this.forceDropdownOpen = true;
        this.setState({selectedRepositories: []});
        JobActions.setJobFilter([]);
    },

    isAllActive() {
        return this.state.selectedRepositories.length === 0;
    },

    getButtonText() {

        let label = '';

        let numberOfSelectedRepositories = this.state.selectedRepositories.length;

        if (numberOfSelectedRepositories === 1) {
            let repo = this.state.selectedRepositories[0];
            label = repo ? repo : "";
        } else {
            label = this.props.intl.formatMessage({"id": "search.repository.btn.text"}, {"numberOfSelectedRepositories": numberOfSelectedRepositories});
        }

        return label;
    },

    renderRepositories() {
        return this.state.repositories.map(
            (repository) =>
                <MenuItem key={`JobsRepositoryDropdown.${repository}`} eventKey={repository} active={this.state.selectedRepositories.includes(repository)} onSelect={this.onRepositorySelected}>{repository}</MenuItem>
        );
    },


    /**
     * @return {JSX}
     */
    render() {

        return (
            <span className="mlm repository-dropdown">
                <DropdownButton id="WorkbenchRepositoryDropdown" title={this.getButtonText()} onToggle={this.onDropdownToggle} open={this.state.isDropdownOpenned} disabled={this.state.repositories.length === 0}>
                    <MenuItem id="WorkbenchRepositoryDropdown.selectAll" active={this.isAllActive()} onSelect={this.onSelectAll}><FormattedMessage id="search.repository.selectAll"/></MenuItem>

                    <MenuItem id="WorkbenchRepositoryDropdown.divider" divider/>

                    {this.renderRepositories()}
                </DropdownButton>
            </span>
        );
    },
});

export default injectIntl(RepositoryDropDown);
