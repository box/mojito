import React from "react";
import PropTypes from 'prop-types';
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";


class BranchesStatusDropdown extends React.Component {

    static propTypes = {
        "deleted": PropTypes.bool.isRequired,
        "undeleted": PropTypes.bool.isRequired,
        "empty": PropTypes.bool.isRequired,
        "notEmpty": PropTypes.bool.isRequired,
        "onlyMyBranches": PropTypes.bool.isRequired,
        "onDeletedChanged": PropTypes.func.isRequired,
        "onUndeletedChanged": PropTypes.func.isRequired,
        "onEmptyChanged": PropTypes.func.isRequired,
        "onNotEmptyChanged": PropTypes.func.isRequired,
        "onOnlyMyBranchesChanged": PropTypes.func.isRequired,
    }

    onFilterSelected(filter) {
        this.props.onFilterSelected(filter)
    }

    renderFilterMenuItem(filter, isYes, prop, callback) {

        let msg = isYes ? this.props.intl.formatMessage({id: "search.statusDropdown.yes"}) : this.props.intl.formatMessage({id: "search.statusDropdown.no"});

        return (
            <MenuItem eventKey={filter} active={prop}
                      onSelect={() => {
                          callback(!prop)
                      }}>
                {msg}
            </MenuItem>
        );
    }

    render() {
        return (
            <DropdownButton id="BranchesStatusDropdown"
                            title={this.props.intl.formatMessage({id: "search.statusDropdown.title"})}>

                <MenuItem header><FormattedMessage id="branches.searchstatusDropdown.owner"/></MenuItem>
                <MenuItem eventKey={"onlyMyBranches"} active={this.props.onlyMyBranches}
                          onSelect={() => this.props.onOnlyMyBranchesChanged(!this.props.onlyMyBranches)}>
                    <FormattedMessage id="branches.searchstatusDropdown.owner.onlyMyBranches"/>
                </MenuItem>

                <MenuItem divider/>

                <MenuItem header><FormattedMessage id="branches.searchstatusDropdown.deleted"/></MenuItem>
                {this.renderFilterMenuItem("deleted", true, this.props.deleted, this.props.onDeletedChanged)}
                {this.renderFilterMenuItem("undeleted", false, this.props.undeleted, this.props.onUndeletedChanged)}

                <MenuItem divider/>

                <MenuItem header><FormattedMessage id="branches.searchstatusDropdown.empty"/></MenuItem>
                {this.renderFilterMenuItem("empty", false, this.props.empty, this.props.onEmptyChanged)}
                {this.renderFilterMenuItem("notEmpty", true, this.props.notEmpty, this.props.onNotEmptyChanged)}

            </DropdownButton>
        );
    }
};


export default injectIntl(BranchesStatusDropdown);
