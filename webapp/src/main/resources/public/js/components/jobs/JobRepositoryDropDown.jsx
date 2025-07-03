import React from "react";
import PropTypes from "prop-types";
import { DropdownButton, MenuItem } from "react-bootstrap";
import RepositoryActions from "../../actions/RepositoryActions";
import RepositoryStore from "../../stores/RepositoryStore";

class JobRepositoryDropDown extends React.Component {
    static propTypes = {
        onSelect: PropTypes.func,
        selectedRepository: PropTypes.string,
    };

    constructor(props) {
        super(props);
        this.state = {
            repositories: [],
        };
        this.repositoryStoreChange = this.repositoryStoreChange.bind(this);
        this.handleSelect = this.handleSelect.bind(this);
    }

    componentDidMount() {
        RepositoryStore.listen(this.repositoryStoreChange);
        this.repositoryStoreChange(RepositoryStore.getState());

        if (!RepositoryStore.getState().repositories || RepositoryStore.getState().repositories.length === 0) {
            RepositoryActions.getAllRepositories();
        }
    }

    componentWillUnmount() {
        RepositoryStore.unlisten(this.repositoryStoreChange);
    }

    repositoryStoreChange(state) {
        const repositories = state.repositories || [];
        this.setState({ repositories });
    }

    handleSelect(repo) {
        if (this.props.onSelect) {
            this.props.onSelect(repo.name);
        }
    }

    render() {
        return (
            <div>
                <DropdownButton
                    id="create-job-repo-dropdown"
                    title={this.props.selectedRepository ? this.props.selectedRepository : "Choose a repository"}
                    onSelect={this.handleSelect}
                >
                    {this.state.repositories.map(repo => (
                        <MenuItem eventKey={repo} key={repo.id}>
                            {repo.name}
                        </MenuItem>
                    ))}
                </DropdownButton>
            </div>
        );
    }
}

export default JobRepositoryDropDown;