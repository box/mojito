import React from "react";
import PropTypes from "prop-types";
import { DropdownButton, MenuItem } from "react-bootstrap";
import RepositoryActions from "../../actions/RepositoryActions";
import RepositoryStore from "../../stores/RepositoryStore";

class CreateJobRepositoryDropDown extends React.Component {
    static propTypes = {
        onSelect: PropTypes.func,
    };

    constructor(props) {
        super(props);
        this.state = {
            repositories: [],
            selected: null,
        };
        this.repositoryStoreChange = this.repositoryStoreChange.bind(this);
        this.handleSelect = this.handleSelect.bind(this);
    }

    componentDidMount() {
        RepositoryActions.getAllRepositories();
        RepositoryStore.listen(this.repositoryStoreChange);
        this.repositoryStoreChange(RepositoryStore.getState());
    }

    componentWillUnmount() {
        RepositoryStore.unlisten(this.repositoryStoreChange);
    }

    repositoryStoreChange(state) {
        const repositories = state.repositories || [];
        this.setState({ repositories });
    }

    handleSelect(eventKey) {
        this.setState({ selected: eventKey });
        if (this.props.onSelect) {
            this.props.onSelect(eventKey);
        }
    }

    render() {
        return (
            <div>
                <DropdownButton
                    id="create-job-repo-dropdown"
                    title={this.state.selected || "Choose a repository"}
                    onSelect={this.handleSelect}
                >
                    {this.state.repositories.map(repo => (
                        <MenuItem eventKey={repo.name} key={repo.name}>
                            {repo.name}
                        </MenuItem>
                    ))}
                </DropdownButton>
            </div>
        );
    }
}

export default CreateJobRepositoryDropDown;
