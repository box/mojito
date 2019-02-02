import React from "react";
import PropTypes from 'prop-types';
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";


class DashboardStatusDropdown extends React.Component {

    static propTypes = {
        "isMine": PropTypes.bool.isRequired,
        "deleted": PropTypes.bool.isRequired,
        "undeleted": PropTypes.bool.isRequired,

        "onFilterSelected": PropTypes.func.isRequired

    }

    onFilterSelected(filter) {
        this.props.onFilterSelected(filter)
    }

    /**
     * Renders the filter menu item.
     *
     * @param filter
     * @param isYes
     * @returns {XML}
     */
    renderFilterMenuItem(filter, isYes) {

        let msg = isYes ? this.props.intl.formatMessage({ id: "search.statusDropdown.yes" }) : this.props.intl.formatMessage({ id: "search.statusDropdown.no" });

        return (
            <MenuItem eventKey={filter} active={this.props[filter]} onSelect={() => this.onFilterSelected(filter)} >{msg}</MenuItem>
        );
    }


    render() {

        return (

            <DropdownButton
                id="DashboardStatusDropdown"
                title={this.props.intl.formatMessage({id: "search.statusDropdown.title"})}
            >
                <MenuItem header><FormattedMessage id="dashboardSearch.statusDropdown.owner"/></MenuItem>
                <MenuItem eventKey={"isMine"} active={this.props.isMine} onSelect={() => this.onFilterSelected("isMine")} >
                    <FormattedMessage id="dashboardSearch.statusDropdown.owner.mine"/>
                </MenuItem>
                <MenuItem divider/>
                <MenuItem header><FormattedMessage id="dashboardSearch.statusDropdown.deleted"/></MenuItem>
                {this.renderFilterMenuItem("deleted", true)}
                {this.renderFilterMenuItem("undeleted", false)}


            </DropdownButton>
        );
    }
};


export default injectIntl(DashboardStatusDropdown);
