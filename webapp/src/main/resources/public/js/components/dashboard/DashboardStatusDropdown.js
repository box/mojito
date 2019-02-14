import React from "react";
import PropTypes from 'prop-types';
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";


class DashboardStatusDropdown extends React.Component {

    static propTypes = {

        //TODO(ja) remove required for null?
        "deleted": PropTypes.bool.isRequired,
        "undeleted": PropTypes.bool.isRequired,
        "onlyMyBranches": PropTypes.string.isRequired,


        "onDeletedChanged": PropTypes.func.isRequired,
        "onUndeletedChanged": PropTypes.func.isRequired,
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
console.log(this.props.onlyMyBranches)
        return (

            <DropdownButton id="DashboardStatusDropdown"
                            title={this.props.intl.formatMessage({id: "search.statusDropdown.title"})}>

                <MenuItem header><FormattedMessage id="dashboardSearch.statusDropdown.owner"/></MenuItem>
                <MenuItem eventKey={"onlyMyBranches"} active={this.props.onlyMyBranches}
                          onSelect={() => this.props.onOnlyMyBranchesChanged(!this.props.onlyMyBranches)}>
                    <FormattedMessage id="dashboardSearch.statusDropdown.owner.onlyMyBranches"/>
                </MenuItem>

                <MenuItem divider/>

                <MenuItem header><FormattedMessage id="dashboardSearch.statusDropdown.deleted"/></MenuItem>
                {this.renderFilterMenuItem("deleted", true, this.props.deleted, this.props.onDeletedChanged)}
                {this.renderFilterMenuItem("undeleted", false, this.props.undeleted, this.props.onUndeletedChanged)}
            </DropdownButton>
        );
    }
};


export default injectIntl(DashboardStatusDropdown);
